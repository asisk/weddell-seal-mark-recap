package weddellseal.markrecap.ui.tagretag

import android.util.Log
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.frameworks.room.Seal
import weddellseal.markrecap.models.TagRetagModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.DropdownField

@Composable
fun SealCard(
    viewModel: TagRetagModel,
    seal: Seal,
    wedCheckViewModel: WedCheckViewModel,
) {
    val focusManager = LocalFocusManager.current

    var sealSpeNo by remember { mutableStateOf(if (seal.speNo != 0) seal.speNo.toString() else "") }
    var numRelatives by remember { mutableStateOf(seal.numRelatives) }
    var possibleRelatives by remember { mutableStateOf(seal.numRelatives) }
    var isRetag by remember { mutableStateOf(seal.tagEventType == "Retag") }
    var tagNumber by remember { mutableStateOf(seal.tagNumber) }
    var oldTagID by remember { mutableStateOf(seal.oldTagId) }
    var notebookStr by remember { mutableStateOf(seal.notebookDataString) }
    var isWeightToggled by remember { mutableStateOf(seal.weightTaken) }
    var isNoTagsChecked by remember { mutableStateOf(seal.numTags.toIntOrNull() == 0) }
    var isTissueChecked by remember { mutableStateOf(seal.tissueTaken) }
    var isOldTagMarksChecked by remember { mutableStateOf(seal.oldTagMarks) }
    val showDeleteRelativesDialog = remember { mutableStateOf(false) }
    val showWedCheckCommentsDialog = remember { mutableStateOf(false) }

    LaunchedEffect(seal.numRelatives) {
        numRelatives = if (seal.sex == "Male" && seal.name == "primary") {
            "0"
        } else {
            seal.numRelatives
        }
    }

    LaunchedEffect(seal.tagNumber) {
        tagNumber = seal.tagNumber
    }

    LaunchedEffect(seal.isNoTag) {
        isNoTagsChecked = seal.isNoTag
    }

    LaunchedEffect(seal.oldTagId) {
        oldTagID = seal.oldTagId
    }

    LaunchedEffect(seal.tissueTaken) {
        isTissueChecked = seal.tissueTaken
    }

    LaunchedEffect(seal.oldTagMarks) {
        isOldTagMarksChecked = seal.oldTagMarks
    }

    LaunchedEffect(seal.weightTaken) {
        isWeightToggled = seal.weightTaken
    }

    LaunchedEffect(seal.oldTagId) {
        Log.d("LaunchedEffect", "change in oldTagId detected")
        if (seal.oldTagId != "" && isRetag) {
            // check for an existing wedcheck seal record for this seal
            // conduct search if we haven't already located a wedcheck record
            val wedCheckSeal = viewModel.getWedCheckSeal(seal.oldTagId)
            if (wedCheckSeal == null) {
                // clear the speno & hasSpeno fields from our seal
                viewModel.clearSpeNo(seal)

                // find the seal
                if (!wedCheckViewModel.uiState.value.isSearching) {
                    Log.d("LaunchedEffect", "looking up seal")
                    wedCheckViewModel.findSealbyTagID(seal.oldTagId)
                }
            }
        }
    }

    LaunchedEffect(seal.tagNumber, seal.tagAlpha) {
        Log.d("LaunchedEffect", "change in either tagNumber or tagAlpha detected")
        // update the speNo if we don't have one once we have a tag number and a tag alpha
        if (!isRetag && seal.tagNumber != "" && seal.tagAlpha != "") {
            if (seal.tagNumber.length == 3 || seal.tagNumber.length == 4) {

                // construct a string without two alpha characters
                // to allow comparison with WedCheck record
                // and searching for a WedCheck record
                val searchStr = seal.tagNumber + seal.tagAlpha

                // check for an existing wedcheck seal record for this seal
                // conduct search if we haven't already located a wedcheck record
                val wedCheckSeal = viewModel.getWedCheckSeal(searchStr)
                if (wedCheckSeal == null) {
                    // clear the speno & hasSpeno fields from our seal
                    viewModel.clearSpeNo(seal)

                    // find the seal
                    if (!wedCheckViewModel.uiState.value.isSearching) {
                        Log.d("LaunchedEffect", "looking up seal")
                        wedCheckViewModel.findSealbyTagID(searchStr)
                    }
                }
            }
        }
    }

    // there's a lag when finding the seal in the wedcheck model
    // this LaunchedEffect allows us to be aware the presence of a new wedCheckSeal
    LaunchedEffect(wedCheckViewModel.wedCheckSeal.speNo) {
        if (wedCheckViewModel.wedCheckSeal.speNo != 0) { // check that the wedcheck seal was found
            var tagID = seal.tagNumber + seal.tagAlpha
            if (isRetag) { // if retag set the tag id to the old tag
                tagID = seal.oldTagId
            }

            // only assign wedcheck fields if they are for this seal
            if (wedCheckViewModel.wedCheckSeal.tagIdOne == tagID) {

                //map the relevant wedcheck fields to our seal
                if (seal.speNo != wedCheckViewModel.wedCheckSeal.speNo) {
                    viewModel.mapSpeno(seal.name, wedCheckViewModel.wedCheckSeal)
                    viewModel.addWedCheckSeal(tagID, wedCheckViewModel.wedCheckSeal)
                }

                // make sure that the comments are displayed when a seal record is updated to
                // allow the technician to take action if needed (TAKE PHOTOS)
                showWedCheckCommentsDialog.value = true
            }
        }
    }

    LaunchedEffect(seal.notebookDataString) {
        notebookStr = seal.notebookDataString
    }

    LaunchedEffect(seal.speNo) {
        sealSpeNo = if (seal.speNo != 0) {
            seal.speNo.toString()
        } else {
            ""
        }
    }

    LaunchedEffect(seal.tagEventType) {
        isRetag = seal.tagEventType == "Retag"
    }

// SEAL CARD HEADING/NOTEBOOK STRING & SPENO
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // NOTEBOOK STRING
            Box() {
                Row() {
                    Text(
                        notebookStr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            //SPENO
            Box() {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (sealSpeNo != "") {
                        Text(
                            "Speno:",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            sealSpeNo,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(.9f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(.8f)
            ) {

                Text(
                    "Age",
                    style = MaterialTheme.typography.titleLarge
                )

                if (seal.name != "primary") { // when the primary seal is a pup or yearling, there can be no other relatives
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
                            if (seal.age == "Pup" && seal.age != it) { // sex has been changed from pup to adult or yearling, clear pup fields
                                viewModel.resetPupFields(seal.name)
                            }

                            if (it == "Pup" || it == "Yearling") { // if the primary seal is a pup or a yearling, there are no relatives
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
                    val focusManager = LocalFocusManager.current

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

                if (seal.name == "primary" && seal.isObservationLogEntry) {
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
            )
        }

        // CONDITION
        val conditions =
            listOf(
                "None",
                "Dead - 0",
                "Poor - 1",
                "Fair - 2",
                "Good - 3",
                "Newborn - 4"
            )

        Box(
            modifier = Modifier
                .weight(.5f)
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

                DropdownField(conditions, seal.condition) { newText ->
                    viewModel.updateCondition(
                        seal.name,
                        newText
                    )
                }
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

                if (isNoTagsChecked) { // when the user has selected noTag, force them to unselect to change the event type
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
                            when (it) {
                                "Retag" -> {
                                    isRetag = true
                                }

                                "Marked" -> {
                                    isRetag = false
                                }

                                "New" -> {
                                    isRetag = false
                                }
                            }
                            viewModel.updateTagEventType(seal, it)
                        }
                    )
                }
            }
        }
    }

// TAG FIELDS (old tag id row - label & field, reason for retag row - label & dropdown, tag id row - label, field & alpha buttons)
    if (!isNoTagsChecked) { // None of the tag fields should show if the No Tag checkbox has been selected

        // OLD TAG ID
        if (isRetag) {
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

                        TagIDOutlinedTextField(
                            value = oldTagID,
                            labelText = "Old Tag ID",
                            placeholderText = "Enter Old Tag ID",
                            errorMessage = "",
                            keyboardType = KeyboardType.Text,
                            onValueChangeDo = {
                                // do nothing
                            },
                            onClearValueDo = {
                                viewModel.clearOldTag(seal.name)
                            },
                            onFocusChange = { isFocused, lastValue ->
                                if (!isFocused) {
                                    Log.d("Old Tag Row", "calling model update")
                                    // save the input to the model
                                    viewModel.updateOldTag(seal, lastValue.uppercase().trim())
                                }
                            }
                        )
                    }
                }
            }
        }

        // REASON FOR RETAG
        if (isRetag) {
            val retagOptions =
                listOf(
                    "None",
                    "1 of 4",
                    "2 of 4",
                    "3 of 4",
                    "Worn",
                    "Broken",
                    "Other"
                )

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
                        DropdownField(retagOptions, seal.reasonForRetag) { newText ->
                            viewModel.updateRetagReason(
                                seal.name,
                                newText
                            )
                        }
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
                    val errMessage = "3 or 4 digits"
                    val fieldLabel = if (isRetag) "New\nTag ID" else "Tag ID"

                    Text(
                        fieldLabel,
                        style = MaterialTheme.typography.titleLarge
                    )

                    // TAG ID
                    TagIDOutlinedTextField(
                        value = tagNumber,
                        labelText = "Number",
                        placeholderText = "Enter Tag Number",
                        errorMessage = errMessage,
                        keyboardType = KeyboardType.Number,
                        onValueChangeDo = {
                            // do nothing
                        },
                        onClearValueDo = {
                            // clear the tag value when this field has been cleared
                            viewModel.clearTag(seal)

                            // when the event type is Marked or New and this field has been cleared
                            // clear the current Seal SpeNo
                            // clear the seal in the WedCheck model when this field is cleared
                            if (!isRetag) {
                                viewModel.clearSpeNo(seal)
                                wedCheckViewModel.resetState()
                            }
                        },
                        onFocusChange = { isFocused, lastValue ->
                            if (!isFocused) {
                                Log.d("TagID Row", "executing on Focus")
                                // save the input to the model
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
                    ) { newText -> viewModel.updateTagAlpha(seal, newText) }
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

                if (!isNoTagsChecked) {

                    Text(
                        "# of Tags",
                        style = MaterialTheme.typography.titleLarge
                    )

                    SegmentedButtonGroup(
                        options = numTagsList,
                        selectedOption = seal.numTags,
                        onOptionSelected = { newVal ->
                            viewModel.updateNumTags(
                                seal.name,
                                newVal
                            )
                        }
                    )
                }

                Text(
                    text = "No Tag",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )

                Checkbox(
                    checked = isNoTagsChecked,
                    onCheckedChange = {
                        focusManager.clearFocus()

                        if (it) { // per 9/4 meeting, event type should be Marked when NoTag is checked
                            viewModel.updateTagEventType(
                                seal,
                                "Marked"
                            )
                        } else { // reset the event type if the checkbox is deselected
                            viewModel.updateTagEventType(
                                seal,
                                ""
                            )
                        }

                        isNoTagsChecked = it

                        viewModel.updateNoTag(seal.name)
                    },
                    modifier = Modifier
                        .padding(8.dp)
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
                    checked = isTissueChecked,
                    onCheckedChange = {
                        focusManager.clearFocus()
                        isTissueChecked = it
                        viewModel.updateTissueTaken(seal.name, it)
                    },
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }
    }

    // COMMENT && OLD TAG MARKS

    // WEDCHECK COMMENTS DIALOG
    if (showWedCheckCommentsDialog.value && wedCheckViewModel.wedCheckSeal.comment != "") {
        WedCheckCommentDialog(
            wedCheckRecordComments = wedCheckViewModel.wedCheckSeal.comment,
            onDismiss = { showWedCheckCommentsDialog.value = false },
        )
    }

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
                        checked = isOldTagMarksChecked,
                        onCheckedChange = {
                            focusManager.clearFocus()

                            isOldTagMarksChecked = it
                            viewModel.updateOldTagMarks(seal.name, it)
                        },
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }
            }
        }

        // COMMENT
        Box(
            modifier = Modifier
                .weight(.6f)
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
                        checked = isWeightToggled,
                        onCheckedChange = { isChecked ->
                            isWeightToggled = isChecked
                            viewModel.updateIsWeightTaken(seal.name, isChecked)
                        }
                    )
                }
            }

            // PUP WEIGHT ENTRY FIELD
            val weightRecorded by remember { mutableStateOf(seal.weight.toString()) }

            Box(
                modifier = Modifier
                    .weight(.6f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isWeightToggled) {
                        Spacer(modifier = Modifier.width(8.dp))

                        PupWeightOutlinedTextField(
                            value = weightRecorded,
                            labelText = "Weight",
                            placeholderText = "Enter in lbs",
                            onValueChangeDo = {
                                val number: Int? = it.toIntOrNull()
                                if (number != null) {
                                    viewModel.updateWeight(seal, number)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}