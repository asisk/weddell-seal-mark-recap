package weddellseal.markrecap.ui.tagretag

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.ui.tagretag.dialogs.RemoveDialog

@Composable
fun SealCard(
    viewModel: TagRetagModel,
    seal: Seal
) {
    val uiState by viewModel.uiState.collectAsState()
//    val isEditMode by remember {
//        viewModel.uiState.map { it.isEditMode }
//    }.collectAsStateWithLifecycle(false)
//
//    val isPrefilled by remember {
//        viewModel.uiState.map { it.isPrefilled }
//    }.collectAsStateWithLifecycle(false)
//
//    val isSaveAttempted by remember {
//        viewModel.uiState.map { it.isSaveAttempted }
//    }.collectAsStateWithLifecycle(false)

    val focusManager = LocalFocusManager.current

    // local values used to prevent a user from changing model values if the selection is invalid based on other field values
    var ageSelected by remember { mutableStateOf(seal.ageClass) }
    var sexSelected by remember { mutableStateOf(seal.sex) }
    var numRelsSelected by remember { mutableStateOf(seal.numRelatives) }

    val promptForDeleteRelatives = remember { mutableStateOf("") }
    val showDeleteRelativesDialog = remember { mutableStateOf(false) }

    LaunchedEffect(seal.ageClass, seal.sex, seal.numRelatives) {
        if (uiState.isPrefilled
            && ageSelected == SealAgeClass.UNKNOWN
            && sexSelected == SealSex.NONE
            && numRelsSelected == SealRelatives.UNKNOWN
        ) {
            // only do this the first time the seal is prefilled, otherwise the local state values should reflect only the values the user selects
            // fix for prefill options not setting initial local state values,
            // which was resulting in age and sex being reset based on the local state which erased the prefill values
            Log.d(
                "TagRetagModel",
                "prefilled seal, setting initial local state values for age, sex and numRels"
            )
            ageSelected = seal.ageClass
            sexSelected = seal.sex
            numRelsSelected = seal.numRelatives
        }
    }

    // VALIDATION ERROR BANNER
    if (uiState.isSaveAttempted && seal.validationErrors.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    color = Color(0xFFE30707), //  red background
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            seal.validationErrors.forEach { error ->
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = error,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }

    // AGE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Age",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.width(10.dp))

                if (uiState.isEditMode && seal.sealType == SealType.PRIMARY && seal.hasPup) {
                    // age is not selectable in edit mode for the primary seal
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        seal.ageClass.description,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    // when the primary seal is a pup or yearling, there can be no other relatives
                    if (seal.sealType != SealType.PRIMARY) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            SealAgeClass.PUP.description,
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        val buttonListAge = SealAgeClass.values()
                            .filter { it != SealAgeClass.UNKNOWN }
                            .map { it.description }

                        SegmentedButtonGroup(
                            options = buttonListAge,
                            selectedOption = seal.ageClass.description,
                            onOptionSelected = {
                                ageSelected = SealAgeClass.fromSelection(it)

                                if (ageSelected == SealAgeClass.PUP || ageSelected == SealAgeClass.YEARLING) {
                                    // if the primary seal is a pup or a yearling, there should be no relatives
                                    if (seal.numRelatives.value > 0) {
                                        // handle the case where the number of relatives is being reduced
                                        // because the age selected does not support having pups
                                        numRelsSelected = SealRelatives.ZERO
                                        promptForDeleteRelatives.value =
                                            "You've selected a pup or yearling for Age, and neither can have relatives.\n"
                                        showDeleteRelativesDialog.value = true
                                    } else {
                                        viewModel.updateNumRelatives(SealRelatives.ZERO) // explicitly set number of relatives to 0 when pup or yearling is selected
                                        viewModel.updateAge(seal.sealType, ageSelected)
                                    }
                                } else {
                                    viewModel.updateAge(seal.sealType, ageSelected)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // SEX
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Sex",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.width(10.dp))

                if (uiState.isEditMode && seal.sealType == SealType.PRIMARY && seal.hasPup) {
                    // sex is not selectable in edit mode for the primary seal
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        seal.sex.description,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    val buttonListSex = SealSex.values()
                        .filter { it != SealSex.NONE }
                        .map { it.description }

                    SegmentedButtonGroup(
                        options = buttonListSex,
                        selectedOption = seal.sex.description,
                        onOptionSelected = { selection ->
                            sexSelected = SealSex.fromSelection(selection)

                            if (seal.sealType == SealType.PRIMARY && selection == SealSex.MALE.description) {
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
                        }
                    )
                }
            }
        }
    }

    // NUMBER OF RELATIVES, CONFIRM DELETE RELATIVES DIALOG, & PUP PEED
    Row(
        modifier = Modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "# of Rels",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.width(10.dp))

                if (uiState.isEditMode && seal.hasPup) {
                    // number of relatives is not selectable in edit mode if the primary seal has a pup
                    // pups may be removed via the delete button
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        seal.numRelatives.label,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    if (seal.ageClass == SealAgeClass.PUP || seal.ageClass == SealAgeClass.YEARLING) {
                        // when the primary seal is a pup or yearling, there can be no other relatives
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            seal.numRelatives.label,
                            style = MaterialTheme.typography.titleLarge
                        )

                    } else if (seal.sealType == SealType.PRIMARY && seal.sex == SealSex.MALE && seal.numRelatives == SealRelatives.ZERO) {
                        // when the primary seal is a male, there can be no other relatives
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            seal.numRelatives.label,
                            style = MaterialTheme.typography.titleLarge
                        )

                    } else {
                        val numRelsList = SealRelatives.values()
                            .filter { it != SealRelatives.UNKNOWN }
                            .map { it.label }

                        SegmentedButtonGroup(
                            options = numRelsList,
                            selectedOption = seal.numRelatives.label,
                            onOptionSelected = {
                                numRelsSelected =
                                    SealRelatives.fromSelection(it)  //value to be used if delete confirmed

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
                            }
                        )
                    }
                }
            }
        }

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

        Spacer(modifier = Modifier.width(30.dp))

        // PUP PEED CHECKBOX
        Box(
            modifier = Modifier.weight(1f)  // take the remaining space
        ) {
            Column(
                Modifier
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // display pup peed only for pups
                if (seal.ageClass == SealAgeClass.PUP) {
                    Text(
                        text = "Pup Peed",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Checkbox(
                        checked = seal.pupPeed,
                        onCheckedChange = {
                            focusManager.clearFocus()
                            viewModel.updatePupPeed(seal.sealType, it)
                        },
                    )
                }
            }
        }
    }

    // TAG FIELDS
    // old tag id row - label & field
    // reason for retag row - label & dropdown
    // tag id row - label, field & alpha buttons
    // None of the tag fields should show if the No Tag checkbox has been selected
    if (!seal.isNoTag) {

        // OLD TAG ID
        if (seal.tagEventType == TagEventType.RETAG) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(.4f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Old\nTag ID",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // OLD TAG ID
                        TagIDOutlinedTextField(
                            value = seal.oldTagNumber,
                            labelText = "3 or 4 Digit Tag Number",
                            placeholderText = "Enter Tag Number",
                            errorMessage = "",
                            keyboardType = KeyboardType.Number,
                            onClearValueDo = {
                                viewModel.clearOldTag(seal.sealType)

                                // when the event type is Retag and this field has been cleared
                                // clear the seal in the WedCheck model when this field is cleared to clear the Seal SpeNo
                                if (seal.tagEventType == TagEventType.RETAG) {
                                    viewModel.removeWedCheckMatch(seal.sealType)
                                }
                            },
                            onFocusChange = { isFocused, lastValue ->
                                if (!isFocused) {
                                    // save the input to the model
                                    viewModel.updateOldTagNumber(
                                        seal.sealType,
                                        lastValue.uppercase().trim()
                                    )
                                }
                            }
                        )
                    }
                }

                // OLD TAG ID ALPHA BUTTONS
                val buttonListAlpha = listOf("A", "C", "D")

                Box(
                    modifier = Modifier
                        .weight(.4f)
                        .padding(start = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        SingleSelectTagAlphaButtonGroup(
                            buttonListAlpha,
                            seal.oldTagAlpha
                        ) { newText ->
                            viewModel.updateOldTagAlpha(seal.sealType, newText)
                        }
                    }
                }
            }
        }

        //TAG ID
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(.4f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fieldLabel =
                        if (seal.tagEventType == TagEventType.RETAG) "New\nTag ID" else "Tag ID"

                    Text(
                        fieldLabel,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // TAG ID
                    TagIDOutlinedTextField(
                        value = seal.tagNumber,
                        labelText = "3 or 4 Digit Tag Number",
                        placeholderText = "Enter Tag Number",
                        errorMessage = "",
                        keyboardType = KeyboardType.Number,
                        onClearValueDo = {
                            viewModel.clearTagID(seal.sealType)

                            // when the event type is Marked or New and this field has been cleared
                            // clear the seal in the WedCheck model when this field is cleared to clear the Seal SpeNo
                            if (seal.tagEventType != TagEventType.RETAG) {
                                viewModel.removeWedCheckMatch(seal.sealType)
                            }
                        },
                        onFocusChange = { isFocused, lastValue ->
                            if (!isFocused) {
                                Log.d("TagID Row", "Updating tag on Focus not active")

                                viewModel.updateTagNumber(seal.sealType, lastValue)
                            }
                        }
                    )
                }
            }

            //TAG ID ALPHA BUTTONS
            val buttonListAlpha = listOf("A", "C", "D")

            Box(
                modifier = Modifier
                    .weight(.4f)
                    .padding(start = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    SingleSelectTagAlphaButtonGroup(
                        buttonListAlpha,
                        seal.tagAlpha
                    ) { newText ->
                        viewModel.updateTagAlpha(seal.sealType, newText)
                    }
                }
            }
        }
    }

    // NUMBER OF TAGS, NO TAG
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // NUMBER OF TAGS
        Box {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!seal.isNoTag) {
                    Text(
                        "# of Tags",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val numTagsList = listOf("1", "2")
                    SegmentedButtonGroup(
                        options = numTagsList,
                        selectedOption = seal.numTags,
                        onOptionSelected = { newVal ->
                            viewModel.updateNumTags(seal.sealType, newVal)
                        }
                    )
                }
            }
        }

        Box {
            Column(
                Modifier.padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // NO TAG
                Text(
                    text = "No Tag",
                    style = MaterialTheme.typography.titleLarge,
                )

                Checkbox(
                    checked = seal.isNoTag,
                    onCheckedChange = {
                        focusManager.clearFocus()
                        viewModel.updateNoTag(seal.sealType, it)

                        if (it) {
                            // per 9/4 meeting, event type should be Marked when NoTag is checked
                            viewModel.updateTagEventType(seal, TagEventType.MARKED)
                        } else {
                            // reset the event type if the checkbox is deselected
                            viewModel.updateTagEventType(seal, TagEventType.UNKNOWN)
                        }

                        // when NoTag marked, clear the tag fields & speno
                        viewModel.clearTagID(seal.sealType)
                        viewModel.clearOldTag(seal.sealType)
                        viewModel.clearNumTags(seal.sealType)
                        viewModel.removeWedCheckMatch(seal.sealType)
                    },
                )
            }
        }

        // OLD TAG MARKS, FOR NEW TAG EVENT ONLY
        if (seal.tagEventType == TagEventType.NEW) {
            Box(
                modifier = Modifier.weight(1f)  // take the remaining space
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Old Tag Marks",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Checkbox(
                        checked = seal.oldTagMarks,
                        onCheckedChange = {
                            focusManager.clearFocus()
                            viewModel.updateOldTagMarks(seal.sealType, it)
                        },
                    )
                }
            }
        }
    }

    // TAG EVENT TYPE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Tag Event",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.width(10.dp))

                if (seal.isNoTag) {
                    // the database record needs to have an event type of marked for Retag
                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        TagEventType.MARKED.description,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {

                    val tagEventList = TagEventType.values()
                        .filter { it != TagEventType.UNKNOWN }
                        .map { it.description }

                    SegmentedButtonGroup(
                        options = tagEventList,
                        selectedOption = seal.tagEventType.description,
                        onOptionSelected = {
                            //TODO, consider moving all logic to new fun onEventTypeSelection in view model

                            if (seal.tagEventType == TagEventType.RETAG && it != TagEventType.RETAG.description) {
                                // toggling back from Retag to New or Marked
                                // do this before updating the event type
                                viewModel.onRetagDeselection(
                                    seal.sealType,
                                    seal.oldTagNumber,
                                    seal.oldTagAlpha
                                )
                            }

                            viewModel.updateTagEventType(seal, TagEventType.fromSelection(it))

                            if (it == TagEventType.RETAG.description) {
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
                        }
                    )
                }
            }
        }
    }

    // REASON FOR RETAG
    if (seal.tagEventType == TagEventType.RETAG) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Text(
                        "Reason for Retag",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    RetagReasonDropDown(
                        selected = seal.reasonForRetag,
                        onSelected = {
                            viewModel.updateRetagReason(seal.sealType, it)
                        }
                    )
                }
            }
        }
    }

    // CONDITION
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    "Condition",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.width(10.dp))

                ConditionSegmentedButtonGroup(
                    selectedOption = seal.condition.code,
                    onOptionSelected = { selection ->
                        val conditionSelected = SealCondition.fromCode(selection)
                        viewModel.updateCondition(seal.sealType, conditionSelected)
                    }
                )
            }
        }
    }

    // TISSUE && COMMENT
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 20.dp, top = 10.dp)

    ) {
        // TISSUE
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tissue",
                    style = MaterialTheme.typography.titleLarge,
                )

                Checkbox(
                    checked = seal.tissueTaken,
                    onCheckedChange = {
                        focusManager.clearFocus()
                        viewModel.updateTissueTaken(seal.sealType, it)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(30.dp))

        CommentField(
            value = seal.comment,
            onClearValueDo = {
                viewModel.updateComment(seal.sealType, "")
            },
            onFocusChange = { isFocused, lastValue ->
                if (!isFocused) {
                    // save the input to the model
                    viewModel.updateComment(seal.sealType, lastValue.trim())
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Comment field allows letters, numbers, and certain special characters.",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    // WEIGHT FOR PUPS ONLY
    if (seal.ageClass == SealAgeClass.PUP) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // PUP WEIGHT SWITCH
            Box(
                modifier = Modifier
                    .weight(.4f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Enter Weight",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Switch(
                        checked = seal.weightTaken,
                        onCheckedChange = { isChecked ->
                            viewModel.updateIsWeightTaken(seal.sealType, isChecked)
                        }
                    )
                }
            }

            // PUP WEIGHT ENTRY FIELD
            Box(
                modifier = Modifier
                    .weight(.6f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (seal.weightTaken) {
                        Spacer(modifier = Modifier.width(8.dp))

                        PupWeightOutlinedTextField(
                            value = seal.weight.toString(),
                            onFocusChange = {
                                val number: Int? = it.toIntOrNull()
                                if (number != null) {
                                    viewModel.updateWeight(seal.sealType, number)
                                }
                            },
                            onClearValueDo = {
                                viewModel.updateWeight(seal.sealType, 0)
                            }
                        )
                    }
                }
            }
        }
    }
}