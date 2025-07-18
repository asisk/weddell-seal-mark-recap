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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.ui.tagretag.dialogs.RemoveDialog

@Composable
fun SealCard(
    viewModel: TagRetagModel,
    seal: Seal
) {
    val uiState by viewModel.uiState.collectAsState()

    val focusManager = LocalFocusManager.current

    // local UI flag
    val showDeleteRelativesDialog = remember { mutableStateOf(false) }

    var numRelatives by remember { mutableStateOf(seal.numRelatives) }
    var possibleRelatives by remember { mutableStateOf(seal.numRelatives) }

    LaunchedEffect(seal.numRelatives) {
        numRelatives = if (seal.sex == "Male" && seal.name == "primary") {
            "0"
        } else {
            seal.numRelatives
        }
    }

    // VALIDATION ERROR BANNER
    if (uiState.isSaveAttempted && seal.validationErrors.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer, // Light red background
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            seal.validationErrors.forEach { error ->
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }

//AGE
    val buttonListAge = listOf("Adult", "Pup", "Yearling")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
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
                // when the primary seal is a pup or yearling, there can be no other relatives
                if (seal.name != "primary") {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Pup",
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    SegmentedButtonGroup(
                        options = buttonListAge,
                        selectedOption = seal.age,
                        onOptionSelected = {
                            if (seal.age == "Pup" && seal.age != it) {
                                // sex has been changed from pup to adult or yearling, clear pup fields
                                viewModel.resetPupFields(seal.name)
                            }

                            if (it == "Pup" || it == "Yearling") {
                                // if the primary seal is a pup or a yearling, there are no relatives
                                if (numRelatives != "" && numRelatives != "0") {
                                    possibleRelatives = "0"

                                    // handle the case where the number of relatives is reduced
                                    // pop a warning and ask for confirmation before moving forward
                                    showDeleteRelativesDialog.value = true

                                } else {
                                    viewModel.updateNumRelatives("0")
                                }
                            }
                            viewModel.updateAge(seal, it)
                        }
                    )
                }
            }
        }
    }

