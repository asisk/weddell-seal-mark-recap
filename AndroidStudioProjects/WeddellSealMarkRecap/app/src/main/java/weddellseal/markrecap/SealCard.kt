package weddellseal.markrecap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.entryfields.DropdownField
import weddellseal.markrecap.entryfields.TextFieldValidateOnCharCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealCard(viewModel: AddObservationLogViewModel) {
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
            Text(text = viewModel.uiState.age)
            val buttonList = listOf<String>("Adult", "Pup", "Yearling")
            SingleSelectButtonGroup(buttonList) { newText -> viewModel.updateAge(newText) }
            Spacer(modifier = Modifier.width(16.dp))
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
            Text(text = viewModel.uiState.sex)
            val buttonList = listOf<String>("Female", "Male", "Unknown")
            SingleSelectButtonGroup(buttonList) { newText ->
                viewModel.updateSex(newText)
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
            TextFieldValidateOnCharCount(
                1,
                "# of Relatives",
                "Enter # of relatives present",
                null
            ) { newVal -> viewModel.updateNumRelatives(newVal.toInt()) }

            // CONDITION
            Text(text = "Condition")
            val conditions = listOf<String>("Dead", "Poor", "Fair", "Good", "Newborn")
            DropdownField(conditions) { newText -> viewModel.updateCondition(newText) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NUMBER OF TAGS
            TextFieldValidateOnCharCount(
                1,
                "# of Tags",
                "Enter # of tags present",
                null
            ) { newVal -> viewModel.updateNumTags(newVal.toInt()) }
            // TISSUE SAMPLED
            val (checkedState, onStateChange) = remember { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checkedState,
                        onValueChange = { onStateChange(!checkedState) },
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
            NumberInputField(
                "Enter TagId",
                "TagId",
                viewModel.uiState.tagId
            ) { newText ->
                viewModel.updateTagIdNum(newText)
            }
            Button(
                onClick = { viewModel.appendAlphaToTagID("A") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow), // Change the background color
            ) { Text("A", color = Color.Black) }
            Button(
                onClick = { viewModel.appendAlphaToTagID("C") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) { Text("C", color = Color.Black) }
            Button(
                onClick = { viewModel.appendAlphaToTagID("D") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) { Text("D", color = Color.White) }
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
            Text(text = viewModel.uiState.tagEventType)
            val tagEventList = listOf<String>("Marked", "New", "Retag")
            SingleSelectButtonGroup(tagEventList) { newText ->
                viewModel.updateTagEventType(newText)
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
            CommentField("") { newText ->
                viewModel.updateComment(newText)
            }
        }
    }
}