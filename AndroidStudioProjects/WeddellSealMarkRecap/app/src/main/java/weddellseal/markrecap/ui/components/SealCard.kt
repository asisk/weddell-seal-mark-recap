package weddellseal.markrecap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.models.AddObservationLogViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SealCard(
    viewModel: AddObservationLogViewModel,
    seal: Seal,
) {
    var newNumRelatives by remember { mutableStateOf(seal.numRelatives.toString()) }
    var isWeightToggled by remember { mutableStateOf(false) }
    var isCommentToggled by remember { mutableStateOf(false) }
    var isRetag by remember { mutableStateOf(false) }
    var tagIDVal by remember { mutableStateOf(seal.tagIdOne) }
    var tagIDValNew by remember { mutableStateOf(seal.tagIdOne) }
    var oldTagOne by remember { mutableStateOf(seal.oldTagIdOne) }
    var oldTagTwo by remember { mutableStateOf(seal.oldTagIdTwo) }
    var notebookStr by remember { mutableStateOf(seal.notebookDataString) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    // Synchronize newNumRelatives with the ViewModel whenever it changes
    LaunchedEffect(seal.numRelatives) {
        newNumRelatives = seal.numRelatives.toString()
    }

    LaunchedEffect(seal.tagIdOne) {
        tagIDVal = seal.tagIdOne
        tagIDValNew = seal.tagIdOne
    }

    LaunchedEffect(seal.notebookDataString) {
        notebookStr = seal.notebookDataString
    }

    Column() {
        // NOTEBOOK DISPLAY
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            // SEAL CARD HEADING/NOTEBOOK STRING
            var headingStr = ""
            if (seal.age.isNotEmpty()) {
                headingStr = seal.age + " :  "
            }
            Text(
                headingStr + notebookStr,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold
            )
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
                        viewModel.updateAge(
                            seal,
                            newText
                        )
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
//                    .padding(end = 8.dp)
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
                                mutableStateOf(false)
                            }
                            Text(
                                text = "Pup Peed",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
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
                            seal.numRelatives.toString(),
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        val numRelsList = listOf("0", "1", "2")
                        SingleSelectButtonGroupSquare(
                            numRelsList,
                            seal.numRelatives.toString()
                        ) { newVal ->
                            newNumRelatives = newVal

                            // handle case where the number of relatives is being set to zero
                            if (newVal.toInt() == 0 && seal.numRelatives > 0) {
                                // pop a warning and ask for confirmation before moving forward
                                showDeleteDialog.value = true
                            } else {
                                viewModel.updateNumRelatives(seal, newVal)
                            }
                        }
                    }
                }
            }

            // because this action results in removing any entered data for pups
            // Show the dialog if showDialog is true
            if (showDeleteDialog.value) {
                RemoveDialog(
                    onDismissRequest = { showDeleteDialog.value = false },
                    onConfirmation = {
                        viewModel.removePups()
                        showDeleteDialog.value = false
                    },
                )
            }

            Box(
                modifier = Modifier
                    .weight(.6f)
//                    .padding(end = 8.dp)
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

                        val focusManager: FocusManager = LocalFocusManager.current
                        var isError by remember { mutableStateOf(false) }

                        Text(
                            "Old Tag ID One",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = seal.oldTagIdOne)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Old Tag ID Two",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = seal.oldTagIdTwo)
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
                        viewModel.updateTagEventType(seal, newText)
                        if (newText == "Retag") {
                            isRetag = true

                            // shift the current tags to the old tags and reset the current tags to empty
                            viewModel.updateOldTags(seal)
                            viewModel.resetTags(seal)
                        }
                        // TODO, handle the case where if was Retag and now something else...revert the tags in the model
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

                        val focusManager: FocusManager = LocalFocusManager.current
                        var isError by remember { mutableStateOf(false) }

                        Text(
                            "Tag ID",
                            style = MaterialTheme.typography.titleLarge
                        )

                        // TAG ID & OLD TAG ID
                        OutlinedTextField(
                            value = tagIDVal,
                            onValueChange = {
//                            val number: Int? = it.toIntOrNull()
                                tagIDVal = it
                                isError =
                                    it.isEmpty() || !it.matches(Regex("\\d{4}")) // Ensure the input is exactly 4 digits
                            },
                            label = {
                                Text(
                                    "Number",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
                            isError = isError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = ContentAlpha.disabled
                                ),
                            ),
                            placeholder = { Text("Enter Tag Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Handle "Done" button action

                                    // change the focus
                                    focusManager.clearFocus()

                                    // save the input to the model
                                    val number: Int? = tagIDVal.toIntOrNull()
                                    if (number != null && !isError) {
                                        viewModel.updateTagOneNumber(seal, number)
                                    }
                                },
                            ),
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Clear, contentDescription = "Clear text",
                                    Modifier.clickable {
                                        tagIDVal = ""
                                        viewModel.clearTagOne(seal)
                                    })
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
                        Text(
                            "Tag Alpha",
                            style = MaterialTheme.typography.titleMedium
                        )

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

                        val focusManager: FocusManager = LocalFocusManager.current
                        var isError by remember { mutableStateOf(false) }

                        Text(
                            "New \n" +
                                    "Tag ID",
                            style = MaterialTheme.typography.titleLarge
                        )

                        // TAG ID NEW
                        OutlinedTextField(
                            value = tagIDValNew,
                            onValueChange = {
//                            val number: Int? = it.toIntOrNull()
                                tagIDValNew = it
                                isError =
                                    it.isEmpty() || !it.matches(Regex("\\d{4}")) // Ensure the input is exactly 4 digits
                            },
                            label = {
                                Text(
                                    "Number",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
                            isError = isError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = ContentAlpha.disabled
                                ),
                            ),
                            placeholder = { Text("Enter Tag Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Handle "Done" button action

                                    // change the focus
                                    focusManager.clearFocus()

                                    // save the input to the model
                                    val number: Int? = tagIDValNew.toIntOrNull()
                                    if (number != null && !isError) {
                                        viewModel.updateTagOneNumber(seal, number)
                                    }
                                },
                            ),
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Clear, contentDescription = "Clear text",
                                    Modifier.clickable {
                                        tagIDValNew = ""
                                        viewModel.clearTagOne(seal)
                                    })
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
                        Text(
                            "Tag Alpha",
                            style = MaterialTheme.typography.titleMedium
                        )

                        val buttonListAlpha = listOf("A", "C", "D")
                        SingleSelectTagAlphaButtonGroup(
                            buttonListAlpha,
                            seal.tagOneAlpha
                        ) { newText -> viewModel.updateTagOneAlpha(seal, newText) }
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
                    Text(
                        "# of Tags",
                        style = MaterialTheme.typography.titleLarge
                    )
                    val numTagsList = listOf("1", "2")
                    SingleSelectButtonGroupSquare(
                        numTagsList,
                        seal.numTags
                    ) { newVal -> viewModel.updateNumTags(seal.name, newVal) }

                    Spacer(modifier = Modifier.width(8.dp))

                    TagSwitch(seal.numTags) {
                        // actions on change of value
                        viewModel.resetTags(seal)
                        viewModel.updateNumTags(seal.name, "NoTag")
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(.4f)
                    .padding(start = 8.dp, end = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // TISSUE
                    val (checkedStateTissue, onStateChangeTissue) = remember {
                        mutableStateOf(
                            seal.tissueTaken
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checkedStateTissue,
                                onValueChange = {
                                    onStateChangeTissue(!checkedStateTissue)
                                    viewModel.updateTissueTaken(seal.name, checkedStateTissue)
                                },
                                role = Role.Checkbox
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tissue",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Checkbox(
                            checked = checkedStateTissue,
                            onCheckedChange = null // null recommended for accessibility with screenreaders
                        )
                    }
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
                    .weight(.4f)
//                    .padding(end = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter Comment",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = isCommentToggled,
                        onCheckedChange = { isChecked ->
                            isCommentToggled = isChecked
                        }
                    )
                }
            }

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
                    if (isCommentToggled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CommentField(seal.comment) { newText ->
                            viewModel.updateComment(seal.name, newText)
                        }
                    }
                }
            }
        }
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
                        val focusManager: FocusManager = LocalFocusManager.current
                        var weightRecorded by remember { mutableStateOf("") }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = weightRecorded,
                            onValueChange = {
                                weightRecorded = it
                            },
                            label = {
                                Text(
                                    "Weight",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
                            colors = OutlinedTextFieldDefaults.colors(
                                MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = ContentAlpha.disabled
                                ),
                            ),
                            placeholder = { Text("Enter Weight in lbs") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Handle "Done" button action

                                    // change the focus
                                    focusManager.clearFocus()

                                    // save the input to the model
                                    val number: Int? = weightRecorded.toIntOrNull()
                                    if (number != null) {
                                        viewModel.updateWeight(seal, number)
                                    }
                                },
                            ),
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Clear, contentDescription = "Clear text",
                                    Modifier.clickable {
                                        weightRecorded = ""
//                                        viewModel.clearTagOne(seal)
                                    })
                            }
                        )
                    }
                }
            }
        }
    }
}