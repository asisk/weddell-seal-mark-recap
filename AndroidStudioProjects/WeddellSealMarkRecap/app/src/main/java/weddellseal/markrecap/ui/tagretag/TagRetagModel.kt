package weddellseal.markrecap.ui.tagretag

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.RetagReason
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observations.toSeal
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.wedCheck.toSeal
import weddellseal.markrecap.ui.UiEvent
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.DisplayObservation
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

    private val _primarySeal = MutableStateFlow(Seal(sealType = SealType.PRIMARY))
    val primarySeal: StateFlow<Seal> = _primarySeal

    private val _pupOne =
        MutableStateFlow(Seal(sealType = SealType.PUPONE, ageClass = SealAgeClass.PUP))
    val pupOne: StateFlow<Seal> = _pupOne

    private val _pupTwo =
        MutableStateFlow(Seal(sealType = SealType.PUPTWO, ageClass = SealAgeClass.PUP))
    val pupTwo: StateFlow<Seal> = _pupTwo

    fun prefillSingleMale() {
        _primarySeal.update {
            it.copy(
                ageClass = SealAgeClass.ADULT,
                sex = SealSex.MALE,
                numRelatives = "0"
            )
        }
        _uiState.update { it.copy(isPrefilled = true) }
    }

    fun prefillSingleFemale() {
        _primarySeal.update {
            it.copy(
                ageClass = SealAgeClass.ADULT,
                sex = SealSex.FEMALE,
                numRelatives = "0"
            )
        }
        _uiState.update { it.copy(isPrefilled = true) }
    }

    fun prefillMomAndPup() {
        _primarySeal.update {
            it.copy(
                ageClass = SealAgeClass.ADULT,
                sex = SealSex.FEMALE,
                numRelatives = "1"
            )
        }
        _pupOne.update { it.copy(numRelatives = "1") }
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
        val metadata: ObservationMetadata = ObservationMetadata(),

        val isSearching: Boolean = false, // indicator for when searching a wedcheck seal

        val isPrefilled: Boolean = false, // indicator for pre-filled form for Census

        val isEditMode: Boolean = false, // indicator that an existing record (WedCheck or Observation) is being edited

        val isSaved: Boolean = false,  // indicator that record was successfully saved
        val isSaveAttempted: Boolean = false, // indicator that user is attempting to save the record
        val isSaveEnabled: Boolean = false, // indicator for save button
        val disableSave: Boolean = false, // indicator that save button should be disabled
        val ineligibleForSaveReason: String = "", // reasons save button is disabled

        val entryNeedsConfirmation: Boolean = false, // indicator that the user needs to confirm the entry

        val allSealsValid: Boolean = false, // indicator that all seals are valid
        val validationFailureReason: String = "", // reason for validation failure
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _selectedRecentObservation = MutableStateFlow<DisplayObservation?>(null)
    val selectedRecentObservation: StateFlow<DisplayObservation?> get() = _selectedRecentObservation

    fun onEditAttempt(observation: DisplayObservation) {
        viewModelScope.launch {
            if (!primarySeal.value.isEntryStarted) {
                _selectedRecentObservation.value = observation // set the observation to edit
                _uiEvent.emit(UiEvent.ShowEditDialog)
            } else {
                _uiEvent.emit(
                    UiEvent.ShowToast("Looks like you're already editing another seal! Save or clear, then edit this record.")
                )
            }
        }
    }

    fun onViewAttempt(observation: DisplayObservation) {
        viewModelScope.launch {
            _selectedRecentObservation.value = observation // set the observation to edit
        }
    }

    // This function is used to ensure that each seal has it’s own WedCheck match & associated speno.
    // Changes to one seal should not affect pup or mom.
    // If there's not WedCheck match, no speno should be assigned.
    // WedCheck match can be populated from the lookup record.

    /* Notes on Display of speno */
    // The WedCheck match record speno is displayed in the UI.
    // Not shown in the UI when the Tag Event is New, but it should be available for validation step.

    /* Notes on when to refresh the WedCheck match */
    // Search with Tag ID when tag event changes to Marked or New.
    // Search when the Tag ID number or alpha character changes.
    // Search with Old Tag ID when tag event changes to Retag.
    fun requestCurrentWedCheckMatch(seal: Seal) {
        val searchStr = when {
            seal.useTagID && seal.isTagIDValid -> seal.tagNumber + seal.tagAlpha
            seal.useOldTag && seal.isOldTagValid -> seal.oldTagNumber + seal.oldTagAlpha
            else -> {
                Log.d("TagRetagModel", "Seal does not have a valid tag to use for WedCheck lookup")
                return
            }
        }

        if (seal.wedCheckMatch?.tagIdOne == searchStr) {
            Log.d(
                "TagRetagModel",
                "Seal with tag ID: $searchStr already has a current WedCheck match: ${seal.wedCheckMatch.tagIdOne}"
            )
            return
        }

        if (!uiState.value.isSearching) {
            if (seal.wedCheckMatch != null) {
                Log.d(
                    "TagRetagModel",
                    "removing current WedCheck match for seal with tag ID: $searchStr"
                )
                removeWedCheckMatch(seal.sealType)
            }
            Log.d("TagRetagModel", "looking up seal for $searchStr")
            findWedCheckMatch(seal, searchStr)
        } else {
            Log.d("TagRetagModel", "ignoring requested lookup as search is already in progress")
        }
    }

    fun onRetagSelection(seal: Seal) { // TODO, consider passing the event Type as a parameter, and change the seal to sealType
        if (primarySeal.value.tagEventType == TagEventType.RETAG
            && (primarySeal.value.reasonForRetag == RetagReason.ONE_OF_FOUR
                    || primarySeal.value.reasonForRetag == RetagReason.TWO_OF_FOUR
                    || primarySeal.value.reasonForRetag == RetagReason.THREE_OF_FOUR)
        ) {
            when (seal.sealType) {
                SealType.PRIMARY -> {
                    _primarySeal.update {
                        it.copy(
                            oldTagNumber = seal.tagNumber,
                            oldTagAlpha = seal.tagAlpha
                        )
                    }
                }

                SealType.PUPONE -> {
                    _pupOne.update {
                        it.copy(
                            oldTagNumber = seal.tagNumber,
                            oldTagAlpha = seal.tagAlpha
                        )
                    }
                }

                SealType.PUPTWO -> {
                    _pupTwo.update {
                        it.copy(
                            oldTagNumber = seal.tagNumber,
                            oldTagAlpha = seal.tagAlpha
                        )
                    }
                }

                SealType.UNKNOWN -> {
                    // No action needed for UNKNOWN
                }
            }
        }
    }

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
                isSaveAttempted = false,
                isSaveEnabled = false,
                ineligibleForSaveReason = "",
                validationFailureReason = "",
                entryNeedsConfirmation = false,
            )
        }

        _primarySeal.update {
            Seal(
                sealType = SealType.PRIMARY
            )
        }
        _pupOne.update {
            Seal(
                sealType = SealType.PUPONE,
                ageClass = SealAgeClass.PUP
            )
        }
        _pupTwo.update {
            Seal(
                sealType = SealType.PUPTWO,
                ageClass = SealAgeClass.PUP
            )
        }
    }

    fun setIsSaving() {
        _uiState.update { it.copy(isSaveAttempted = true, isSaveEnabled = false) }
    }

    fun editAfterAttemptedSave() {
        _uiState.update {
            it.copy(
                isSaved = false,
                isSaveAttempted = false,
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

        // Observe homeViewUiState and update metadata
        // Observe seal validity & save eligibility
        viewModelScope.launch {
            // Each time any of these change:
            // 1) build a metadata object describing things like colony, observers, and season.
            // 2) check each seal (primary, pup one, pup two) to see if their information is complete and valid.
            // 3) create a list of reasons why the data isn’t ready to save.
            combine(
                _primarySeal,
                _pupOne,
                _pupTwo,
                homeViewUiState
            ) { primary, pupOne, pupTwo, homeUiState ->

                // Initialize the metadata object
                val metadata = ObservationMetadata(
                    selectedColony = homeUiState.selectedColony,
                    selectedObservers = homeUiState.selectedObservers,
                    censusNumber = homeUiState.selectedCensusNumber,
                    isCensusMode = homeUiState.isCensusMode,
                    deviceID = deviceID,
                    currentSeason = currentSeason
                )

                // Check if save is enabled
                val reasons = buildList {
                    if (!metadata.isValid) add(metadata.invalidReason)
                    if (!primary.isComplete) addAll(primary.completenessReasons)
                    if (primary.hasPupOne && !pupOne.isComplete) addAll(pupOne.completenessReasons)
                    if (primary.hasPupTwo && !pupTwo.isComplete) addAll(pupTwo.completenessReasons)
                }

                // Check if the observation has valid seal data
                val allSealsValid = primary.isValid &&
                        (!primary.hasPupOne || pupOne.isValid) &&
                        (!primary.hasPupTwo || pupTwo.isValid)

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

    fun findWedCheckMatch(seal: Seal, searchTagID: String) {
        if (searchTagID != "") {
            viewModelScope.launch {
                _uiState.update { it.copy(isSearching = true) }

                try {
                    val sealFound: WedCheckRecord? = withContext(Dispatchers.IO) {
                        wedCheckRepo.findSealbyTagID(searchTagID.trim())
                    }

                    if (sealFound != null) {
                        when (seal.sealType) {
                            SealType.PRIMARY -> {
                                _primarySeal.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
                            }

                            SealType.PUPONE -> {
                                _pupOne.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
                            }

                            SealType.PUPTWO -> {
                                _pupTwo.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
                            }

                            SealType.UNKNOWN -> {
                                // No action needed for UNKNOWN
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

    fun updateAge(seal: Seal, input: String) {
        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(ageClass = SealAgeClass.fromSelection(input)) }
                updateNotebookEntry(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update {
                    it.copy(ageClass = SealAgeClass.fromSelection(input))
                }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(ageClass = SealAgeClass.fromSelection(input))
                }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateSex(seal: Seal, input: SealSex) {
        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(sex = input) }
                updateNotebookEntry(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update {
                    it.copy(sex = input)
                }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(sex = input)
                }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updatePupPeed(sealName: SealType, input: Boolean) {
        when (sealName) {
            SealType.PRIMARY -> {
                if (primarySeal.value.ageClass == SealAgeClass.PUP) {
                    _primarySeal.update { it.copy(pupPeed = true) }
                }
            }

            SealType.PUPONE -> {
                _pupOne.update {
                    it.copy(pupPeed = input)
                }
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(pupPeed = input)
                }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
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
                        it.copy(numRelatives = input)
                    }
                    _pupTwo.update {
                        it.copy(numRelatives = input)
                    }
                    updateNotebookEntry(pupOne.value)
                    updateNotebookEntry(pupTwo.value)
                }

                2 -> {
                    _pupOne.update {
                        it.copy(numRelatives = input)
                    }
                    _pupTwo.update {
                        it.copy(numRelatives = input)
                    }
                    updateNotebookEntry(pupOne.value)
                    updateNotebookEntry(pupTwo.value)
                }
            }

            _primarySeal.update { it.copy(numRelatives = input) }
            updateNotebookEntry(primarySeal.value)
        }
    }

    fun updateCondition(sealName: SealType, input: SealCondition) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(condition = input) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(condition = input) }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(condition = input) }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateTagEventType(seal: Seal, input: TagEventType) {
        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(tagEventType = input) }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(tagEventType = input) }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(tagEventType = input) }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateTagNumber(seal: Seal, input: String) {
        var tagNumber = input
        // Function to extract numeric value
        if (input.toIntOrNull() != null) {
            tagNumber = input
        }

        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(tagNumber = tagNumber) }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(tagNumber = tagNumber) }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(tagNumber = tagNumber) }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateTagAlpha(seal: Seal, input: String) {
        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(tagAlpha = input) }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(tagAlpha = input) }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(tagAlpha = input) }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateOldTagNumber(seal: Seal, input: String) {
        var tagNumber = input
        // Function to extract numeric value
        if (input.toIntOrNull() != null) {
            tagNumber = input
        }

        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(oldTagNumber = input) }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(oldTagNumber = input) }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(oldTagNumber = input) }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateOldTagAlpha(seal: Seal, input: String) {
        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(oldTagAlpha = input) }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update {
                    it.copy(oldTagAlpha = input)
                }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(oldTagAlpha = input)
                }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateRetagReason(sealName: SealType, input: RetagReason) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(reasonForRetag = input) }
            }

            SealType.PUPONE -> {
                _pupOne.update {
                    it.copy(reasonForRetag = input)
                }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(reasonForRetag = input)
                }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateOldTagMarks(name: SealType, oldTagMarks: Boolean) {
        when (name) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(oldTagMarks = oldTagMarks) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(oldTagMarks = oldTagMarks) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(oldTagMarks = oldTagMarks)
                }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateNumTags(sealName: SealType, input: String) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(numTags = input) }
                updateNotebookEntry(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(numTags = input) }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(numTags = input) }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateNoTag(sealName: SealType, input: Boolean) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(isNoTag = input) }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(isNoTag = input) }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(isNoTag = input) }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateTissueTaken(sealName: SealType, input: Boolean) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(tissueTaken = input) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(tissueTaken = input) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(tissueTaken = input) }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateIsWeightTaken(sealName: SealType, checked: Boolean) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(weightTaken = checked) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(weightTaken = checked) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(weightTaken = checked) }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateComment(sealName: SealType, input: String) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(comment = input) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(comment = input) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(comment = input) }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateWeight(sealType: SealType, number: Int) {
        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(weight = number) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(weight = number) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(weight = number)
                }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    private fun updateNotebookEntry(seal: Seal) {
        val notebookEntry =
            notebookEntryValueSeal(seal) //TODO, consider moving this to a calculated value on the seal

        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(notebookDataString = notebookEntry) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(notebookDataString = notebookEntry) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(notebookDataString = notebookEntry) }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun clearTagID(seal: Seal) {
        when (seal.sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update {
                    it.copy(
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update {
                    it.copy(
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun clearNumTags(sealName: SealType) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(numTags = "") }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(numTags = "") }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(numTags = "") }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun clearOldTag(sealName: SealType) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(oldTagNumber = "", oldTagAlpha = "") }
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(oldTagNumber = "", oldTagAlpha = "") }
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(oldTagNumber = "", oldTagAlpha = "") }
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    /* Notes on when to clear the WedCheck match */
    // Remove the WedCheck match when the Tag ID field is cleared.
    // Remove the WedCheck match when the Old Tag ID field is cleared.
    // Remove the WedCheck match when No Tag is selected.
    // Removed when the tag id or old tag id changes and a search for a WedCheck match is initiated.
    fun removeWedCheckMatch(sealName: SealType) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(wedCheckMatch = null) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(wedCheckMatch = null) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(wedCheckMatch = null) }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun resetPupFields(sealName: SealType) {
        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update {
                    it.copy(
                        pupPeed = false, weightTaken = false, weight = 0
                    )
                }
            }

            SealType.PUPONE -> {
                _pupOne.update {
                    it.copy(
                        pupPeed = false, weightTaken = false, weight = 0
                    )
                }
            }

            SealType.PUPTWO -> {
                _pupTwo.update {
                    it.copy(
                        pupPeed = false, weightTaken = false, weight = 0
                    )
                }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun resetSeal(sealName: SealType) {
        var parentNumRels = primarySeal.value.numRelatives
        if (primarySeal.value.numRelatives != "" && primarySeal.value.numRelatives.toIntOrNull() != null) {
            var number = parentNumRels.toInt()
            number -= 1
            parentNumRels = number.toString()
        }

        when (sealName) {
            SealType.PRIMARY -> {
                _primarySeal.update {
                    Seal(sealType = SealType.PRIMARY)
                }
                // removing the primary seal results in removing pups, if present, as well
                _pupOne.update {
                    Seal(
                        sealType = SealType.PUPONE,
                        ageClass = SealAgeClass.PUP
                    )
                }
                _pupTwo.update {
                    Seal(
                        sealType = SealType.PUPTWO,
                        ageClass = SealAgeClass.PUP
                    )
                }
            }

            SealType.PUPONE -> {
                // update parent num rels when pup one is removed
                _primarySeal.update { it.copy(numRelatives = parentNumRels) }

                // if pupOne is removed and there's a second pup
                if (pupTwo.value.isEntryStarted) {
                    // rename the second pup and update it's number of relatives
                    _pupTwo.update {
                        it.copy(
                            sealType = SealType.PUPONE,
                            numRelatives = primarySeal.value.numRelatives
                        )
                    }
                    //reassign it to pupOne
                    _pupOne.update { pupTwo.value }
                    //deactivate pupTwo
                    _pupTwo.update { Seal(sealType = SealType.PUPTWO, ageClass = SealAgeClass.PUP) }
                } else {
                    _pupOne.update {
                        Seal(
                            sealType = SealType.PUPONE,
                            ageClass = SealAgeClass.PUP,
                            numRelatives = primarySeal.value.numRelatives
                        )
                    }
                }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { Seal(sealType = SealType.PUPTWO, ageClass = SealAgeClass.PUP) }

                if (pupOne.value.isEntryStarted) { // TODO, test!
                    _pupOne.update { it.copy(numRelatives = parentNumRels) }
                }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }

        updateNotebookEntry(primarySeal.value)
        updateNotebookEntry(pupOne.value)
        updateNotebookEntry(pupTwo.value)
    }

    private fun removePups() {
        //called when primary seal number of relatives is set to zero
        _pupOne.update { Seal(sealType = SealType.PUPONE, ageClass = SealAgeClass.PUP) }
        _pupTwo.update { Seal(sealType = SealType.PUPTWO, ageClass = SealAgeClass.PUP) }
    }

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen selection of Tag/Retag
// prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun populateSealFromLookup(lookupSeal: WedCheckSeal) {
        // advance the age based on the last seen season
        val currentYear = getCurrentYear()
        var sealAgeAdvanced = SealAgeClass.ADULT
        when (lookupSeal.lastSeenSeason) {
            currentYear -> { // seal last seen this year
                // Age class CANNOT change for seals seen twice in a season
                sealAgeAdvanced = lookupSeal.ageClass
            }

            currentYear - 1 -> { // seal last seen last year
                // Age class must advance for seals seen last year
                val expectedAge = when (lookupSeal.ageClass) {
                    SealAgeClass.PUP -> SealAgeClass.YEARLING
                    SealAgeClass.YEARLING -> SealAgeClass.ADULT
                    else -> SealAgeClass.ADULT
                }

                sealAgeAdvanced = expectedAge
            }

            currentYear - 2 -> { // seal last seen 2 or more years ago
                // Age class must be Adult if seal was observed two or more years ago
                sealAgeAdvanced = SealAgeClass.ADULT
            }
        }

        // number of Relatives shouldn't be populated for Female seals because it's likely that the seal has a pup
        val numberRels = if (lookupSeal.sex == SealSex.FEMALE) "" else "0"

        _primarySeal.update {
            it.copy(
                ageClass = sealAgeAdvanced,
                sex = lookupSeal.sex,
                numRelatives = numberRels,
                tagNumber = lookupSeal.tagOneNumber,
                tagAlpha = lookupSeal.tagOneAlpha,
                oldTagNumber = lookupSeal.tagOneNumber,
                oldTagAlpha = lookupSeal.tagOneAlpha,
                tagEventType = lookupSeal.tagEventType,
                lastPhysio = lookupSeal.lastPhysio,
                colony = lookupSeal.colony,
                wedCheckMatch = lookupSeal
            )
        }

        updateNotebookEntry(primarySeal.value)
    }

    // used to pull over the fields from the ObservationRecord
    // pups recorded with the original ObservationRecord should be included
    // prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun loadSealForEdit(
        displayObservation: DisplayObservation
    ) {
        when (displayObservation) {
            is DisplayObservation.WithPups -> {
                _primarySeal.update {
                    displayObservation.primarySeal.toSeal()
                        .copy(sealType = SealType.PRIMARY)
                }
                updateNotebookEntry(primarySeal.value)

                displayObservation.pupOne?.let {
                    _pupOne.update {
                        displayObservation.pupOne.toSeal()
                            .copy(sealType = SealType.PUPONE)
                    }
                    updateNotebookEntry(pupOne.value)
                }

                displayObservation.pupTwo?.let {
                    _pupTwo.update {
                        displayObservation.pupTwo.toSeal()
                            .copy(sealType = SealType.PUPTWO)
                    }
                    updateNotebookEntry(pupTwo.value)
                }
            }

            is DisplayObservation.Standalone -> {
                _primarySeal.update { displayObservation.primarySeal.toSeal() }
                updateNotebookEntry(primarySeal.value)
            }
        }

        _uiState.update { it.copy(isEditMode = true) }
    }

    fun flagSealForReview(type: SealType) {
        when (type) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(flaggedForReview = true) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(flaggedForReview = true) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(flaggedForReview = true) }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun createLog(
        currentLocation: GeoLocation?,
    ) {
        val sealsComplete = listOf(primarySeal.value, pupOne.value, pupTwo.value)
            .filter { it.isComplete } // checking for completeness & not validated (confirmed records will be invalid)

        for (seal in sealsComplete) {
            // get the tags for this seal's relatives
            val (relOneTag, relTwoTag) = getRelativesTags(seal.sealType)
            val log = buildObservationRecord(
                currentLocation,
                seal,
                relOneTag,
                relTwoTag,
                uiState.value.metadata,
            )

            // write an entry to the database for each seal that has valid input (or has been confirmed, if invalid)
            viewModelScope.launch {
                observationRepo.addObservation(log)
            }

            //TODO, consider overwriting the database entry, instead of appending a new entry
            // especially if the editmode is set
//                if (primarySeal.observationID != 0) {
//
//                }

        }
        _uiState.update {
            it.copy(
                isSaved = true,
                isSaveAttempted = false,
                isSaveEnabled = true
            )
        }
    }

    private fun getRelativesTags(sealName: SealType): Pair<String, String> {
        when (sealName) {
            SealType.PRIMARY -> {
                var relOneTagId = pupOne.value.tagNumber + pupOne.value.tagAlpha
                var relTwoTagId = pupTwo.value.tagNumber + pupTwo.value.tagAlpha
                return Pair(relOneTagId, relTwoTagId)
            }

            SealType.PUPONE -> {
                var relOneTagId = primarySeal.value.tagNumber + primarySeal.value.tagAlpha
                var relTwoTagId = pupTwo.value.tagNumber + pupTwo.value.tagAlpha
                return Pair(relOneTagId, relTwoTagId)
            }

            SealType.PUPTWO -> {
                var relOneTagId = primarySeal.value.tagNumber + primarySeal.value.tagAlpha
                var relTwoTagId = pupOne.value.tagNumber + pupOne.value.tagAlpha
                return Pair(relOneTagId, relTwoTagId)
            }

            SealType.UNKNOWN -> return Pair("", "")
        }
    }
}