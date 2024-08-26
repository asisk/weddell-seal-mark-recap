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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SealCard(
    viewModel: AddObservationLogViewModel,
    seal: Seal,
    wedCheckViewModel: WedCheckViewModel,
) {
    var numRelatives by remember { mutableStateOf(seal.numRelatives.toString()) }
    var isWeightToggled by remember { mutableStateOf(seal.weightTaken) }
    var isRetag by remember { mutableStateOf(seal.tagEventType == "Retag") }
    var sealSpeNo by remember { mutableStateOf(seal.speNo.toString()) }
    var tagIDVal by remember { mutableStateOf(seal.tagIdOne) }
    var oldTagIDOneVal by remember { mutableStateOf(seal.oldTagIdOne) }
    var oldTagIDTwoVal by remember { mutableStateOf(seal.oldTagIdTwo) }
    var notebookStr by remember { mutableStateOf(seal.notebookDataString) }
    val showDeleteRelativesDialog = remember { mutableStateOf(false) }

    LaunchedEffect(seal.numRelatives) {
        numRelatives = seal.numRelatives.toString()
    }

    LaunchedEffect(seal.tagIdOne) {
        tagIDVal = seal.tagIdOne

        // update the speNo if we don't have one
        // and the tagID entry is complete
        if (seal.speNo == 0 && seal.tagIdOne != "" && seal.tagOneAlpha != "" && !wedCheckViewModel.uiState.value.isSearching) {
            if (seal.tagOneNumber.toString().length == 3 || seal.tagOneNumber.toString().length == 4) {
                wedCheckViewModel.findSealbyTagID(seal.tagIdOne)

                delay(1500) // Wait for 1500 milliseconds

                if (wedCheckViewModel.wedCheckSeal.speNo != 0) {
                    viewModel.mapWedCheckFields(seal.name, wedCheckViewModel.wedCheckSeal)
                    oldTagIDOneVal = wedCheckViewModel.wedCheckSeal.tagIdOne
                    oldTagIDTwoVal = wedCheckViewModel.wedCheckSeal.tagIdTwo
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

    Column() {
        // NOTEBOOK DISPLAY
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            // SEAL CARD HEADING/NOTEBOOK STRING & SPENO
            Box(
                modifier = Modifier
                    .weight(.9f)
//                    .padding(end = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(.8f)
                ) {
                    Text(
                        notebookStr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (sealSpeNo.toIntOrNull() != 0) {
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
//                    .padding(end = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(.8f)
            ) {
                //AGE
                Text(
                    "Age",
                    style = MaterialTheme.typography.titleLarge
                )
                val buttonListAge = listOf<String>("Adult", "Pup", "Yearling")
                SingleSelectButtonGroup(buttonListAge, seal.age) { newText ->
                    //handle the case
                    if (seal.age == "Pup" && seal.age != newText) {
                        viewModel.resetPupFields(seal.name)
                    }
                    viewModel.updateAge(seal, newText)
                }
            }
        }
    }
    // SEX & PUP PEED
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Sex",
                    style = MaterialTheme.typography.titleLarge
                )
                val buttonListSex = listOf("Female", "Male", "Unknown")
                SingleSelectButtonGroup(buttonListSex, seal.sex) { newText ->
                    // change in sex to Male should result in removing any entered pups
                    if (seal.name == "primary" && seal.numRelatives > 0 && newText == "Male") {
                        numRelatives = "0"
                        // pop a warning and ask for confirmation before moving forward
                        showDeleteRelativesDialog.value = true
                    }
                    viewModel.updateSex(seal, newText)
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(.4f)
                .padding(start = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .weight(2f)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // display pup peed only for pups
                    if (seal.age == "Pup") {
                        var isChecked by remember {
                            mutableStateOf(seal.pupPeed)
                        }
                        val focusManager = LocalFocusManager.current

                        Text(
                            text = "Pup Peed",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                focusManager.clearFocus()

                                isChecked = it
                                viewModel.updatePupPeed(seal.name, it)
                            },
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    // NUMBER OF RELATIVES && CONDITION
    Row(
        modifier = Modifier
//                .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(.8f)
//                    .padding(end = 8.dp)
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
                if (seal.age == "Pup") {
                    Text(
                        numRelatives,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    val numRelsList = listOf("0", "1", "2")
                    SingleSelectButtonGroupSquare(
                        numRelsList,
                        numRelatives
                    ) { newVal ->
                        // handle case where the number of relatives is being reduced
                        if (newVal.toInt() < seal.numRelatives) {
                            numRelatives = newVal
                            // pop a warning and ask for confirmation before moving forward
                            showDeleteRelativesDialog.value = true
                        } else {
                            viewModel.updateNumRelatives(seal, newVal)
                        }
                    }
                }
            }
        }

        // because this action results in removing any entered data
        // Show the dialog if showDialog is true
        if (showDeleteRelativesDialog.value) {
            RemoveDialog(
                onDismissRequest = { showDeleteRelativesDialog.value = false },
                onConfirmation = {
                    viewModel.updateNumRelatives(seal, numRelatives)
                    showDeleteRelativesDialog.value = false
                },
            )
        }

        Box(
            modifier = Modifier
                .weight(.6f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                // CONDITION
                Text(
                    "Condition",
                    style = MaterialTheme.typography.titleLarge
                )
                val conditions =
                    listOf(
                        "None",
                        "Dead - 0",
                        "Poor - 1",
                        "Fair - 2",
                        "Good - 3",
                        "Newborn - 4"
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
//                    .padding(end = 8.dp)
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
                //TODO, consider an enum for this an other strings
                val tagEventList = listOf("Marked", "New", "Retag")
                SingleSelectButtonGroup(tagEventList, seal.tagEventType) { newText ->
                    when (newText) {
                        "Retag" -> {
                            isRetag = true
                            tagIDVal = ""
                            viewModel.clearTagOne(seal)
                            viewModel.clearTagTwo(seal)
                        }

                        "Marked" -> {
                            if (isRetag && seal.speNo != 0) {
                                tagIDVal = wedCheckViewModel.uiState.value.tagIdForSpeNo
                                viewModel.revertTagID(seal.name, tagIDVal)
                            }
                            isRetag = false
                        }

                        "New" -> {
                            tagIDVal = ""
                            viewModel.clearTagOne(seal)
                            viewModel.clearTagTwo(seal)
                            viewModel.clearOldTags(seal.name)
                            viewModel.clearSpeNo(seal)
                            isRetag = false
                        }
                    }

                    viewModel.updateTagEventType(seal, newText)
                }
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
                            val number: Int? = it.toIntOrNull()
                            if (number != null) {
                                viewModel.updateTagOneNumber(seal, number)
                            }
                        },
                        onClearValueDo = {
                            viewModel.clearTag(seal)
                            if (seal.tagEventType != "Retag") {
                                viewModel.clearSpeNo(seal)
                            }
                        }
                    )
                }
            }
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
                    //TAG ALPHA BUTTONS
                    val buttonListAlpha = listOf("A", "C", "D")
                    SingleSelectTagAlphaButtonGroup(
                        buttonListAlpha,
                        seal.tagOneAlpha
                    ) { newText -> viewModel.updateTagOneAlpha(seal, newText) }
                }
            }
        }
    }

    if (isRetag) {
        // REASON FOR RETAG
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

                    // TAG ID NEW
                    TagIDOutlinedTextField(
                        value = tagIDVal,
                        labelText = "Number",
                        placeholderText = "Enter Tag Number",
                        errorMessage = "Tag numbers should be 3 or 4 digits long.",
                        onValueChangeDo = {
                            // save the input to the model
                            val number: Int? = it.toIntOrNull()
                            if (number != null) {
                                viewModel.updateTagOneNumber(seal, number)
                            }
                        },
                        onClearValueDo = {
                            viewModel.clearTag(seal)
                        }
                    )
                }
            }
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
                    //TAG ALPHA BUTTONS
                    val buttonListAlpha = listOf("A", "C", "D")
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
                // NUMBER OF TAGS
                var isNoTagsChecked by remember { mutableStateOf(seal.numTags.toIntOrNull() == 0) }
                val focusManager = LocalFocusManager.current
                val numTagsList = listOf("1", "2")

                Text(
                    "# of Tags",
                    style = MaterialTheme.typography.titleLarge
                )
                SingleSelectButtonGroupSquare(
                    numTagsList,
                    seal.numTags
                ) { newVal -> viewModel.updateNumTags(seal.name, newVal) }

                Text(
                    text = "No Tag",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
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
                var isTissueChecked by remember {
                    mutableStateOf(seal.tissueTaken)
                }
                val focusManager = LocalFocusManager.current

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
    // COMMENTS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .weight(.6f)
//                    .padding(end = 8.dp)
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
    if (seal.age == "Pup") {

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
//                    .padding(end = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (seal.age == "Pup") {
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isWeightToggled) {
                        Spacer(modifier = Modifier.width(8.dp))

                        var weightRecorded by remember { mutableStateOf(seal.weight.toString()) }

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