// SEX & PUP PEED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // SEX
        val buttonListSex = listOf("Female", "Male", "Unknown")

        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Sex",
                    style = MaterialTheme.typography.titleLarge
                )

                SegmentedButtonGroup(
                    options = buttonListSex,
                    selectedOption = seal.sex,
                    onOptionSelected = {
                        if (seal.name == "primary" && it == "Male") { //primary seals that are male do not have relatives
                            if (numRelatives != "" && numRelatives != "0") {
                                possibleRelatives = "0"
                                // handle the case where the number of relatives is being reduced
                                // pop a warning and ask for confirmation before moving forward
                                showDeleteRelativesDialog.value = true

                            } else {
                                viewModel.updateNumRelatives("0")
                            }
                        }
                        viewModel.updateSex(seal, it)
                    }
                )
            }
        }

        // PUP PEED CHECKBOX
        Box(
            modifier = Modifier
                .weight(.3f)
                .padding(start = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                // display pup peed only for pups
                if (seal.age == "Pup") {
                    var isPupPeedChecked by remember {
                        mutableStateOf(seal.pupPeed)
                    }

                    Text(
                        text = "Pup" + "\n" + "Peed",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Checkbox(
                        checked = isPupPeedChecked,
                        onCheckedChange = {
                            focusManager.clearFocus()

                            isPupPeedChecked = it
                            viewModel.updatePupPeed(seal.name, it)
                        },
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }
            }
        }
    }

// NUMBER OF RELATIVES, CONFIRM DELETE RELATIVES DIALOG, && CONDITION
    val numRelsList = listOf("0", "1", "2")

    Row(
        modifier = Modifier
            .padding(10.dp)
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
                .weight(.7f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "# of Rels",
                    style = MaterialTheme.typography.titleLarge
                )

                if (seal.name == "primary" && seal.isTagRetagEntry) {
                    // when the primary seal has been populated from a observation log entry record
                    Text(
                        numRelatives,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (seal.age == "Pup" || seal.age == "Yearling") {
                    // when the primary seal is a pup or yearling, there can be no other relatives
                    Text(
                        numRelatives,
                        style = MaterialTheme.typography.titleLarge
                    )

                } else if (seal.name == "primary" && seal.sex == "Male" && numRelatives == "0") {
                    // when the primary seal is a male, there can be no other relatives
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        numRelatives,
                        style = MaterialTheme.typography.titleLarge
                    )

                } else {
                    SegmentedButtonGroup(
                        options = numRelsList,
                        selectedOption = numRelatives,
                        onOptionSelected = {
                            possibleRelatives = it  //value to be used if delete confirmed

                            if (it == "") {
                                if (seal.numRelatives != "0") { // no need to pop a dialog when zero is deselected by the user
                                    // handle case where the number of relatives is being reduced
                                    // pop a warning and ask for confirmation before moving forward
                                    showDeleteRelativesDialog.value = true
                                } else {
                                    numRelatives = it
                                }

                            } else if (seal.numRelatives != "" && it.toInt() < seal.numRelatives.toInt()) {
                                // handle case where the number of relatives is being reduced
                                // pop a warning and ask for confirmation before moving forward
                                showDeleteRelativesDialog.value = true

                            } else {
                                viewModel.updateNumRelatives(it)
                            }
                        }
                    )
                }
            }
        }

        // CONFIRM DELETE RELATIVES DIALOG
        // ask the user for confirmation of deletion of relatives
        // this situation arises when the number of relatives changes
        if (showDeleteRelativesDialog.value) {
            RemoveDialog(
                onDismissRequest = { showDeleteRelativesDialog.value = false },
                onConfirmation = {
                    showDeleteRelativesDialog.value = false
                    if (seal.sex == "Male") {
                        possibleRelatives = "0"  // set the value to zero
                    }
                    viewModel.updateNumRelatives(possibleRelatives) // use the value selected by the user to update the model value
                },
                text = "This will remove data you've entered for pups. Are you sure?",
                buttonText = "Yes, clear pup data."
            )
        }

        // CONDITION
        Box(
            modifier = Modifier
                .weight(.6f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Condition",
                    style = MaterialTheme.typography.titleLarge
                )
                SealConditionDropdown(
                    selected = seal.condition,
                    onSelected = {
                        viewModel.updateCondition(seal.name, it)
                    }
                )
            }
        }
    }

// TAG EVENT TYPE
//TODO, consider an enum for this an other strings
    val tagEventList = listOf("Marked", "New", "Retag")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
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
                .weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Tag Event",
                    style = MaterialTheme.typography.titleLarge
                )

                if (seal.isNoTag) {
                    // the database record needs to have an event type of marked for Retag
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Marked",
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    SegmentedButtonGroup(
                        options = tagEventList,
                        selectedOption = seal.tagEventType,
                        onOptionSelected = {
                            viewModel.updateTagEventType(seal, it)
                            if (it == "Retag") {
                                viewModel.onRetagSelection(seal)
                            }
                        }
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
        if (seal.tagEventType == "Retag") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
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
                            "Old Tag",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // OLD TAG ID
                        TagIDOutlinedTextField(
                            value = seal.oldTagNumber,
                            labelText = "Old Tag ID",
                            placeholderText = "3 or 4 Digit Tag Number",
                            errorMessage = "",
                            keyboardType = KeyboardType.Text,
                            onClearValueDo = {
                                viewModel.clearOldTag(seal.name)
                            },
                            onFocusChange = { isFocused, lastValue ->
                                if (!isFocused) {
                                    // save the input to the model
                                    viewModel.updateOldTagNumber(seal, lastValue.uppercase().trim())
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
                            viewModel.updateOldTagAlpha(seal, newText)
                        }
                    }
                }
            }
        }

        // REASON FOR RETAG
        if (seal.tagEventType == "Retag") {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(.4f)
                    ) {
                        Text(
                            "Reason for Retag",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(.5f)
                    ) {
                        RetagReasonDropDown(
                            selected = seal.reasonForRetag,
                            onSelected = {
                                viewModel.updateRetagReason(seal.name, it)
                                viewModel.onRetagSelection(seal)
                            }
                        )

                    }
                }
            }
        }

        //TAG ID
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
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
//                    val errMessage = "3 or 4 digits"
                    val fieldLabel = if (seal.tagEventType == "Retag") "New\nTag ID" else "Tag ID"

                    Text(
                        fieldLabel,
                        style = MaterialTheme.typography.titleLarge
                    )

                    // TAG ID
                    TagIDOutlinedTextField(
                        value = seal.tagNumber,
                        labelText = "3 or 4 Digit Tag Number",
                        placeholderText = "Enter Tag Number",
                        errorMessage = "",
                        keyboardType = KeyboardType.Number,
                        onClearValueDo = {
                            viewModel.clearTagID(seal)

                            // when the event type is Marked or New and this field has been cleared
                            // clear the seal in the WedCheck model when this field is cleared to clear the Seal SpeNo
                            if (seal.tagEventType != "Retag") {
                                viewModel.removeWedCheckMatch(seal)
                            }
                        },
                        onFocusChange = { isFocused, lastValue ->
                            if (!isFocused) {
                                Log.d("TagID Row", "Updating tag on Focus not active")

                                viewModel.updateTagNumber(seal, lastValue)
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
                        viewModel.updateTagAlpha(seal, newText)
                    }
                }
            }
        }
    }

