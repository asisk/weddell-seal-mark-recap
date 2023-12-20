package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ChipInputForm(initialChips: List<String>) {
    val selectedChip = remember { mutableStateOf<String?>(null) }
    var chipText by remember { mutableStateOf("") }
    val chips = remember { mutableStateListOf<String>()}
    chips.addAll(initialChips)
     // Input field for adding new chips
    TextField(
        value = chipText,
        onValueChange = { chipText = it },
        label = { Text("Add Chip") },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (selectedChip.value != null) {
                    chipText = selectedChip.value!!
                }
//                val enteredText = chipText.trim()
//                if (enteredText.isNotEmpty()) {
//                    if (enteredText != selectedChip.value) {
//                        // Add the entered text as a new chip
//                        chips.add(enteredText)
//                    }
//                    chipText = ""
//                }
            }
        )
    )

    // Display the selected chip using InputChipExample
    selectedChip.value?.let { selected ->
        chipText = selected
    }


//    Spacer(modifier = Modifier.height(16.dp))

    // Display the selected chips using InputChipExample
    Row {
        chips.forEach { chip ->
            SuggestionChip(
                text = chip,
                onDismiss = {}
           )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
