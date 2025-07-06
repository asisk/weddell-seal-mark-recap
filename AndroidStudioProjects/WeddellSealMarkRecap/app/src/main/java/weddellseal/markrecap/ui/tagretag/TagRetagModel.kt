package weddellseal.markrecap.ui.tagretag

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.wedCheck.processTags
import weddellseal.markrecap.frameworks.room.wedCheck.toSeal
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.utils.buildObservationRecord
import weddellseal.markrecap.ui.tagretag.utils.notebookEntryValueSeal
import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.ui.utils.getDeviceName

class TagRetagModel(
    application: Application,
    private val observationRepo: ObservationRepository,
    private val wedCheckRepo: WedCheckRepository,
    private val homeViewUiState: StateFlow<HomeViewModel.UiState>,
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    private val _primarySeal = MutableStateFlow(Seal(name = "primary", isStarted = false))
    val primarySeal: StateFlow<Seal> = _primarySeal

    private val _pupOne = MutableStateFlow(Seal(name = "pupOne", age = "Pup", isStarted = false))
    val pupOne: StateFlow<Seal> = _pupOne

    private val _pupTwo = MutableStateFlow(Seal(name = "pupTwo", age = "Pup", isStarted = false))
    val pupTwo: StateFlow<Seal> = _pupTwo

    fun prefillSingleMale() {
        _primarySeal.update {
            it.copy(
                age = "Adult",
                sex = "Male",
                numRelatives = "0",
                isStarted = true
            )
        }
        _uiState.update { it.copy(isPrefilled = true) }
    }

    fun prefillSingleFemale() {
        _primarySeal.update {
            it.copy(
                age = "Adult",
                sex = "Female",
                numRelatives = "0",
                isStarted = true
            )
        }
        _uiState.update { it.copy(isPrefilled = true) }
    }

    fun prefillMomAndPup() {
        _primarySeal.update {
            it.copy(
                age = "Adult",
                sex = "Female",
                numRelatives = "1",
                isStarted = true
            )
        }
        _pupOne.update { it.copy(numRelatives = "1", isStarted = true) }
        _uiState.update { it.copy(isPrefilled = true) }
    }

    //    init {
//        // Automatically update colonyLocation if it's set to ""
//        // In other words, if the user has not selected a location, use the auto-detected location
//        viewModelScope.launch {
//            combine(
//                sealColonyRepository.autoDetectedColony,
//                sealColonyRepository.overrideAutoColony
//            ) { detectedColony, overrideAutoColony ->
//                Pair(detectedColony, overrideAutoColony)
//            }.collect { (detectedColony, overrideAutoColony) ->
//                detectedColony?.let {
//                    if (!overrideAutoColony) {
//                        _uiState.update{it.copy(selectedColony = it.location)}
//                    }
//                }
//            }
//        }
//    }

    data class UiState(
        val observationRecord: ObservationRecord? = null,
        val metadata: ObservationMetadata = ObservationMetadata(),

        val isSearching: Boolean = false, // indicator for when searching a wedcheck seal

        val isPrefilled: Boolean = false, // indicator for pre-filled form for Census

        val isEditMode: Boolean = false, // indicator that an existing record (WedCheck or Observation) is being edited

        val isSaved: Boolean = false,  // indicator that record was successfully saved
        val isSaving: Boolean = false, // indicator that user is attempting to save the record
        val isSaveEnabled: Boolean = false, // indicator for save button
        val disableSave: Boolean = false, // indicator that save button should be disabled
        val ineligibleForSaveReason: String = "", // reasons save button is disabled

        val entryNeedsConfirmation: Boolean = false, // indicator that the user needs to confirm the entry

        val allSealsValid: Boolean = false, // indicator that all seals are valid
        val validationFailureReason: String = "", // reason for validation failure
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // called:
    // 1. after navigation command from the recent observation screen when a record is to be edited
    // 2. when a save is successful
    // 3. when a record is selected for editing from the Tag/Retag screen
    fun resetUiStateIndicators() {
        _uiState.update {
            it.copy(
                isSearching = false,
                isPrefilled = false,
                isEditMode = false,
                isSaved = false,
                isSaving = false,
                isSaveEnabled = false,
                ineligibleForSaveReason = "",
                validationFailureReason = "",
                entryNeedsConfirmation = false,
            )
        }

        _primarySeal.update { Seal(name = "primary", isStarted = false) }
        _pupOne.update { Seal(name = "pupOne", age = "Pup", isStarted = false) }
        _pupTwo.update { Seal(name = "pupTwo", age = "Pup", isStarted = false) }
    }

    fun setIsSaving() {
        _uiState.update { it.copy(isSaving = true, isSaveEnabled = false) }
    }

    fun editAfterAttemptedSave() {
        _uiState.update {
            it.copy(
                isSaved = false,
                isSaving = false,
                isSaveEnabled = true,
                entryNeedsConfirmation = false,
            )
        }
    }

    data class ObservationMetadata(
        val selectedColony: String = "",
        val selectedObservers: List<String> = listOf(),
        val censusNumber: String = "",
        val isCensusMode: Boolean = false,
        val deviceID: String = "Unknown", // no validation as the user cannot affect change, set default value in case of error getting device
        val currentSeason: String = "2025 Preset", // no validation as the user cannot affect change, set default value in case of error generating season
    ) {
        // computed property, evaluated only when explicitly accessed
        val isValid: Boolean
            get() = selectedColony != ""
                    && selectedObservers != emptyList<String>()
                    && (!isCensusMode || censusNumber != "")


        // computed property, evaluated only when explicitly accessed
        val invalidReason: String
            get() {
                val sb = StringBuilder()
                if (selectedColony == "") sb.append("Select a colony.")
                if (isCensusMode && censusNumber == "") sb.append("\nSelect a census number.")
                if (selectedObservers == emptyList<String>()) sb.append("\nSelect observer(s).")
                return sb.toString()
            }

        fun getObserversString(): String {
            return selectedObservers.joinToString(", ")
        }
    }

    // Initialize the ViewModel
    init {
        val deviceID = getDeviceName(context)
        val currentSeason = getCurrentYear().toString()

        viewModelScope.launch {
            combine(
                _primarySeal,
                _pupOne,
                _pupTwo,
                homeViewUiState
            ) { primary, pupOne, pupTwo, metadata ->

                // Initialize the metadata object
                val metadata = ObservationMetadata(
                    selectedColony = homeViewUiState.value.selectedColony,
                    selectedObservers = homeViewUiState.value.selectedObservers,
                    censusNumber = homeViewUiState.value.selectedCensusNumber,
                    isCensusMode = homeViewUiState.value.isCensusMode,
                    deviceID = deviceID,
                    currentSeason = currentSeason,
                )

                // Check if save is enabled
                val reasons = buildList {
                    if (!metadata.isValid) add(metadata.invalidReason)
                    if (!primary.isComplete) addAll(primary.completenessReasons)
                    if (pupOne.isStarted && !pupOne.isComplete) addAll(pupOne.completenessReasons)
                    if (pupTwo.isStarted && !pupTwo.isComplete) addAll(pupTwo.completenessReasons)
                }

                // Check if the observation has valid seal data
                var allSealsValid = true
                if (!primary.isValid) {
                    allSealsValid = false
                }
                if (pupOne.isStarted && !pupOne.isValid) {
                    allSealsValid = false
                }
                if (pupTwo.isStarted && !pupTwo.isValid) {
                    allSealsValid = false
                }

                // emit a Triple that can be unpacked in `collect`
                Triple(metadata, reasons, allSealsValid)

            }.collect { (metadata, reasons, allSealsValid) ->
                _uiState.update {
                    it.copy(
                        metadata = metadata,
                        isSaveEnabled = reasons.isEmpty(),
                        ineligibleForSaveReason = reasons.joinToString("\n"),
                        allSealsValid = allSealsValid
                    )
                }
            }
        }
    }

    fun checkNeedsConfirmation(
        primarySealValidationErrors: List<String>,
        pupOneSealValidationErrors: List<String>,
        pupTwoSealValidationErrors: List<String>
    ) {
        // the validation errors for a seal that isn't started will be empty
        val validationErrorString = buildList {
            addAll(primarySealValidationErrors)
            addAll(pupOneSealValidationErrors)
            addAll(pupTwoSealValidationErrors)
        }
        _uiState.update { it.copy(validationFailureReason = validationErrorString.joinToString()) }

        if (validationErrorString.isNotEmpty()) {
            // Needs Confirmation
            // require the technician to save the record by confirming and saving
            _uiState.update { it.copy(entryNeedsConfirmation = true) }
        }
    }

    fun findWedCheckMatch(seal: Seal, searchTagID : String) {
        if (searchTagID != "") {
            viewModelScope.launch {
                _uiState.update { it.copy(isSearching = true) }

                try {
                    val sealFound: WedCheckRecord? = withContext(Dispatchers.IO) {
                        wedCheckRepo.findSealbyTagID(searchTagID.trim())
                    }

                    if (sealFound != null) {
                        when (seal.name) {
                            "primary" -> {
                                _primarySeal.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
                            }

                            "pupOne" -> {
                                _pupOne.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
                            }

                            "pupTwo" -> {
                                _pupTwo.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SealLookup", "Error fetching seal: ${e.localizedMessage}", e)
                }
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun resetWedCheckMatch(seal : Seal) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(wedCheckMatch = WedCheckSeal()) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(wedCheckMatch = WedCheckSeal()) }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(wedCheckMatch = WedCheckSeal()) }
            }
        }
    }

    fun updateAge(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(age = input, isStarted = true) }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(age = input, isStarted = true)
                }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(age = input, isStarted = true)
                }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateSex(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(sex = input, isStarted = true) }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(sex = input, isStarted = true)
                }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(sex = input, isStarted = true)
                }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updatePupPeed(sealName: String, input: Boolean) {
        when (sealName) {
            "primary" -> {
                if (primarySeal.value.age == "Pup") {
                    _primarySeal.update { it.copy(pupPeed = true, isStarted = true) }
                }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(pupPeed = input, isStarted = true)
                }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(pupPeed = input, isStarted = true)
                }
            }
        }
    }

    fun updateNumRelatives(input: String) {
        val number: Int? = input.toIntOrNull()
        if (number != null) {
            when (number) {
                0 -> {
                    removePups()
                }

                1 -> {
                    _pupOne.update {
                        it.copy(numRelatives = input, isStarted = true)
                    }
                    _pupTwo.update {
                        it.copy(numRelatives = input, isStarted = false)
                    }
                    updateNotebookEntry(pupOne.value)
                    updateNotebookEntry(pupTwo.value)
                }

                2 -> {
                    _pupOne.update {
                        it.copy(numRelatives = input, isStarted = true)
                    }
                    _pupTwo.update {
                        it.copy(numRelatives = input, isStarted = true)
                    }
                    updateNotebookEntry(pupOne.value)
                    updateNotebookEntry(pupTwo.value)
                }
            }

            _primarySeal.update { it.copy(numRelatives = input, isStarted = true) }
            updateNotebookEntry(primarySeal.value)
        }
    }

    fun updateCondition(sealName: String, input: SealCondition) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(condition = input, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(condition = input, isStarted = true) }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(condition = input, isStarted = true) }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateTagEventType(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(tagEventType = input, isStarted = true) }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update { it.copy(tagEventType = input, isStarted = true) }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(tagEventType = input, isStarted = true) }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateTagAlpha(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(tagAlpha = input, isStarted = true) }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(tagAlpha = input, isStarted = true)
                }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(tagAlpha = input, isStarted = true)
                }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateTagNumber(seal: Seal, input: String) {
        var tagNumber = input
        // Function to extract numeric value
        if (input.toIntOrNull() != null) {
            tagNumber = input
        }

        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(tagNumber = tagNumber, isStarted = true) }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update { it.copy(tagNumber = tagNumber, isStarted = true) }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(tagNumber = tagNumber, isStarted = true) }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateOldTag(seal: Seal, oldTagIdOne: String) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(oldTagId = oldTagIdOne, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(oldTagId = oldTagIdOne, isStarted = true) }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(oldTagId = oldTagIdOne, isStarted = true) }
            }
        }
    }

    fun updateRetagReason(sealName: String, input: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(reasonForRetag = input) }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(reasonForRetag = input)
                }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(reasonForRetag = input)
                }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateOldTagMarks(name: String, oldTagMarks: Boolean) {
        when (name) {
            "primary" -> {
                _primarySeal.update { it.copy(oldTagMarks = oldTagMarks) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(oldTagMarks = oldTagMarks) }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(oldTagMarks = oldTagMarks)
                }
            }
        }
    }

    fun updateNumTags(sealName: String, input: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(numTags = input, isStarted = true) }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update { it.copy(numTags = input, isStarted = true) }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(numTags = input, isStarted = true) }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateNoTag(sealName: String, input: Boolean) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(isNoTag = input) }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update { it.copy(isNoTag = input) }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(isNoTag = input) }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun updateTissueTaken(sealName: String, input: Boolean) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(tissueTaken = input, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(tissueTaken = input, isStarted = true) }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(tissueTaken = input, isStarted = true) }
            }
        }
    }

    fun updateIsWeightTaken(sealName: String, checked: Boolean) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(weightTaken = checked, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(weightTaken = checked, isStarted = true) }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(weightTaken = checked, isStarted = true) }
            }
        }
    }

    fun updateComment(sealName: String, input: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(comment = input, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(comment = input, isStarted = true) }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(comment = input, isStarted = true) }
            }
        }
    }

    fun updateWeight(seal: Seal, number: Int) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(weight = number, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(weight = number, isStarted = true) }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(weight = number, isStarted = true)
                }
            }
        }
    }

    private fun updateNotebookEntry(seal: Seal) {
        val notebookEntry = notebookEntryValueSeal(seal)

        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(notebookDataString = notebookEntry) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(notebookDataString = notebookEntry) }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(notebookDataString = notebookEntry) }
            }
        }
    }

    fun updateWedCheckMatch(seal: Seal, lookupSeal: WedCheckSeal) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update {
                    it.copy(
                        wedCheckMatch = lookupSeal,
                    )
                }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(
                        wedCheckMatch = lookupSeal,
                    )
                }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(
                        wedCheckMatch = lookupSeal,
                    )
                }
            }
        }
    }

    fun clearTagID(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update {
                    it.copy(
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(pupTwo.value)
            }
        }
    }

    fun clearNumTags(sealName: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(numTags = "") }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(numTags = "") }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(numTags = "") }
            }
        }
    }

    fun clearOldTag(sealName: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(oldTagId = "") }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(oldTagId = "") }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(oldTagId = "")
                }
            }
        }
    }

    fun removeWedCheckMatch(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update {
                    it.copy(wedCheckMatch = null)
                }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(wedCheckMatch = null)
                }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(wedCheckMatch = null)
                }
            }
        }
    }


    fun resetPupFields(sealName: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update {
                    it.copy(
                        pupPeed = false, weightTaken = false, weight = 0, isStarted = true
                    )
                }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(
                        pupPeed = false, weightTaken = false, weight = 0, isStarted = true
                    )
                }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(
                        pupPeed = false, weightTaken = false, weight = 0, isStarted = true
                    )
                }
            }
        }
    }

    fun resetSeal(sealName: String) {
        var parentNumRels = primarySeal.value.numRelatives
        if (primarySeal.value.numRelatives != "" && primarySeal.value.numRelatives.toIntOrNull() != null) {
            var number = parentNumRels.toInt()
            number -= 1
            parentNumRels = number.toString()
        }

        when (sealName) {
            "primary" -> {
                _primarySeal.update {
                    Seal(name = "primary", isStarted = false)
                }
                // removing the primary seal results in removing pups, if present, as well
                _pupOne.update {
                    Seal(
                        name = "pupOne",
                        age = "Pup",
                        isStarted = false
                    )
                }
                _pupTwo.update {
                    Seal(
                        name = "pupTwo",
                        age = "Pup",
                        isStarted = false
                    )
                }
            }

            "pupOne" -> {
                // update parent num rels when pup one is removed
                _primarySeal.update { it.copy(numRelatives = parentNumRels) }

                // if pupOne is removed and there's a second pup
                if (pupTwo.value.isStarted) {
                    // rename the second pup and update it's number of relatives
                    _pupTwo.update {
                        it.copy(
                            name = "pupOne",
                            numRelatives = primarySeal.value.numRelatives
                        )
                    }
                    //reassign it to pupOne
                    _pupOne.update { pupTwo.value }
                    //deactivate pupTwo
                    _pupTwo.update { Seal(name = "pupTwo", age = "Pup", isStarted = false) }
                } else {
                    _pupOne.update {
                        Seal(
                            name = "pupOne",
                            age = "Pup",
                            numRelatives = primarySeal.value.numRelatives,
                            isStarted = false
                        )
                    }
                }
            }

            "pupTwo" -> {
                _pupTwo.update { Seal(name = "pupTwo", age = "Pup", isStarted = false) }

                if (pupOne.value.isStarted) {
                    _pupOne.update { it.copy(numRelatives = parentNumRels) }
                }
            }
        }

        updateNotebookEntry(primarySeal.value)
        updateNotebookEntry(pupOne.value)
        updateNotebookEntry(pupTwo.value)
    }

    private fun removePups() {
        //called when primary seal number of relatives is set to zero
        _pupOne.update { Seal(name = "pupOne", age = "Pup", isStarted = false) }
        _pupTwo.update { Seal(name = "pupTwo", age = "Pup", isStarted = false) }
    }

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen selection of Tag/Retag
    // prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun populateSealFromLookup(lookupSeal: WedCheckSeal) {
        // advance the age based on the last seen season
        val currentYear = getCurrentYear()
        var sealAgeAdvanced = "Adult"
        when (lookupSeal.lastSeenSeason) {
            currentYear -> { // seal last seen this year
                // Age class CANNOT change for seals seen twice in a season
                sealAgeAdvanced = lookupSeal.age
            }

            currentYear - 1 -> { // seal last seen last year
                // Age class must advance for seals seen last year
                val expectedAge = when (lookupSeal.age) {
                    "Pup" -> "Yearling"
                    "Yearling" -> "Adult"
                    else -> "Adult"
                }

                sealAgeAdvanced = expectedAge
            }

            currentYear - 2 -> { // seal last seen 2 or more years ago
                // Age class must be Adult if seal was observed two or more years ago
                sealAgeAdvanced = "Adult"
            }
        }

        // number of Relatives shouldn't be populated for Female seals because it's likely that the seal has a pup
        val numberRels = if (lookupSeal.sex == "Female") "" else "0"

        _primarySeal.update {
            it.copy(
                age = sealAgeAdvanced,
                sex = lookupSeal.sex,
                numRelatives = numberRels,
                tagNumber = lookupSeal.tagOneNumber,
                tagAlpha = lookupSeal.tagOneAlpha,
                oldTagId = lookupSeal.tagIdOne,
                tagEventType = "Marked",
                lastPhysio = lookupSeal.lastPhysio,
                colony = lookupSeal.colony,
                wedCheckMatch = lookupSeal
            )
        }

        updateNotebookEntry(primarySeal.value)
    }

    fun loadObservationEntryForView(observation: ObservationRecord) {
        _uiState.update { it.copy(observationRecord = observation) }
    }

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen
    // prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun loadSealForEdit(log: ObservationRecord?) {
        // Create a String array for the data
        if (log != null) {
            var ageString = when (log.ageClass) {
                "A" -> "Adult"
                "P" -> "Pup"
                "Y" -> "Yearling"
                else -> ""
            }

            val sealSex = when (log.sex) {
                "F" -> "Female"
                "M" -> "Male"
                "U" -> "Unknown"
                else -> ""
            }

            val tagEvent = when (log.tagEvent) {
                "M" -> "Marked"
                "N" -> "New"
                "R2" -> "Retag"
                else -> ""
            }

            var processedTagOneNumber = ""
            var processedTagOneAlpha = ""
            var numTags = 0

            if (log.tagIDOne != "NoTag") {
                val processedTagOne = processTags(log.tagIDOne)
                if (processedTagOne.tagValid) {
                    numTags++
                    processedTagOneNumber = processedTagOne.tagNumber
                    processedTagOneAlpha = processedTagOne.tagAlpha
                }

                val processedTagTwo = processTags(log.tagIDTwo)
                if (processedTagTwo.tagValid) {
                    numTags++
                }
            }

            _primarySeal.update {
                it.copy(
                    colony = log.colony,
                    observationRecordSpeno = log.speno.toInt(),
                    age = ageString, // expecting to advance the seal age based on the last season seen
                    sex = sealSex,
                    numRelatives = log.numRelatives,
                    condition = SealCondition.fromCode(log.sealCondition),
                    tagNumber = processedTagOneNumber,
                    tagAlpha = processedTagOneAlpha,
                    oldTagId = log.oldTagIDOne,
                    tagEventType = tagEvent,
                    reasonForRetag = log.retagReason,
                    numTags = if (numTags > 0) numTags.toString() else "",
                    isNoTag = log.tagIDOne == "NoTag" && log.tagEvent == "Marked",
                    comment = log.comments,
                    weightTaken = log.weight != "",
                    weight = if (log.weight != "") log.weight.toInt() else 0,
                    tissueTaken = log.tissueSampled != "",
                    flaggedForReview = log.flaggedEntry != "",
                    isStarted = true,
                    observationID = log.id,
                    isTagRetagEntry = true,
                )
            }
        }
        updateNotebookEntry(primarySeal.value)
    }

    fun flagSealForReview(name: String) {
        when (name) {
            "primary" -> {
                _primarySeal.update { it.copy(flaggedForReview = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(flaggedForReview = true) }
            }

            "pupTwo" -> {
                _pupTwo.update { it.copy(flaggedForReview = true) }
            }
        }
    }

    fun createLog(
        currentLocation: GeoLocation?,
    ) {
        val sealsList = listOf(primarySeal.value, pupOne.value, pupTwo.value)

        for (seal in sealsList) {
            if (seal.isStarted) {
                // get the tags for this seal's relatives
                val (relOneTag, relTwoTag) = getRelativesTags(seal.name)
                val log = buildObservationRecord(
                    currentLocation,
                    seal,
                    relOneTag,
                    relTwoTag,
                    uiState.value.metadata,
                )

                //write an entry to the database for each seal that has valid input
                viewModelScope.launch {
                    observationRepo.addObservation(log)
                }

                //TODO, consider overwriting the database entry, instead of appending a new entry
//                if (primarySeal.observationID != 0) {
//
//                }
            }
        }
        _uiState.update { it.copy(isSaved = true, isSaving = false, isSaveEnabled = true) }
    }

    private fun getRelativesTags(sealName: String): Pair<String, String> {
        var relOneTagId = ""
        var relTwoTagId = ""
        when (sealName) {
            "primary" -> {
                relOneTagId = pupOne.value.tagNumber + pupOne.value.tagAlpha
                relTwoTagId = pupTwo.value.tagNumber + pupTwo.value.tagAlpha
            }

            "pupOne" -> {
                relOneTagId =
                    primarySeal.value.tagNumber + primarySeal.value.tagAlpha
                relTwoTagId = pupTwo.value.tagNumber + pupTwo.value.tagAlpha
            }

            "pupTwo" -> {
                relOneTagId =
                    primarySeal.value.tagNumber + primarySeal.value.tagAlpha
                relTwoTagId = pupOne.value.tagNumber + pupOne.value.tagAlpha
            }
        }
        return Pair(relOneTagId, relTwoTagId)
    }
}