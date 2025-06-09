package weddellseal.markrecap.models

/*
* Main model that stores data entered from the observation page for up to three seals
 */

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observations.Seal
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckSeal
import weddellseal.markrecap.frameworks.room.wedCheck.processTags
import weddellseal.markrecap.ui.tagretag.sealValidation
import weddellseal.markrecap.ui.tagretag.utils.buildLogEntry
import weddellseal.markrecap.ui.tagretag.utils.notebookEntryValueSeal
import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.ui.utils.getDeviceName

class TagRetagModel(
    application: Application,
    private val observationRepo: ObservationRepository,
    private val homeViewUiState: StateFlow<HomeViewModel.UiState>,
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

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
        val isSaveEnabled: Boolean = false,
        val isSaved: Boolean = false,
        val ineligibleForSaveReason: String = "",

        val isPrefilled: Boolean = false,

        val isError: Boolean = false,
        val errorMessage: String = "",

        val validationFailureReason: String = "",
        val isValidated: Boolean = false,
        val validEntry: Boolean = false,

        val observationLogEntry: ObservationLogEntry? = null,
        val recordID: Int = 0,

        val isEditMode: Boolean = false,

        val metadata: ObservationMetadata = ObservationMetadata(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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

    fun setMetadata(metadata: ObservationMetadata) {
        _uiState.update { it.copy(metadata = metadata) }
    }

    private val _primarySeal = MutableStateFlow(Seal(name = "primary", isStarted = false))
    val primarySeal: StateFlow<Seal> = _primarySeal

    private val _primaryWedCheckSeal = MutableStateFlow(WedCheckSeal())
    val primaryWedCheckSeal: StateFlow<WedCheckSeal> = _primaryWedCheckSeal

    private val _pupOne = MutableStateFlow(Seal(name = "pupOne", age = "Pup", isStarted = false))
    val pupOne: StateFlow<Seal> = _pupOne

    private val _pupOneWedCheckSeal = MutableStateFlow(WedCheckSeal())
    val pupOneWedCheckSeal: StateFlow<WedCheckSeal> = _pupOneWedCheckSeal

    private val _pupTwo = MutableStateFlow(Seal(name = "pupTwo", age = "Pup", isStarted = false))
    val pupTwo: StateFlow<Seal> = _pupTwo

    private val _pupTwoWedCheckSeal = MutableStateFlow(WedCheckSeal())
    val pupTwoWedCheckSeal: StateFlow<WedCheckSeal> = _pupTwoWedCheckSeal

    private var wedCheckSealMap = mutableMapOf<String, WedCheckSeal>()

    // Initialize the ViewModel
    init {
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
                    deviceID = getDeviceName(context),
                    currentSeason = getCurrentYear().toString(),
                )

                // Check if save is enabled
                val reasons = buildList {
                    if (!metadata.isValid) add(metadata.invalidReason)
                    if (!primary.isComplete) addAll(primary.completenessReasons)
                    if (pupOne.isStarted && !pupOne.isComplete) addAll(pupOne.completenessReasons)
                    if (pupTwo.isStarted && !pupTwo.isComplete) addAll(pupTwo.completenessReasons)
                }
                // uses Kotlin’s infix function for creating a Pair<ObservationMetadata, List<String>>
                metadata to reasons

            }.collect { (metadata, reasons) ->
                _uiState.update {
                    it.copy(
                        metadata = metadata,
                        isSaveEnabled = reasons.isEmpty(),
                        ineligibleForSaveReason = reasons.joinToString("\n")
                    )
                }
            }
        }
    }

    fun clearValidationState() {
        _uiState.update {
            it.copy(
                validationFailureReason = "",
                isValidated = false,
                validEntry = false
            )
        }

        _primarySeal.update {
            it.copy(
                isValidated = false,
                reasonNotValid = ""
            )
        }

        _pupOne.update {
            it.copy(
                isValidated = false,
                reasonNotValid = ""
            )
        }

        _pupTwo.update {
            it.copy(
                isValidated = false,
                reasonNotValid = ""
            )
        }
    }

    fun updateObservationEntry(observation: ObservationLogEntry) {
        _uiState.update { it.copy(observationLogEntry = observation) }
    }

    fun updateCondition(sealName: String, input: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update { it.copy(condition = input, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update { it.copy(condition = input, isStarted = true) }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(condition = input, isStarted = true)
                }
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

    fun clearTag(seal: Seal) {
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

    fun clearSpeNo(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(speNo = 0, hasWedCheckSpeno = false) }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(speNo = 0, hasWedCheckSpeno = false)
                }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(speNo = 0, hasWedCheckSpeno = false)
                }
            }
        }
    }

    fun updateNoTag(sealName: String) {
        when (sealName) {
            "primary" -> {
                _primarySeal.update {
                    it.copy(
                        isNoTag = true,
                        numTags = "",
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(primarySeal.value)
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(
                        isNoTag = true,
                        numTags = "",
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(pupOne.value)
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(
                        isNoTag = true,
                        numTags = "",
                        tagAlpha = "",
                        tagNumber = "",
                    )
                }
                updateNotebookEntry(pupTwo.value)
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

    fun updateTagAlpha(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                _primarySeal.update { it.copy(tagAlpha = input, isStarted = true) }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(tagAlpha = input, isStarted = true)
                }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(tagAlpha = input, isStarted = true)
                }
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
    fun populateSeal(lookupSeal: WedCheckSeal) {
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
                speNo = lookupSeal.speNo,
                age = sealAgeAdvanced, // expecting to advance the seal age based on the last season seen
                sex = lookupSeal.sex,
                numRelatives = numberRels,
                tagNumber = lookupSeal.tagOneNumber,
                tagAlpha = lookupSeal.tagOneAlpha,
                oldTagId = lookupSeal.tagIdOne,
                tagEventType = "Marked",
                lastPhysio = lookupSeal.lastPhysio,
                colony = lookupSeal.colony,
                isWedCheck = true,
            )
        }
        _primaryWedCheckSeal.update { lookupSeal }

//        val tagID = lookupSeal.tagOneNumber + lookupSeal.tagOneAlpha
//        addWedCheckSeal(tagID, lookupSeal)
        updateNotebookEntry(primarySeal.value)
    }

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen
    // prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun populateSealFromObservation(log: ObservationLogEntry?) {
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

            val condition = when (log.sealCondition) {
                "0" -> "Dead - 0"
                "1" -> "Poor - 1"
                "2" -> "Fair - 2"
                "3" -> "Good - 3"
                "4" -> "Newborn - 4"
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
                    speNo = log.speno.toInt(),
                    age = ageString, // expecting to advance the seal age based on the last season seen
                    sex = sealSex,
                    numRelatives = log.numRelatives,
                    condition = condition,
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
                    hasWedCheckSpeno = true,
                    observationID = log.id,
                    isObservationLogEntry = true,
                )
            }
        }
        updateNotebookEntry(primarySeal.value)
    }


    fun mapSpeno(name: String, wedCheckSeal: WedCheckSeal) {
        when (name) {
            "primary" -> {
                _primarySeal.update {
                    it.copy(
                        speNo = wedCheckSeal.speNo,
                        hasWedCheckSpeno = true,
                    )
                }
                _primaryWedCheckSeal.update { wedCheckSeal }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(
                        speNo = wedCheckSeal.speNo,
                        hasWedCheckSpeno = true,
                    )
                }
                _pupOneWedCheckSeal.update { wedCheckSeal }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(
                        speNo = wedCheckSeal.speNo,
                        hasWedCheckSpeno = true,
                    )
                }
                _pupTwoWedCheckSeal.update { wedCheckSeal }
            }
        }
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

    // called after navigation command from the summary screen to prevent the summary screen from
    // preemptively navigating back to the observation screen
    fun resetSaved() {
        // reset the values in the model once the records are save successfully
        wedCheckSealMap = mutableMapOf()

        _uiState.update {
            it.copy(
                validationFailureReason = "",
                isValidated = false,
                validEntry = false,
                isSaved = false,
                isError = false,
                errorMessage = "",
                isPrefilled = false,
            )
        }

        _primarySeal.update { Seal(name = "primary", isStarted = false) }
        _pupOne.update { Seal(name = "pupOne", age = "Pup", isStarted = false) }
        _pupTwo.update { Seal(name = "pupTwo", age = "Pup", isStarted = false) }
    }

    fun createLog(
        currentLocation: GeoLocation?,
    ) {
        val sealsList = listOf(primarySeal.value, pupOne.value, pupTwo.value)
        _uiState.update { it.copy(isSaved = false) }

        for (seal in sealsList) {
            if (seal.isStarted) {
                // get the tags for this seal's relatives
                val (relOneTag, relTwoTag) = getRelativesTags(seal.name)
                val log = buildLogEntry(
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

                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }

    // validate is called when the observer saves an entry
    // the model state is used to prompt the user to review and confirm their entry if validation fails
    // validation errors are saved to the seal state for inclusion in the output file
    fun validate(seal: Seal, wedChecKSealMatch: WedCheckSeal) {
        // reset the model state values that help determine whether the seal entry is valid
        _uiState.update {
            it.copy(
                validationFailureReason = "",
                isValidated = false,
                validEntry = false
            )
        }

        // set the name used in the validation string to match the UI display
        var sealName = seal.name
        if (seal.name == "primary") {
            sealName = "Seal"
        }

        // if the event type is marked or retag, the seal must have a speNo(matching WedCheck record)
        var tagID = seal.tagNumber + seal.tagAlpha
        if (seal.tagEventType == "Retag") { // if retag set the tag id to the old tag
            tagID = seal.oldTagId
        }

//        val wedCheckSeal =
//            getWedCheckSeal(tagID) // attempt to locate the WedCheck seal in the map stored in the model

        // use the utility method to validate the seal
        val (sealValid, validationErrors) = sealValidation(
            seal,
            getCurrentYear(),
            wedChecKSealMatch
        )

        // update the model state with the validation state
        if (sealValid) {
            _uiState.update { it.copy(isValidated = true, validEntry = true) }
        } else {
            val invalidEntrySB = StringBuilder()
            invalidEntrySB.append("$sealName failed validation!")
            invalidEntrySB.append(_uiState.value.validationFailureReason)
            invalidEntrySB.append("\n")
            invalidEntrySB.append(validationErrors)
            _uiState.update {
                it.copy(
                    validationFailureReason = invalidEntrySB.toString(),
                    isValidated = true,
                    validEntry = false
                )
            }
        }

        // update the seal with the validation state
        when (seal.name) {
            "primary" -> {
                _primarySeal.update {
                    it.copy(
                        isValidated = true,
                        reasonNotValid = validationErrors
                    )
                }
            }

            "pupOne" -> {
                _pupOne.update {
                    it.copy(
                        isValidated = true,
                        reasonNotValid = validationErrors
                    )
                }
            }

            "pupTwo" -> {
                _pupTwo.update {
                    it.copy(
                        isValidated = true,
                        reasonNotValid = validationErrors
                    )
                }
            }
        }
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
}