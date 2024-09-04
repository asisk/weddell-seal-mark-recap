package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    // State to hold the currently selected option
    var currentSelection by remember { mutableStateOf(selectedOption) }

    LaunchedEffect(selectedOption) {
        currentSelection = selectedOption
    }

    // Row to hold all the segmented buttons
    Row(
        modifier = Modifier
            .padding(4.dp)
//            .fillMaxWidth()
            .background(color = Color.DarkGray, shape = RoundedCornerShape(50)) // Background color for the whole segmented group
            .padding(2.dp) // Padding between the background and the buttons
    ) {
        options.forEach { option ->
            val isSelected = option == currentSelection

            // Individual Button for each option
            Button(
                onClick = {
                    // Toggle selection: deselect if already selected, select otherwise
                    if (isSelected) {
                        currentSelection = ""
                        onOptionSelected("") // Deselect the button
                    } else {
//                        currentSelection = option // this could be the cause of a delay, but commented out to allow the logic for male numrelatives
                        onOptionSelected(option) // Select the button
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.Black else Color.White, // Change color based on selection
                    contentColor = if (isSelected) Color.White else Color.Black
                ),
                shape = when (option) {
                    // Adjust shapes to make buttons rounded at the ends
                    options.first() -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                    options.last() -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                modifier = Modifier
//                    .weight(.5f)
                    .padding(horizontal = 2.dp) // padding between the buttons
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(text = option, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}