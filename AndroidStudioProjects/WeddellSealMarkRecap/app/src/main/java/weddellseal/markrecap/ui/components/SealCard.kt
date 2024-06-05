package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.models.AddObservationLogViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SealCard(
    viewModel: AddObservationLogViewModel,
    seal: AddObservationLogViewModel.Seal,
    showDetails: Boolean
) {
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current

        // NOTEBOOK DISPLAY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            // SEAL CARD HEADING/NOTEBOOK STRING
            var headingStr = ""
            if (seal.age.isNotEmpty()) {
                headingStr = seal.age + " :  "
            }
            Text(
                headingStr + seal.notebookDataString,
                style = MaterialTheme.typography.titleLarge
            )
        }
        if (showDetails) {
            //AGE
            Row(
                modifier = Modifier
                    .fillMaxWidth(.7f)
                    .padding(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //AGE
                    Text(text = "Age")
                    val buttonListAge = listOf<String>("Adult", "Pup", "Yearling")
                    SingleSelectButtonGroup(buttonListAge, seal.age) { newText ->
                        viewModel.updateAge(
                            seal,
                            newText
                        )
                    }
                }
            }

            // SEX & PUP PEED
//            val (checkedStatePeed, onStateChangePeed) = remember { mutableStateOf(seal.tissueTaken) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .toggleable(
//                        value = checkedStatePeed,
//                        onValueChange = {
//                            onStateChangePeed(!checkedStatePeed)
//                            viewModel.updatePupPeed(seal.name, checkedStatePeed)
//                        },
//                        role = Role.Checkbox
//                    )
                    .padding(10.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sex")
                    val buttonListSex = listOf<String>("Female", "Male", "Unknown")
                    SingleSelectButtonGroup(buttonListSex, seal.sex) { newText ->
                        viewModel.updateSex(seal, newText)
                    }
                }
//                if (seal.age == "Pup") {
                Spacer(modifier = Modifier.width(10.dp))
                var isChecked by remember {
                    mutableStateOf(false)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pup Peed",
                        style = MaterialTheme.typography.bodyLarge,
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
//            }
            }
            // NUMBER OF RELATIVES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
//                val numRelsFieldVal = if (seal.numRelatives > 0) {
//                    seal.numRelatives
//                } else {
//                    ""
//                }
//                NumberFieldValidateOnCharCount(
//                    numRelsFieldVal.toString(),
//                    1,
//                    "# of Relatives",
//                    "Enter # of relatives present"
//                ) { newVal -> viewModel.updateNumRelatives(seal, newVal) }

                // NUMBER OF RELATIVES
                Text(text = "# of Relatives")
                val numTagsList = listOf<String>("0", "1", "2")
                SingleSelectButtonGroup(
                    numTagsList,
                    seal.numRelatives.toString()
                ) { newVal -> viewModel.updateNumRelatives(seal, newVal) }

                // CONDITION
                Spacer(modifier = Modifier.width(30.dp))
                Text(text = "Condition")
                Spacer(modifier = Modifier.width(10.dp))
                // TODO, address the option to clear a condition for an adult
                val conditions =
                    listOf<String>("0 - Dead", "1 - Poor", "2 - Fair", "3 - Good", "4 - Newborn")
                DropdownField(conditions) { newText ->
                    viewModel.updateCondition(
                        seal.name,
                        newText
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NUMBER OF TAGS
                Text(text = "# of Tags")
                val numTagsList = listOf<String>("0", "1", "2", "3", "4")
                SingleSelectButtonGroup(
                    numTagsList,
                    seal.numTags.toString()
                ) { newVal -> viewModel.updateNumTags(seal.name, newVal) }


//                val numTagsFieldVal = if (seal.numTags > 0) {
//                    seal.numTags
//                } else {
//                    ""
//                }
//                NumberFieldValidateOnCharCount(
//                    numTagsFieldVal.toString(),
//                    1,
//                    "# of Tags",
//                    "Enter # of tags present"
//                ) { newVal -> viewModel.updateNumTags(seal.name, newVal) }

                // TISSUE SAMPLED
                val (checkedStateTissue, onStateChangeTissue) = remember { mutableStateOf(seal.tissueTaken) }
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
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tissue Sampled",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Checkbox(
                        checked = checkedStateTissue,
                        onCheckedChange = null // null recommended for accessibility with screenreaders
                    )
                }
            }
            //TAG ID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val focusManager: FocusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                var isError by remember { mutableStateOf(false) }
                var tagIDVal by remember {
                    mutableStateOf("")
                }
                OutlinedTextField(
                    value = tagIDVal,
                    onValueChange = {
                        val number: Int? = it.toIntOrNull()
                        tagIDVal = it
                        isError = it.isEmpty() || !it.matches(Regex("\\d{4}")) // Ensure the input is exactly 4 digits
                    },
                    label = { Text("TagNumber") },
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
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
                                viewModel.updateTagNumber(seal, number)
                            }
                        },
                    ),
                    trailingIcon = {
                        Icon(
                            Icons.Filled.Clear, contentDescription = "Clear text",
                            Modifier.clickable {
                                tagIDVal = ""
                                viewModel.clearTag(seal)
                            })
                    }
                )
                if (isError) {
                    // change the outlined text box red and add text below indicating the error
                }

                //TAG ALPHA BUTTONS
                val buttonListAlpha = listOf<String>("A", "C", "D")
                SingleSelectTagAlphaButtonGroup(
                    buttonListAlpha,
                    seal.tagAlpha
                ) { newText -> viewModel.updateTagAlpha(seal, newText) }
            }
            // TAG EVENT TYPE
            val (checkedState, onStateChange) = remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth(.7f)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Event Type")
                val tagEventList = listOf<String>("Marked", "New", "Retag")
                SingleSelectButtonGroup(tagEventList, seal.tagEventType) { newText ->
                    viewModel.updateTagEventType(seal, newText)
                }
            }
            // COMMENTS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .fillMaxHeight()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
//                Text(text = "Comments")
//                Spacer(modifier = Modifier.width(10.dp))
                CommentField(seal.comment) { newText ->
                    viewModel.updateComment(seal.name, newText)
                }
            }
        }
    }
}

@Composable
fun DropdownExample() {
    val options = listOf<String>("A", "C", "D")

    var selectedOption by remember { mutableStateOf("Option 1") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Dropdown Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .clickable { expanded = true }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = selectedOption, color = Color.White)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(text = option) },
                    onClick = {
                        selectedOption = option
                        expanded = false
                    }
                )
            }
        }
    }
}