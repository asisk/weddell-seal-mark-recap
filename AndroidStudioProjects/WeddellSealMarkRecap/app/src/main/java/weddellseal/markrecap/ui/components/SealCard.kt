package weddellseal.markrecap.ui.components

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.WedCheckViewModel

@Composable
fun SealCard(
    viewModel: AddObservationLogViewModel,
    seal: Seal,
    wedCheckViewModel: WedCheckViewModel,
) {
    var sealSpeNo by remember { mutableStateOf(seal.speNo.toString()) }
    var numRelatives by remember { mutableStateOf(seal.numRelatives) }
    var possibleRelatives by remember { mutableStateOf(seal.numRelatives) }
    var isRetag by remember { mutableStateOf(seal.tagEventType == "Retag") }
    var tagIDVal by remember { mutableStateOf(seal.tagIdOne) }
    var oldTagIDOneVal by remember { mutableStateOf(seal.oldTagIdOne) }
    var oldTagIDTwoVal by remember { mutableStateOf(seal.oldTagIdTwo) }
    var notebookStr by remember { mutableStateOf(seal.notebookDataString) }
    var isWeightToggled by remember { mutableStateOf(seal.weightTaken) }
    val showDeleteRelativesDialog = remember { mutableStateOf(false) }

    LaunchedEffect(seal.numRelatives) {
        numRelatives = if (seal.sex == "Male" && seal.name == "primary") {
            "0"
        } else {
            seal.numRelatives
        }
    }

    LaunchedEffect(seal.tagIdOne) {
        tagIDVal = seal.tagIdOne
    }

    LaunchedEffect(seal.tagOneAlpha) {
        // update the speNo if we don't have one once we have a tag number and a tag alpha
        if (!wedCheckViewModel.uiState.value.isSearching) {
            if (seal.tagIdOne != "" && seal.tagOneAlpha != "") {
                if (seal.tagOneNumber.length == 3 || seal.tagOneNumber.length == 4) {

                    // construct a string without two alpha characters
                    // to allow comparison with WedCheck record
                    // and searching for a WedCheck record
                    val searchStr = seal.tagOneNumber + seal.tagOneAlpha

                    if (seal.speNo == 0 || searchStr != wedCheckViewModel.wedCheckSeal.tagIdOne) {
                        viewModel.clearSpeNo(seal)
                        wedCheckViewModel.resetState()
                        wedCheckViewModel.findSealbyTagID(searchStr)

                        delay(1500) // Wait for 1500 milliseconds

                        if (wedCheckViewModel.wedCheckSeal.speNo != 0) {
                            viewModel.mapWedCheckFields(seal.name, wedCheckViewModel.wedCheckSeal)
                            oldTagIDOneVal = wedCheckViewModel.wedCheckSeal.tagIdOne
                            oldTagIDTwoVal = wedCheckViewModel.wedCheckSeal.tagIdTwo
                        }
                    }
                }
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
        if (seal.tagEventType == "Retag") {
            isRetag = true
        }
    }

    // SEAL CARD HEADING/NOTEBOOK STRING & SPENO
    Column() {
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
            .padding(10.dp),
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

                SegmentedButtonGroup(
                    options = buttonListAge,
                    selectedOption = seal.age,
                    onOptionSelected = {
                        if (seal.age == "Pup" && seal.age != it) { // sex has been changed from pup to adult or yearling, clear pup fields
                            viewModel.resetPupFields(seal.name)
                        }

                        if (it == "Pup") { // if the primary seal is a pup, there are no relatives
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

    // SEX & PUP PEED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
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
            .padding(10.dp),
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

                if (seal.age == "Pup") { // when the primary seal is a pup, there can be no other relatives
                    Text(
                        numRelatives,
                        style = MaterialTheme.typography.titleLarge
                    )

                } else if (seal.name == "primary" && seal.sex == "Male" && numRelatives == "0") { // when the primary seal is a male, there can be no other relatives
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

    // OLD TAG ID
    if (isRetag || seal.isWedCheck) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
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
                        "Old Tag ID One",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(text = oldTagIDOneVal)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "Old Tag ID Two",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(text = oldTagIDTwoVal)
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
            .padding(10.dp),
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

                SegmentedButtonGroup(
                    options = tagEventList,
                    selectedOption = seal.tagEventType,
                    onOptionSelected = {
                        when (it) {
                            "Retag" -> {
                                isRetag = true
                                tagIDVal = ""
                                viewModel.clearTagOne(seal)
                                viewModel.clearTagTwo(seal)
                            }

                            "Marked" -> {
                                if (isRetag && seal.speNo != 0) {
                                    tagIDVal = wedCheckViewModel.uiState.value.tagIdForSpeNo
//                                    viewModel.revertTagID(seal.name, tagIDVal)
                                }
                                isRetag = false
                            }

                            "New" -> {
//                                tagIDVal = ""
//                                viewModel.clearTagOne(seal)
//                                viewModel.clearTagTwo(seal)
//                                viewModel.clearOldTags(seal.name)
//                                viewModel.clearSpeNo(seal)
//                                wedCheckViewModel.resetState()
                                isRetag = false
                            }
                        }
                        viewModel.updateTagEventType(seal, it)
                    }
                )
            }
        }
    }

    //TAG ID
    if (!isRetag && !seal.isWedCheck) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
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
                        "Tag ID",
                        style = MaterialTheme.typography.titleLarge
                    )

                    // TAG ID
                    TagIDOutlinedTextField(
                        value = tagIDVal,
                        labelText = "Number",
                        placeholderText = "Enter Tag Number",
                        errorMessage = "Tag numbers should be 3 or 4 digits long.",
                        onValueChangeDo = {
                            // save the input to the model
                            viewModel.updateTagOneNumber(seal, it)
                        },
                        onClearValueDo = {
                            viewModel.clearTag(seal)
                            if (seal.tagEventType != "Retag") {
                                viewModel.clearSpeNo(seal)
                            }
                            wedCheckViewModel.resetState()
                        }
                    )
                }
            }

            //TAG ALPHA BUTTONS
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
                        seal.tagOneAlpha
                    ) { newText -> viewModel.updateTagOneAlpha(seal, newText) }
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
                .padding(10.dp),
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

        //TAG ID NEW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
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
                        "New \n" +
                                "Tag ID",
                        style = MaterialTheme.typography.titleLarge
                    )

                    TagIDOutlinedTextField(
                        value = tagIDVal,
                        labelText = "Number",
                        placeholderText = "Enter Tag Number",
                        errorMessage = "Tag numbers should be 3 or 4 digits long.",
                        onValueChangeDo = {
                            // save the input to the model
                            viewModel.updateTagOneNumber(seal, it)
                        },
                        onClearValueDo = {
                            viewModel.clearTag(seal)
                        }
                    )
                }
            }

            //TAG ID NEW ALPHA BUTTONS
            val buttonListAlpha = listOf("A", "C", "D")

            Box(
                modifier = Modifier
                    .weight(.4f)
                    .padding(start = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SingleSelectTagAlphaButtonGroup(
                        buttonListAlpha,
                        seal.tagOneAlpha
                    ) { newText ->
                        viewModel.updateTagOneAlpha(seal, newText)
                    }
                }
            }
        }
    }

    // NUMBER OF TAGS && TISSUE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // NUMBER OF TAGS
        var isNoTagsChecked by remember { mutableStateOf(seal.numTags.toIntOrNull() == 0) }
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

                Text(
                    "# of Tags",
                    style = MaterialTheme.typography.titleLarge
                )

                SegmentedButtonGroup(
                    options = numTagsList,
                    selectedOption = seal.numTags,
                    onOptionSelected = { newVal -> viewModel.updateNumTags(seal.name, newVal) }
                )

                Text(
                    text = "No Tag",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )

                val focusManager = LocalFocusManager.current
                Checkbox(
                    checked = isNoTagsChecked,
                    onCheckedChange = {
                        focusManager.clearFocus()
                        isNoTagsChecked = it
                        viewModel.updateNoTag(seal.name)
                    },
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }

        // TISSUE
        var isTissueChecked by remember {
            mutableStateOf(seal.tissueTaken)
        }

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

                val focusManager = LocalFocusManager.current
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // OLD TAG MARKS, FOR NEW TAG EVENT ONLY
        if (seal.tagEventType == "New") {
            var isOldTagMarksChecked by remember {
                mutableStateOf(seal.oldTagMarks)
            }

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

                    val focusManager = LocalFocusManager.current
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
                Spacer(modifier = Modifier.width(8.dp))

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