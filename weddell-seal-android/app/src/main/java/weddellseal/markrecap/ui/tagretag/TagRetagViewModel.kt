package weddellseal.markrecap.ui.tagretag

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.RetagReason
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observations.toSeal
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.wedCheck.toSeal
import weddellseal.markrecap.ui.UiEvent
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.home.ObservationMetadata
import weddellseal.markrecap.ui.recentobservations.DisplayObservation
import weddellseal.markrecap.ui.tagretag.utils.buildObservationRecord
import weddellseal.markrecap.ui.tagretag.utils.notebookEntryValueSeal
import weddellseal.markrecap.ui.utils.getCurrentYear

class TagRetagViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
    private val wedCheckRepo: WedCheckRepository,
    private val metadata: StateFlow<ObservationMetadata>,
    private val homeViewUiState: StateFlow<HomeViewModel.UiState>,
) : AndroidViewModel(application) {

    data class UiState(
        val originalMetadata: ObservationMetadata = ObservationMetadata(selectedColony = null), // when a record is edited, these are the values that were originally saved for the observation

        val isSearching: Boolean = false, // indicator for when searching a wedcheck seal

        val isPrefilled: Boolean = false, // indicator for pre-filled form for Census

        val isEditMode: Boolean = false, // indicator that an existing record (WedCheck or Observation) is being edited
        val observationTimestamp: String = "", // UI display value in Tag/Retag screen header, values originally saved for the observation
        val observationLocation: GeoLocation? = null, // location originally saved for the observation

        val isSaveAttempted: Boolean = false, // indicator that user is attempting to save the record
        val isSaveEnabled: Boolean = false, // indicator for save button

        val ineligibleForSaveReason: String = "", // reasons save button is disabled

        val allSealsValid: Boolean = false, // indicator that all seals are valid

        val validationFailureReason: String = "", // reason for validation failure
        val entryNeedsConfirmation: Boolean = false, // indicator that the user needs to confirm the entry

        /** Incremented on [resetModelState] so tag text fields drop leftover local state (fix #3). */
        val tagFieldResetGeneration: Int = 0,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _primarySeal = MutableStateFlow(Seal(sealType = SealType.PRIMARY))
    val primarySeal: StateFlow<Seal> = _primarySeal

    private val _primarySealEdits = MutableStateFlow<List<String>>(emptyList())
    val primarySealEdits: StateFlow<List<String>> = _primarySealEdits

    private val _pupOne =
        MutableStateFlow(Seal(sealType = SealType.PUPONE, ageClass = SealAgeClass.PUP))
    val pupOne: StateFlow<Seal> = _pupOne

    private val _pupOneEdits = MutableStateFlow<List<String>>(emptyList())
    val pupOneEdits: StateFlow<List<String>> = _pupOneEdits

    private val _pupTwo =
        MutableStateFlow(Seal(sealType = SealType.PUPTWO, ageClass = SealAgeClass.PUP))
    val pupTwo: StateFlow<Seal> = _pupTwo

    private val _pupTwoEdits = MutableStateFlow<List<String>>(emptyList())
    val pupTwoEdits: StateFlow<List<String>> = _pupTwoEdits

    private var originalPrimarySeal: Seal? = null
    private var originalPupOne: Seal? = null
    private var originalPupTwo: Seal? = null

    /** BUG FIX #1: In-progress tag numbers from [TagIDOutlinedTextField] before blur commits to the seal model. */
    private val pendingTagNumbers = mutableMapOf<SealType, String>()
    private val pendingOldTagNumbers = mutableMapOf<SealType, String>()

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

    private val _selectedRecentObservation = MutableStateFlow<DisplayObservation?>(null)
    val selectedRecentObservation: StateFlow<DisplayObservation?> get() = _selectedRecentObservation

    private val _hasEdits = MutableStateFlow(false)
    val hasEdits: StateFlow<Boolean> = _hasEdits

    fun onEditAttempt(observation: DisplayObservation) {
        viewModelScope.launch {
            if (!primarySeal.value.isEntryStarted) {
                _selectedRecentObservation.value = observation // set the observation to edit
                _uiEvent.emit(UiEvent.ShowEditDialog)
            } else {
                _uiEvent.emit(
                    UiEvent.ShowEditToast("Looks like you're already editing another seal! Save or clear, then edit this record.")
                )
            }
        }
    }

    fun exitEditMode() {
        _selectedRecentObservation.value = null // reset the observation to edit
        resetModelState()
    }

    fun onViewAttempt(observation: DisplayObservation) {
        // Set synchronously so ObservationViewer has data on first composition
        // (navigation from RecentObservations runs in the same frame).
        _selectedRecentObservation.value = observation
    }

    fun prefillSingleMale() {
        _primarySeal.update {
            it.copy(
                ageClass = SealAgeClass.ADULT,
                sex = SealSex.MALE,
                numRelatives = SealRelatives.ZERO
            )
        }
        _uiState.update { it.copy(isPrefilled = true) }
    }

    fun prefillSingleFemale() {
        _primarySeal.update {
            it.copy(
                ageClass = SealAgeClass.ADULT,
                sex = SealSex.FEMALE,
                numRelatives = SealRelatives.ZERO
            )
        }
        _uiState.update { it.copy(isPrefilled = true) }
    }

    fun prefillMomAndPup() {
        _primarySeal.update {
            it.copy(
                ageClass = SealAgeClass.ADULT,
                sex = SealSex.FEMALE,
                numRelatives = SealRelatives.ONE
            )
        }
        _pupOne.update { it.copy(numRelatives = SealRelatives.ONE) }
        _uiState.update { it.copy(isPrefilled = true) }
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

    // called:
    // 1. after navigation command from the recent observation screen when a record is to be edited
    // 2. when a save is successful
    // 3. when a record is selected for editing from the Tag/Retag screen
    fun resetModelState() {
        _uiState.update {
            it.copy(
                isSearching = false,
                isPrefilled = false,
                isEditMode = false,
                observationTimestamp = "",
                originalMetadata = ObservationMetadata(selectedColony = null),
                observationLocation = null,
                isSaveAttempted = false,
                isSaveEnabled = false,
                ineligibleForSaveReason = "",
                validationFailureReason = "",
                entryNeedsConfirmation = false,
                tagFieldResetGeneration = it.tagFieldResetGeneration + 1,
            )
        }

        _primarySeal.update {
            Seal(
                sealType = SealType.PRIMARY,
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

        // reset the edit seal comparison values
        originalPrimarySeal = null
        originalPupOne = null
        originalPupTwo = null
        pendingTagNumbers.clear()
        pendingOldTagNumbers.clear()
    }

    fun setIsSaving() {
        _uiState.update { it.copy(isSaveAttempted = true, isSaveEnabled = false) }
    }

    private fun allSealsValid(
        primary: Seal = _primarySeal.value,
        pupOne: Seal = _pupOne.value,
        pupTwo: Seal = _pupTwo.value,
    ): Boolean =
        primary.isValid &&
            (!primary.hasPupOne || pupOne.isValid) &&
            (!primary.hasPupTwo || pupTwo.isValid)

    /**
     * Save button entry point (fix #1 + #2).
     *
     * Fix #1: [TagIDOutlinedTextField] keeps in-progress tag edits in [pendingTagNumbers] until
     * blur. We must [commitPendingTagNumbers] before validating or writing, and read seal state
     * from the ViewModel — not from a Compose snapshot captured at click time.
     *
     * Fix #2: After commit, [refreshAllWedCheckMatches] awaits WedCheck lookups so Marked/Retag
     * validation and speno assignment use the committed tag, not a stale or in-flight match.
     */
    fun attemptSave(currentLocation: GeoLocation?) {
        commitPendingTagNumbers()
        setIsSaving()

        viewModelScope.launch {
            refreshAllWedCheckMatches()

            val primary = _primarySeal.value
            val pupOne = _pupOne.value
            val pupTwo = _pupTwo.value

            if (allSealsValid(primary, pupOne, pupTwo)) {
                writeObservationRecord(currentLocation)
            } else {
                checkNeedsConfirmation(
                    primary.validationErrors,
                    pupOne.validationErrors,
                    pupTwo.validationErrors,
                )
            }
        }
    }

    /**
     * Confirm & Save entry point after the validation banner is shown.
     *
     * Runs in the ViewModel so flag-for-review and persistence use committed tag values and
     * resolved WedCheck matches (same fix #1 / #2 requirements as [attemptSave]).
     * [setIsSaving] is not called here; [UiState.isSaveAttempted] is already true from the first Save tap.
     */
    fun confirmAndSave(currentLocation: GeoLocation?) {
        commitPendingTagNumbers()

        viewModelScope.launch {
            refreshAllWedCheckMatches()

            val primary = _primarySeal.value
            val pupOne = _pupOne.value
            val pupTwo = _pupTwo.value

            if (!primary.isValid) {
                flagSealForReview(primary.sealType)
            }
            if (!pupOne.isValid) {
                flagSealForReview(pupOne.sealType)
            }
            if (!pupTwo.isValid) {
                flagSealForReview(pupTwo.sealType)
            }

            writeObservationRecord(currentLocation)
        }
    }

    fun editAfterAttemptedSave() {
        _uiState.update {
            it.copy(
                isSaveAttempted = false,
                isSaveEnabled = true,
                entryNeedsConfirmation = false,
            )
        }
    }

    // Initialize the ViewModel
    init {
        // Observe homeViewModel metadata
        // Observe seal validity & save eligibility
        viewModelScope.launch {
            // Each time any of these change:
            // 1) check each seal (primary, pup one, pup two) to see if their information is complete and valid.
            // 2) create a list of reasons why the data isn’t ready to save.
            combine(
                _primarySeal,
                _pupOne,
                _pupTwo
            ) { primary, pupOne, pupTwo -> Triple(primary, pupOne, pupTwo) }
                .combine(metadata) { triple, metadata -> Pair(triple, metadata) }
                .combine(homeViewUiState.map { it.overrideColony }) { pair, overrideColony ->
                    Triple(pair.first.first, pair.first.second, pair.first.third to overrideColony)
                }
                .combine(uiState.map { it.isEditMode }) { triple, editMode ->
                    val primary = triple.first
                    val pupOne = triple.second
                    val pupTwo = triple.third.first

                    val overrideColony = triple.third.second

                    // Check if save is enabled
                    val reasons = buildList {
                        if (!editMode) { // edit mode uses the original metadata, don't validate the current metadata
                            if (!metadata.value.isValid) add(metadata.value.invalidReason)
                        }
                        if (overrideColony) {
                            if (!metadata.value.isSelectedColonyValid) add(metadata.value.invalidColonyReason)
                        }
                        if (!primary.isEntryStarted) add("Missing all required fields!") // condition upon starting observation entry
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

                }.collectLatest { (_, reasons, allSealsValid) ->
                    _uiState.update {
                        it.copy(
                            isSaveEnabled = reasons.isEmpty(),
                            ineligibleForSaveReason = reasons.joinToString("\n"),
                            allSealsValid = allSealsValid
                        )
                    }
                }
        }

        // Observe seal edits
        viewModelScope.launch {
            combine(
                _primarySeal,
                _pupOne,
                _pupTwo,
                uiState.map { it.isEditMode }, // wrap the snapshot value of isEditMode in a Flow<Boolean>
            ) { currentPrimary, currentPupOne, currentPupTwo, editMode ->

                if (!editMode) return@combine Triple(
                    emptyList<String>(),
                    emptyList<String>(),
                    emptyList<String>()
                )

                // Check if edits have been made
                val primarySealEdits = currentPrimary.edits(originalPrimarySeal)
                val pupOneSealEdits = currentPupOne.edits(originalPupOne)
                val pupTwoSealEdits = currentPupTwo.edits(originalPupTwo)

                // emit a Triple that can be unpacked in `collect`
                Triple(primarySealEdits, pupOneSealEdits, pupTwoSealEdits)

            }.collectLatest { (primarySealEdits, pupOneSealEdits, pupTwoSealEdits) ->
                var edits = emptyList<String>()

                if (primarySealEdits.isNotEmpty()) {
                    edits = edits.plus(primarySealEdits)
                    _primarySealEdits.update { primarySealEdits }
                    _primarySeal.update { it.copy(hasEdits = true) }
                }
                if (pupOneSealEdits.isNotEmpty()) {
                    edits = edits.plus(pupOneSealEdits)
                    _pupOneEdits.update { pupOneSealEdits }
                    _pupOne.update { it.copy(hasEdits = true) }
                }
                if (pupTwoSealEdits.isNotEmpty()) {
                    edits = edits.plus(pupTwoSealEdits)
                    _pupTwoEdits.update { pupTwoSealEdits }
                    _pupTwo.update { it.copy(hasEdits = true) }
                }

                if (edits.isNotEmpty()) {
                    _hasEdits.value = true
                }
            }
        }

        // Observe census mode to default Event Type to Marked
        viewModelScope.launch {
            combine(
                _primarySeal,
                metadata.map { it.isCensusMode }, // wrap the snapshot value of isEditMode in a Flow<Boolean>.
            ) { currentPrimary, censusMode ->

                val setEventTypeMarked =
                    censusMode &&
                            currentPrimary.tagEventType == TagEventType.UNKNOWN &&
                            currentPrimary.isEntryStarted

                setEventTypeMarked

            }.collectLatest { setEventTypeMarked ->
                if (setEventTypeMarked) {
                    _primarySeal.update { it.copy(tagEventType = TagEventType.MARKED) }
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
                    val sealFound = withContext(Dispatchers.IO) {
                        wedCheckRepo.findSealbyTagID(searchTagID.trim())
                    }

                    // Guard against stale results if the tag changed or the form was reset
                    // while this lookup was in flight.
                    applyWedCheckLookupResult(seal.sealType, searchTagID, sealFound)
                } catch (e: Exception) {
                    Log.e("SealLookup", "Error fetching seal: ${e.localizedMessage}", e)
                }
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    /** Tag string used for WedCheck lookup: current tag ID (Marked/New) or old tag ID (Retag). */
    private fun wedCheckSearchTagFor(seal: Seal): String? = when {
        seal.useTagID && seal.isTagIDValid -> seal.tagNumber + seal.tagAlpha
        seal.useOldTag && seal.isOldTagValid -> seal.oldTagNumber + seal.oldTagAlpha
        else -> null
    }

    /**
     * Blocking WedCheck lookup for the save path (fix #2).
     *
     * [requestCurrentWedCheckMatch] / [findWedCheckMatch] are async; if we build the observation
     * record before they finish, Marked/Retag entries are saved with speno "0" even when the tag
     * exists in WedCheck. Only Marked and Retag need a match for speno; New tags intentionally skip.
     */
    private suspend fun resolveWedCheckForSeal(seal: Seal): Seal {
        if (seal.isNoTag) return seal
        if (seal.tagEventType != TagEventType.MARKED && seal.tagEventType != TagEventType.RETAG) {
            return seal
        }

        val searchTag = wedCheckSearchTagFor(seal) ?: return seal
        if (seal.wedCheckMatch?.tagIdOne == searchTag) return seal

        return try {
            val record = withContext(Dispatchers.IO) {
                wedCheckRepo.findSealbyTagID(searchTag.trim())
            }
            seal.copy(wedCheckMatch = record.toSeal())
        } catch (e: Exception) {
            Log.e("SealLookup", "Error fetching seal for save: ${e.localizedMessage}", e)
            seal.copy(wedCheckMatch = null)
        }
    }

    /**
     * Applies an async WedCheck lookup result only if the seal still has the same search tag.
     * Prevents a slow in-flight lookup from attaching speno to the wrong tag or to a reset form.
     */
    private fun applyWedCheckLookupResult(
        sealType: SealType,
        searchTagID: String,
        sealFound: WedCheckRecord,
    ) {

        val currentSeal = when (sealType) {
            SealType.PRIMARY -> _primarySeal.value
            SealType.PUPONE -> _pupOne.value
            SealType.PUPTWO -> _pupTwo.value
            SealType.UNKNOWN -> return
        }

        val expectedTag = wedCheckSearchTagFor(currentSeal) ?: return
        if (expectedTag != searchTagID.trim()) return

        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(wedCheckMatch = sealFound.toSeal()) }
            }

            else -> {}
        }
    }

    private suspend fun refreshWedCheckMatchFor(sealType: SealType) {
        val seal = when (sealType) {
            SealType.PRIMARY -> _primarySeal.value
            SealType.PUPONE -> _pupOne.value
            SealType.PUPTWO -> _pupTwo.value
            SealType.UNKNOWN -> return
        }
        if (!seal.isEntryStarted) return

        val resolved = resolveWedCheckForSeal(seal)
        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(wedCheckMatch = resolved.wedCheckMatch) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(wedCheckMatch = resolved.wedCheckMatch) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(wedCheckMatch = resolved.wedCheckMatch) }
            }

            else -> {}
        }
    }

    /** Updates seal state with awaited WedCheck results before save validation or persistence. */
    private suspend fun refreshAllWedCheckMatches() {
        refreshWedCheckMatchFor(SealType.PRIMARY)
        refreshWedCheckMatchFor(SealType.PUPONE)
        refreshWedCheckMatchFor(SealType.PUPTWO)
    }

    fun getPupOneNotebookString(): String {
        return pupOne.value.notebookDataString
    }

    fun getPupTwoNotebookString(): String {
        return pupTwo.value.notebookDataString
    }

    fun updateAge(sealType: SealType, input: SealAgeClass) {
        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(ageClass = input) }
                updateNotebookEntry(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(ageClass = input) }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(ageClass = input) }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateSex(sealType: SealType, input: SealSex) {
        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(sex = input) }
                updateNotebookEntry(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(sex = input) }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(sex = input) }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updatePupPeed(sealType: SealType, input: Boolean) {
        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(pupPeed = input) }
                updateNotebookEntry(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(pupPeed = input) }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(pupPeed = input) }
                updateNotebookEntry(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateNumRelatives(input: SealRelatives) {
        when (input) {
            SealRelatives.ZERO -> {
                removePups()
                _primarySeal.update { it.copy(pupAdded = false) } // to support edit mode
                updateNotebookEntry(primarySeal.value)
            }

            SealRelatives.ONE -> {
                _pupOne.update {
                    it.copy(numRelatives = input)
                }
                _pupTwo.update {
                    it.copy(numRelatives = input)
                }
                _primarySeal.update { it.copy(pupAdded = true) } // to support edit mode
                updateNotebookEntry(pupOne.value)
                updateNotebookEntry(pupTwo.value)
            }

            SealRelatives.TWO -> {
                _pupOne.update {
                    it.copy(numRelatives = input)
                }
                _pupTwo.update {
                    it.copy(numRelatives = input)
                }
                _primarySeal.update { it.copy(pupAdded = true) } // to support edit mode
                updateNotebookEntry(pupOne.value)
                updateNotebookEntry(pupTwo.value)
            }

            SealRelatives.UNKNOWN -> {
                removePups()
                _primarySeal.update { it.copy(pupAdded = false) }
                updateNotebookEntry(primarySeal.value)
            }
        }

        _primarySeal.update { it.copy(numRelatives = input) }
        updateNotebookEntry(primarySeal.value)
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

    fun updatePendingTagNumber(sealType: SealType, input: String) {
        if (sealType == SealType.UNKNOWN) return
        pendingTagNumbers[sealType] = input
    }

    fun updatePendingOldTagNumber(sealType: SealType, input: String) {
        if (sealType == SealType.UNKNOWN) return
        pendingOldTagNumbers[sealType] = input
    }

    private fun commitPendingTagNumbers() {
        // Flush in-progress tag ID edits from the UI before validation or persistence (fix #1).
        pendingTagNumbers.toMap().forEach { (sealType, number) ->
            updateTagNumber(sealType, number)
        }
        pendingOldTagNumbers.toMap().forEach { (sealType, number) ->
            updateOldTagNumber(sealType, number)
        }
    }

    // TODO, why is this a string input and not a numeric input, see updateOldTagNumber
    fun updateTagNumber(sealType: SealType, input: String) {
        var tagNumber = input
        // Function to extract numeric value
        if (input.toIntOrNull() != null) {
            tagNumber = input
        }

        when (sealType) {
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
        pendingTagNumbers.remove(sealType)
    }

    fun updateTagAlpha(sealType: SealType, input: String) {
        when (sealType) {
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

    fun updateOldTagNumber(sealType: SealType, input: String) {
        when (sealType) {
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
        pendingOldTagNumbers.remove(sealType)
    }

    fun updateOldTagAlpha(sealType: SealType, input: String) {
        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(oldTagAlpha = input) }
                updateNotebookEntry(primarySeal.value)
                requestCurrentWedCheckMatch(primarySeal.value)
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(oldTagAlpha = input) }
                updateNotebookEntry(pupOne.value)
                requestCurrentWedCheckMatch(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(oldTagAlpha = input) }
                updateNotebookEntry(pupTwo.value)
                requestCurrentWedCheckMatch(pupTwo.value)
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    fun updateRetagReason(sealType: SealType, input: RetagReason) {
        when (sealType) {
            SealType.PRIMARY -> {
                _primarySeal.update { it.copy(reasonForRetag = input) }
            }

            SealType.PUPONE -> {
                _pupOne.update { it.copy(reasonForRetag = input) }
                updateNotebookEntry(pupOne.value)
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(reasonForRetag = input) }
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
                _pupTwo.update { it.copy(oldTagMarks = oldTagMarks) }
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

    fun clearTagID(sealType: SealType) {
        when (sealType) {
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

    // When retag is selected
    // 1) Old Tag ID is set to the current Tag ID
    // 2) Tag ID should be cleared
    fun onRetagSelection(sealType: SealType, tagNumber: String, tagAlpha: String) {
        //TODO, need to think about the value when populated from lookup
        // Set Old Tag ID to current Tag ID
        updateOldTagNumber(sealType, tagNumber)
        updateOldTagAlpha(sealType, tagAlpha)

        clearTagID(sealType)
    }

    fun onRetagDeselection(sealType: SealType, oldTagNumber: String, oldTagAlpha: String) {
        // Set Tag ID to Old Tag ID when toggling back to Marked or New from Retag
        updateTagNumber(sealType, oldTagNumber)
        updateTagAlpha(sealType, oldTagAlpha)

        clearOldTag(sealType)
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

    // TODO, why did this become unused???
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

    fun markPupRemoved(sealName: SealType) {
        when (sealName) {
            SealType.PUPONE -> {
                _pupOne.update { it.copy(markedRemoved = true) }
                _primarySeal.update { it.copy(pupOneRemoved = true) }
            }

            SealType.PUPTWO -> {
                _pupTwo.update { it.copy(markedRemoved = true) }
                _primarySeal.update { it.copy(pupTwoRemoved = true) }
            }

            else -> {
                // No action for primary seal
            }
        }
    }

    fun resetSeal(sealName: SealType) {
        var parentNumRels = primarySeal.value.numRelatives
        if (primarySeal.value.numRelatives != SealRelatives.UNKNOWN) {
            var number = parentNumRels.value
            number -= 1
            parentNumRels = SealRelatives.fromIntVal(number)
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
        val numberRels = if (lookupSeal.sex == SealSex.FEMALE
            && sealAgeAdvanced == SealAgeClass.ADULT
        ) SealRelatives.UNKNOWN else SealRelatives.ZERO

        _primarySeal.update {
            it.copy(
                ageClass = sealAgeAdvanced,
                sex = lookupSeal.sex,
                numRelatives = numberRels,
                tagNumber = lookupSeal.tagOneNumber,
                tagAlpha = lookupSeal.tagOneAlpha,
                oldTagNumber = lookupSeal.tagOneNumber,
                oldTagAlpha = lookupSeal.tagOneAlpha,
                tagEventType = TagEventType.MARKED, // WedCheck seals are Marked by default
                lastPhysio = lookupSeal.lastPhysio,
                colony = lookupSeal.population,
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
                _uiState.update {
                    val observationMetaData = ObservationMetadata(
                        selectedColony = SealColony(
                            colonyId = 0,
                            inOut = "",
                            location = displayObservation.primarySeal.colony,
                            nLimit = 0.0,
                            sLimit = 0.0,
                            wLimit = 0.0,
                            eLimit = 0.0,
                            adjLong = 0.0,
                            adjLat = 0.0,
                            fileUploadId = 0
                        ),
                        selectedObservers = listOf(displayObservation.primarySeal.observerInitials),
                        censusNumber = displayObservation.primarySeal.censusID,
                        currentSeason = displayObservation.primarySeal.season,
                        deviceID = displayObservation.primarySeal.deviceID,
                        originalDate = displayObservation.primarySeal.date,
                        originalTimestamp = displayObservation.primarySeal.time
                    )

                    it.copy(
                        originalMetadata = observationMetaData,
                        observationLocation = safeGeoLocation(
                            displayObservation.primarySeal.latitude,
                            displayObservation.primarySeal.longitude
                        ),
                        observationTimestamp = displayObservation.primarySeal.date + " " + displayObservation.primarySeal.time
                    )
                }

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
                _uiState.update {
                    val observationMetaData = ObservationMetadata(
                        selectedColony = SealColony(
                            colonyId = 0,
                            inOut = "",
                            location = displayObservation.primarySeal.colony,
                            nLimit = 0.0,
                            sLimit = 0.0,
                            wLimit = 0.0,
                            eLimit = 0.0,
                            adjLong = 0.0,
                            adjLat = 0.0,
                            fileUploadId = 0
                        ),
                        selectedObservers = listOf(displayObservation.primarySeal.observerInitials),
                        censusNumber = displayObservation.primarySeal.censusID,
                        currentSeason = displayObservation.primarySeal.season,
                        deviceID = displayObservation.primarySeal.deviceID,
                        originalDate = displayObservation.primarySeal.date,
                        originalTimestamp = displayObservation.primarySeal.time
                    )

                    it.copy(
                        originalMetadata = observationMetaData,
                        observationLocation = safeGeoLocation(
                            displayObservation.primarySeal.latitude,
                            displayObservation.primarySeal.longitude
                        ),
                        observationTimestamp = displayObservation.primarySeal.date + " " + displayObservation.primarySeal.time,
                    )
                }

                _primarySeal.update {
                    displayObservation.primarySeal.toSeal()
                        .copy(sealType = SealType.PRIMARY)
                }

                updateNotebookEntry(primarySeal.value)
            }
        }

        originalPrimarySeal = primarySeal.value // set the original value for comparison
        originalPupOne = pupOne.value // set the original value for comparison
        originalPupTwo = pupTwo.value // set the original value for comparison

        _uiState.update { it.copy(isEditMode = true) }
    }

    fun flagSealForReview(type: SealType) {
        val confirmed = "technician confirmed"
        when (type) {
            SealType.PRIMARY -> {
                val updatedComment = _primarySeal.value.comment + confirmed
                _primarySeal.update { it.copy(flaggedForReview = true, comment = updatedComment) }
            }

            SealType.PUPONE -> {
                val updatedComment = _pupOne.value.comment + confirmed
                _pupOne.update { it.copy(flaggedForReview = true, comment = updatedComment) }
            }

            SealType.PUPTWO -> {
                val updatedComment = _pupTwo.value.comment + confirmed
                _pupTwo.update { it.copy(flaggedForReview = true, comment = updatedComment) }
            }

            SealType.UNKNOWN -> {
                // No action needed for UNKNOWN
            }
        }
    }

    /**
     * Persists observation record(s) to the database.
     *
     * Suspend so WedCheck can be resolved and writes can complete before [resetModelState].
     * [commitPendingTagNumbers] is also called from [attemptSave] / [confirmAndSave]; kept here
     * as a safety net when this function is invoked directly (e.g. unit tests).
     */
    suspend fun writeObservationRecord(
        currentLocation: GeoLocation?,
    ) {
        Log.i("writeObservationRecord", "latitude at time of write: ${currentLocation?.coordinates?.latitude}")

        commitPendingTagNumbers()

        if (uiState.value.isEditMode) {

            // filter for seals that are to be REMOVED
            // this covers removing pups from female observation record
            val sealsToRemove = listOf(primarySeal.value, pupOne.value, pupTwo.value)
                .filter { it.markedRemoved }

            for (seal in sealsToRemove) {
                observationRepo.deleteObservation(seal.observationID)
            }

            if (primarySeal.value.pupAdded) { // TODO TEST, be wary of race condition
                // remove the primary record and add a new observation records for mom with the pup
                // this action supports ordering the mom and pup together
                observationRepo.deleteObservation(primarySeal.value.observationID)
            }

            // filter for seals that are to be UPDATED
            val sealToUpdate = listOf(primarySeal.value, pupOne.value, pupTwo.value)
                .filter { !it.markedRemoved && it.hasEdits }

            for (seal in sealToUpdate) {
                val edits = when (seal.sealType) {
                    SealType.PRIMARY -> {
                        primarySealEdits.value.joinToString("; ")
                    }

                    SealType.PUPONE -> {
                        pupOneEdits.value.joinToString("; ")
                    }

                    SealType.PUPTWO -> {
                        pupTwoEdits.value.joinToString("; ")
                    }

                    SealType.UNKNOWN -> ""
                }

                // get the tags for this seal's relatives
                val (relOneTag, relTwoTag) = getRelativesTags(seal.sealType)
                // Await WedCheck so speno is populated before building the record (fix #2).
                val sealForRecord = resolveWedCheckForSeal(seal)
                val observationRecord = buildObservationRecord(
                    uiState.value.observationLocation,
                    sealForRecord,
                    edits,
                    relOneTag,
                    relTwoTag,
                    uiState.value.originalMetadata,
                )

                // write an entry to the database for each seal
                observationRepo.writeObservation(observationRecord)
            }

        } else {
            // new seal observation records
            val sealsComplete = listOf(primarySeal.value, pupOne.value, pupTwo.value)
                // filter out any seals that aren't complete, are marked deleted, or have edits
                .filter { it.isComplete && !it.markedRemoved && !it.hasEdits }

            for (seal in sealsComplete) {
                Log.i(TAG, "current location at the time of save ${currentLocation?.coordinates?.latitude}")
                // get the tags for this seal's relatives
                val (relOneTag, relTwoTag) = getRelativesTags(seal.sealType)
                // Await WedCheck so speno is populated before building the record (fix #2).
                val sealForRecord = resolveWedCheckForSeal(seal)
                val observationRecord = buildObservationRecord(
                    currentLocation,
                    sealForRecord,
                    "",
                    relOneTag,
                    relTwoTag,
                    metadata.value
                )

                // write an entry to the database for each seal
                observationRepo.writeObservation(observationRecord)
            }

            _uiEvent.emit(
                UiEvent.ShowSavedToast("Record for ${primarySeal.value.notebookDataString} saved!")
            )
        }

        // Safe to reset only after awaited WedCheck resolution and DB writes complete.
        resetModelState()
    }

    private fun getRelativesTags(sealName: SealType): Pair<String, String> {
        when (sealName) {
            SealType.PRIMARY -> {
                val relOneTagId = pupOne.value.tagNumber + pupOne.value.tagAlpha
                val relTwoTagId = pupTwo.value.tagNumber + pupTwo.value.tagAlpha
                return Pair(relOneTagId, relTwoTagId)
            }

            SealType.PUPONE -> {
                val relOneTagId = primarySeal.value.tagNumber + primarySeal.value.tagAlpha
                val relTwoTagId = pupTwo.value.tagNumber + pupTwo.value.tagAlpha
                return Pair(relOneTagId, relTwoTagId)
            }

            SealType.PUPTWO -> {
                val relOneTagId = primarySeal.value.tagNumber + primarySeal.value.tagAlpha
                val relTwoTagId = pupOne.value.tagNumber + pupOne.value.tagAlpha
                return Pair(relOneTagId, relTwoTagId)
            }

            SealType.UNKNOWN -> return Pair("", "")
        }
    }
}

fun safeGeoLocation(lat: String?, lon: String?): GeoLocation? {
    val latDouble = lat?.toDoubleOrNull()
    val lonDouble = lon?.toDoubleOrNull()

    return if (latDouble != null && lonDouble != null) {
        GeoLocation(Coordinates(latDouble, lonDouble))
    } else
        null
}
