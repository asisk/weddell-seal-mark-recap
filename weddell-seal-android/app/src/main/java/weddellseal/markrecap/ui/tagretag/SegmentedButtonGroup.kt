package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import weddellseal.markrecap.R
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .background(
                color = Color.DarkGray,
                shape = RoundedCornerShape(50)
            ) // Background color for the whole segmented group
            .padding(4.dp) // Padding between the background and the buttons
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption

            // Individual Button for each option
            Button(
                onClick = {
                    focusManager.clearFocus() // Clear focus when button is clicked

                    // Toggle selection: deselect if already selected, select otherwise
                    if (isSelected) {
                        onOptionSelected("") // Deselect the button
                    } else {
                        onOptionSelected(option) // Select the button
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.Black else Color.White,
                    contentColor = if (isSelected) Color.White else Color.Black
                ),
                shape = when (option) {
                    // Adjust shapes to make buttons rounded at the ends
                    options.first() -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                    options.last() -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                modifier = Modifier.padding(horizontal = 2.dp) // padding between the buttons
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    Text(text = option, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}