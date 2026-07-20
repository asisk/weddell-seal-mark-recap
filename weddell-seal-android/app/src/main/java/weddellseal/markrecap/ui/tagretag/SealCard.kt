package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.dialogs.RemoveDialog
import weddellseal.markrecap.ui.tagretag.sealcard.AgeSection
import weddellseal.markrecap.ui.tagretag.sealcard.ConditionSection
import weddellseal.markrecap.ui.tagretag.sealcard.PupWeightSection
import weddellseal.markrecap.ui.tagretag.sealcard.RelativesSection
import weddellseal.markrecap.ui.tagretag.sealcard.RetagReasonSection
import weddellseal.markrecap.ui.tagretag.sealcard.SexSection
import weddellseal.markrecap.ui.tagretag.sealcard.TagCountNoTagSection
import weddellseal.markrecap.ui.tagretag.sealcard.TagEventSection
import weddellseal.markrecap.ui.tagretag.sealcard.TagIdSection
import weddellseal.markrecap.ui.tagretag.sealcard.TissueCommentSection
import weddellseal.markrecap.ui.tagretag.sealcard.ValidationBanner

// ---- Small utility: tap anywhere in the row to clear focus ----
private fun Modifier.clearFocusOnTap(fm: FocusManager) = pointerInput(fm) {
    detectTapGestures(onTap = { fm.clearFocus() })
}