// NUMBER OF TAGS, NO TAG && TISSUE
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

        // NUMBER OF TAGS
        val numTagsList = listOf("1", "2")
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                if (!seal.isNoTag) {

                    Text(
                        "# of Tags",
                        style = MaterialTheme.typography.titleLarge
                    )

                    SegmentedButtonGroup(
                        options = numTagsList,
                        selectedOption = seal.numTags,
                        onOptionSelected = { newVal ->
                            viewModel.updateNumTags(seal.name, newVal)
                        }
                    )
                }
                // NO TAG
                Text(
                    text = "No Tag",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )

                Checkbox(
                    modifier = Modifier.padding(8.dp),
                    checked = seal.isNoTag,
                    onCheckedChange = {
                        focusManager.clearFocus()
                        viewModel.updateNoTag(seal.name, it)

                        if (it) {
                            // per 9/4 meeting, event type should be Marked when NoTag is checked
                            viewModel.updateTagEventType(seal, "Marked")
                        } else {
                            // reset the event type if the checkbox is deselected
                            viewModel.updateTagEventType(seal, "")
                        }

                        // when NoTag marked, clear the tag fields & speno
                        viewModel.clearTagID(seal)
                        viewModel.clearOldTag(seal.name)
                        viewModel.clearNumTags(seal.name)
                        viewModel.removeWedCheckMatch(seal)
                    },
                )
            }
        }

        // TISSUE
        Box(
            modifier = Modifier
                .weight(.4f)
                .padding(start = 8.dp, end = 4.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tissue",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )

                Checkbox(
                    modifier = Modifier.padding(8.dp),
                    checked = seal.tissueTaken,
                    onCheckedChange = {
                        focusManager.clearFocus()
                        viewModel.updateTissueTaken(seal.name, it)
                    }
                )
            }
        }
    }

// OLD TAG MARKS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // OLD TAG MARKS, FOR NEW TAG EVENT ONLY
        if (seal.tagEventType == "New") {

            Box(
                modifier = Modifier
                    .weight(.3f)
                    .padding(start = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Old Tag Marks",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Checkbox(
                        modifier = Modifier.padding(8.dp),
                        checked = seal.oldTagMarks,
                        onCheckedChange = {
                            focusManager.clearFocus()
                            viewModel.updateOldTagMarks(seal.name, it)
                        },
                    )
                }
            }
        }

        // COMMENT
        Box(
            modifier = Modifier.weight(.6f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CommentField(seal.comment) { newText ->
                    viewModel.updateComment(seal.name, newText)
                }
            }
        }
    }

    // WEIGHT FOR PUPS ONLY
    if (seal.age == "Pup") {
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
                            viewModel.updateIsWeightTaken(seal.name, isChecked)
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
                                    viewModel.updateWeight(seal, number)
                                }
                            },
                            onClearValueDo = {
                                viewModel.updateWeight(seal, 0)
                            }
                        )
                    }
                }
            }
        }
    }
}