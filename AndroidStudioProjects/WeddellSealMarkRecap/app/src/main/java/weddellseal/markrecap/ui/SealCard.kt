package weddellseal.markrecap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.ui.entryfields.CommentField
import weddellseal.markrecap.ui.entryfields.DropdownField
import weddellseal.markrecap.ui.entryfields.NumberFieldValidateOnCharCount
import weddellseal.markrecap.ui.entryfields.SingleSelectButtonGroup
import weddellseal.markrecap.ui.entryfields.SingleSelectTagAlphaButtonGroup

@Composable
fun SealCard(viewModel: AddObservationLogViewModel, seal: AddObservationLogViewModel.Seal) {
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

        // ADULT NOTEBOOK DISPLAY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                seal.age + " Seal Details",
                style = MaterialTheme.typography.titleLarge
            )
        }
        //AGE
        Row(
            modifier = Modifier
                .fillMaxWidth(.7f)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //AGE
            Text(text = "Age")
            val buttonListAge = listOf<String>("Adult", "Pup", "Yearling")
            SingleSelectButtonGroup(buttonListAge, seal.age) { newText -> viewModel.updateAge(seal, newText) }
        }

        // SEX
        Row(
            modifier = Modifier
                .fillMaxWidth(.7f)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Sex")
            val buttonListSex = listOf<String>("Female", "Male", "Unknown")
            SingleSelectButtonGroup(buttonListSex, seal.sex) { newText ->
                viewModel.updateSex(seal, newText)
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
            val numRelsFieldVal = if (seal.numRelatives > 0) { seal.numRelatives } else { "" }
            NumberFieldValidateOnCharCount(
                numRelsFieldVal.toString(),
                1,
                "# of Relatives",
                "Enter # of relatives present"
            ) { newVal -> viewModel.updateNumRelatives(seal, newVal) }

            // CONDITION
            Spacer(modifier = Modifier.width(30.dp))
            Text(text = "Condition")
            Spacer(modifier = Modifier.width(10.dp))
            val conditions = listOf<String>("Dead", "Poor", "Fair", "Good", "Newborn")
            DropdownField(conditions) { newText -> viewModel.updateCondition(seal.name, newText) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NUMBER OF TAGS
            val numTagsFieldVal = if (seal.numTags > 0) { seal.numTags } else { "" }
            NumberFieldValidateOnCharCount(
                numTagsFieldVal.toString(),
                1,
                "# of Tags",
                "Enter # of tags present"
            ) { newVal -> viewModel.updateNumTags(seal.name, newVal) }

            // TISSUE SAMPLED
            val (checkedState, onStateChange) = remember { mutableStateOf(seal.tissueTaken) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checkedState,
                        onValueChange = {
                            onStateChange(!checkedState)
                            viewModel.updateTissueTaken(seal.name, checkedState)
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
                    checked = checkedState,
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
            val tagIdFieldVal = (if (seal.tagNumber > 0) { seal.tagNumber } else { "" }).toString()
            TextField(
                value = tagIdFieldVal,
                onValueChange = {
                    val number: Int? = it.toIntOrNull()
                    if (number != null) {
                        viewModel.updateTagNumber(seal, number)
                    }
                },
                label = { Text("TagNumber") },
                placeholder = { Text("Enter Tag Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Handle "Done" button action
                        keyboardController?.hide()
                    }
                ),
                trailingIcon = {
                    Icon(
                        Icons.Filled.Clear, contentDescription = "Clear text",
                        Modifier.clickable { viewModel.clearTag(seal) })
                }
            )

            //TAG ALPHA BUTTONS
            val buttonListAlpha = listOf<String>("A", "C", "D")
            SingleSelectTagAlphaButtonGroup(buttonListAlpha, seal.tagAlpha) { newText -> viewModel.updateTagAlpha(seal, newText) }
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
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Comments")
            Spacer(modifier = Modifier.width(10.dp))
            CommentField(seal.comment) { newText ->
                viewModel.updateComment(seal.name, newText)
            }
        }
    }
}