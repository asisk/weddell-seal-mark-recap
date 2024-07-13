package weddellseal.markrecap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.models.AddObservationLogViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SealCard(
    viewModel: AddObservationLogViewModel,
    seal: Seal,
) {
    val scrollState = rememberScrollState()
    var newNumRelatives by remember { mutableStateOf(seal.numRelatives.toString()) }

    // Synchronize newNumRelatives with the ViewModel whenever it changes
    LaunchedEffect(seal.numRelatives) {
        newNumRelatives = seal.numRelatives.toString()
    }

    Column() {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(2f)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Sex")
                val buttonListSex = listOf("Female", "Male", "Unknown")
                SingleSelectButtonGroup(buttonListSex, seal.sex) { newText ->
                    viewModel.updateSex(seal, newText)
                }
            }
        }
        // NUMBER OF RELATIVES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NUMBER OF RELATIVES
            Text(text = "# of Relatives")
            val numRelsList = listOf("0", "1", "2")
            SingleSelectButtonGroup(
                txtOptions = numRelsList,
                valueInModel = newNumRelatives
            ) { newVal ->
                newNumRelatives = newVal
                // case where the number of relatives is being reset
                if (newVal.toInt() == 0 && seal.numRelatives > 0) {
                    // TODO, pop a warning and ask for confirmation before moving forward
                    // because this action results in removing any entered data for pups
                } else {
                    // TODO, consider firing this if the user confirms the action
                    viewModel.updateNumRelatives(seal, newVal)
                }
            }

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
                    isError =
                        it.isEmpty() || !it.matches(Regex("\\d{4}")) // Ensure the input is exactly 4 digits
                },
                label = { Text("TagNumber") },
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
            //TODO, consider an enum for this an other strings
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
            CommentField(seal.comment) { newText ->
                viewModel.updateComment(seal.name, newText)
            }
        }
    }
//    }
}