@Composable
fun SealCard(
    viewModel: TagRetagViewModel,
    homeViewModel: HomeViewModel,
    seal: Seal
) {
    val isEditMode by remember {
        viewModel.uiState.map { it.isEditMode }
    }.collectAsStateWithLifecycle(false)

    val isPrefilled by remember {
        viewModel.uiState.map { it.isPrefilled }
    }.collectAsStateWithLifecycle(false)

    val isSaveAttempted by remember {
        viewModel.uiState.map { it.isSaveAttempted }
    }.collectAsStateWithLifecycle(false)

    val tagFieldResetCounter by remember {
        viewModel.uiState.map { it.fieldResetCounter }
    }.collectAsStateWithLifecycle(0)

    val autoDetectedColony by homeViewModel.autoDetectedColony.collectAsState()

    val focusManager = LocalFocusManager.current

    // local values used to prevent a user from changing model values if the selection is invalid based on other field values
    var ageSelected by remember { mutableStateOf(seal.ageClass) }
    var sexSelected by remember { mutableStateOf(seal.sex) }
    var numRelsSelected by remember { mutableStateOf(seal.numRelatives) }

    val promptForDeleteRelatives = remember { mutableStateOf("") }
    val showDeleteRelativesDialog = remember { mutableStateOf(false) }

    LaunchedEffect(seal.ageClass, seal.sex, seal.numRelatives) {
        if (isPrefilled
            && ageSelected == SealAgeClass.UNKNOWN
            && sexSelected == SealSex.NONE
            && numRelsSelected == SealRelatives.UNKNOWN
        ) {
            // Set initial local state values for age, sex and numRels
            // only do this the first time the seal is prefilled, otherwise the local state values should reflect only the values the user selects
            // fix for prefill options not setting initial local state values,
            // which was resulting in age and sex being reset based on the local state which erased the prefill values
            ageSelected = seal.ageClass
            sexSelected = seal.sex
            numRelsSelected = seal.numRelatives
        }
    }

    // --------------- Orchestration ---------------

    // VALIDATION BANNER
    if (isSaveAttempted && seal.validationErrors.isNotEmpty()) {
        ValidationBanner(seal)
    }

    if (seal.colony == "White Island") {
        if (autoDetectedColony?.location != seal.colony) {
            // BANNER For White Island Seals that are observed outside of White Island colony
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFE0B2))
                    .padding(14.dp),
            ) {
                Text("This seal was last seen at White Island. Current colony detected by device is ${autoDetectedColony?.location}.")
                Text(
                    "Please take a photo of the tags and seal!",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }

    // AGE
    AgeSection(
        isEditMode = isEditMode,
        seal = seal,
        onSelectAge = { chosen ->
            ageSelected = chosen
            if (chosen == SealAgeClass.PUP || chosen == SealAgeClass.YEARLING) {
                if (seal.numRelatives.value > 0) {
                    numRelsSelected = SealRelatives.ZERO
                    promptForDeleteRelatives.value =
                        "You've selected a pup or yearling for Age, and neither can have relatives.\n"
                    showDeleteRelativesDialog.value = true
                } else {
                    viewModel.updateNumRelatives(SealRelatives.ZERO)
                    viewModel.updateAge(seal.sealType, chosen)
                }
            } else {
                viewModel.updateAge(seal.sealType, chosen)
            }
        },
        modifier = Modifier.clearFocusOnTap(focusManager)
    )

    // SEX
    SexSection(
        isEditMode = isEditMode,
        seal = seal,
        onSelectSex = { chosen ->
            sexSelected = chosen
            if (seal.sealType == SealType.PRIMARY && chosen == SealSex.MALE) {
                // primary seals that are male cannot have relatives
                if (seal.numRelatives.value > 0) {
                    // the number of relatives is being reduced
                    // pop a warning and ask for confirmation before moving forward
                    numRelsSelected = SealRelatives.ZERO
                    promptForDeleteRelatives.value =
                        "You've selected Male for Sex, which cannot have relatives.\n"
                    showDeleteRelativesDialog.value = true
                } else {
                    viewModel.updateNumRelatives(SealRelatives.ZERO) // explicitly set number of relatives to 0 when male is selected
                    viewModel.updateSex(seal.sealType, sexSelected)
                }
            } else {
                viewModel.updateSex(seal.sealType, sexSelected)
            }
        },
        modifier = Modifier.clearFocusOnTap(focusManager)
    )

    // NUMBER OF RELATIVES, CONFIRM DELETE RELATIVES DIALOG, & PUP PEED
    RelativesSection(
        isEditMode = isEditMode,
        seal = seal,
        onSelectRelatives = { chosen ->
            numRelsSelected = chosen

            val isReducingRelatives =
                numRelsSelected.value < seal.numRelatives.value
                        && seal.numRelatives != SealRelatives.ZERO // no need to pop a dialog when zero is deselected by the user

            when {
                // handle case where the number of relatives is being reduced
                // pop a warning and ask for confirmation before moving forward
                isReducingRelatives -> {
                    // Reducing count from known value → needs confirmation
                    promptForDeleteRelatives.value =
                        "It looks like you're trying to remove a relative.\n"
                    showDeleteRelativesDialog.value = true
                }

                else -> {
                    // Valid increase or same count → apply immediately
                    viewModel.updateNumRelatives(numRelsSelected)
                }
            }
        },
        onSelectPupPeed = {
            focusManager.clearFocus()
            viewModel.updatePupPeed(seal.sealType, it)
        },
        modifier = Modifier.clearFocusOnTap(focusManager)
    )

    // TAG ID FIELDS
    // tag id row - label, field & alpha buttons
    // old tag id row - label & field
    // reason for retag row - label & dropdown
    // None of the tag fields should show if the No Tag checkbox has been selected
    if (!seal.isNoTag) {

        // TAG ID / NEW TAG ID
        TagIdSection(
            label = if (seal.tagEventType == TagEventType.RETAG) "New\nTag ID" else "Tag ID",
            number = seal.tagNumber,
            alpha = seal.tagAlpha,
            onClear = {
                viewModel.clearTagID(seal.sealType)

                // when the event type is Marked or New and this field has been cleared
                // clear the seal in the WedCheck model when this field is cleared to clear the Seal SpeNo
                if (seal.tagEventType != TagEventType.RETAG) {
                    viewModel.removeWedCheckMatch(seal.sealType)
                }
            },
            onNumberChanged = { viewModel.updatePendingTagNumber(seal.sealType, it) },
            onNumberCommitted = { viewModel.updateTagNumber(seal.sealType, it) },
            onAlphaSelected = { viewModel.updateTagAlpha(seal.sealType, it) },
            modifier = Modifier.clearFocusOnTap(focusManager),
            fieldKey = "${tagFieldResetCounter}-${seal.sealType}-tag",
        )

        // OLD TAG ID
        if (seal.tagEventType == TagEventType.RETAG) {
            TagIdSection(
                label = "Old\nTag ID",
                number = seal.oldTagNumber,
                alpha = seal.oldTagAlpha,
                onClear = {
                    viewModel.clearOldTag(seal.sealType)
                    viewModel.removeWedCheckMatch(seal.sealType)
                },
                onNumberChanged = { viewModel.updatePendingOldTagNumber(seal.sealType, it) },
                onNumberCommitted = {
                    viewModel.updateOldTagNumber(
                        seal.sealType,
                        it.uppercase().trim()
                    )
                },
                onAlphaSelected = { viewModel.updateOldTagAlpha(seal.sealType, it) },
                modifier = Modifier.clearFocusOnTap(focusManager),
                fieldKey = "${tagFieldResetCounter}-${seal.sealType}-old-tag",
            )
        }
    }

    // NUMBER OF TAGS, NO TAG
    TagCountNoTagSection(
        isNoTag = seal.isNoTag,
        numTags = seal.numTags,
        onNumTagsSelected = { viewModel.updateNumTags(seal.sealType, it) },
        onToggleNoTag = { checked ->
            viewModel.updateNoTag(seal.sealType, checked)
            viewModel.updateTagEventType(
                seal,
                if (checked) TagEventType.MARKED else TagEventType.UNKNOWN
            )
            viewModel.clearTagID(seal.sealType); viewModel.clearOldTag(seal.sealType); viewModel.clearNumTags(
            seal.sealType
        )
            viewModel.removeWedCheckMatch(seal.sealType)
        },
        showOldTagMarks = (seal.tagEventType == TagEventType.NEW),
        oldTagMarks = seal.oldTagMarks,
        onToggleOldTagMarks = { viewModel.updateOldTagMarks(seal.sealType, it) },
        modifier = Modifier.clearFocusOnTap(focusManager)
    )

    // TAG EVENT TYPE
    TagEventSection(
        isNoTag = seal.isNoTag,
        seal = seal,
        onSelectTagEvent = {
            if (seal.tagEventType == TagEventType.RETAG && it != TagEventType.RETAG) {
                // toggling back from Retag to New or Marked
                // do this before updating the event type
                viewModel.onRetagDeselection(
                    seal.sealType,
                    seal.oldTagNumber,
                    seal.oldTagAlpha
                )
            }

            viewModel.updateTagEventType(seal, it)

            if (it == TagEventType.RETAG) {
                viewModel.onRetagSelection(
                    seal.sealType,
                    seal.tagNumber,
                    seal.tagAlpha
                )

                if (seal.wedCheckMatch != null && seal.wedCheckMatch.tagIdOne != seal.oldTagNumber + seal.oldTagAlpha) {
                    // Ensure that the WedCheck match is removed
                    // if the old tag ID does not match the WedCheck tag ID
                    viewModel.removeWedCheckMatch(seal.sealType)
                }
            }
        },
        modifier = Modifier.clearFocusOnTap(focusManager)
    )

    // REASON FOR RETAG
    if (seal.tagEventType == TagEventType.RETAG) {
        RetagReasonSection(
            seal = seal,
            onSelectReason = { viewModel.updateRetagReason(seal.sealType, it) },
            modifier = Modifier.clearFocusOnTap(focusManager)
        )
    }

    // CONDITION
    ConditionSection(
        seal = seal,
        onSelectCondition = { viewModel.updateCondition(seal.sealType, it) },
        modifier = Modifier.clearFocusOnTap(focusManager)
    )

    // TISSUE && COMMENT
    TissueCommentSection(
        seal = seal,
        onSelectTissue = {
            focusManager.clearFocus()
            viewModel.updateTissueTaken(seal.sealType, it)
        },
        onClearComment = {
            viewModel.updateComment(seal.sealType, "")
        },
        onCommentChanged = { viewModel.updatePendingComment(seal.sealType, it) },
        onCommentCommitted = {
            viewModel.updateCommentIfCurrent(seal.sealType, it, tagFieldResetCounter)
        },
        modifier = Modifier.clearFocusOnTap(focusManager),
        fieldKey = "${tagFieldResetCounter}-${seal.sealType}-comment",
    )

    // WEIGHT FOR PUPS ONLY
    if (seal.ageClass == SealAgeClass.PUP) {
        PupWeightSection(
            seal = seal,
            onEnterWeight = {
                focusManager.clearFocus()
                viewModel.updateIsWeightTaken(seal.sealType, it)
            },
            onWeightCleared = {
                viewModel.updateWeight(seal.sealType, 0)
            },
            onWeightCommitted = {
                viewModel.updateWeight(seal.sealType, it)
            },
            modifier = Modifier.clearFocusOnTap(focusManager)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // CONFIRM DELETE RELATIVES DIALOG
    // ask the user for confirmation of deletion of relatives
    // this situation arises when the number of relatives changes
    // because of age selection, sex selection or number of relatives selection
    if (showDeleteRelativesDialog.value) {

        val prompt = when {
            seal.numRelatives == SealRelatives.ONE ->
                "This will remove data for Pup One, ${
                    viewModel.getPupOneNotebookString().trim()
                }?"

            else ->
                if (numRelsSelected == SealRelatives.ONE) {
                    // removing pup two
                    "This will remove data for Pup Two, ${
                        viewModel.getPupTwoNotebookString().trim()
                    }?"
                } else {
                    "This will remove data for Pup One, ${
                        viewModel.getPupOneNotebookString().trim()
                    }, " +
                            "and Pup Two, ${viewModel.getPupTwoNotebookString().trim()}?"
                }
        }

        RemoveDialog(
            onDismissRequest = {
                promptForDeleteRelatives.value = ""
                showDeleteRelativesDialog.value = false
                // reset the local values to the model values
                sexSelected = seal.sex
                ageSelected = seal.ageClass
                numRelsSelected = seal.numRelatives
            },
            onConfirmation = {
                promptForDeleteRelatives.value = ""
                showDeleteRelativesDialog.value = false

                if (seal.ageClass != ageSelected) {
                    viewModel.updateAge(
                        seal.sealType,
                        ageSelected
                    ) // use the value selected by the user to update the model value
                }

                if (seal.sex != sexSelected) {
                    viewModel.updateSex(
                        seal.sealType,
                        sexSelected
                    ) // use the value selected by the user to update the model value
                }

                if (seal.numRelatives != numRelsSelected) {
                    viewModel.updateNumRelatives(numRelsSelected) // use the value selected by the user to update the model value
                }

                // reset the local values to the model values
                sexSelected = seal.sex
                ageSelected = seal.ageClass
                numRelsSelected = seal.numRelatives
            },
            text = promptForDeleteRelatives.value + prompt,
            buttonText = "Yes, clear pup data."
        )
    }
}
