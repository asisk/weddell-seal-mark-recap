package weddellseal.markrecap.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SingleSelectButtonGroupSquare(
    txtOptions: List<String>,
    valueInModel: String,
    onValChangeDo: (String) -> Unit
) {
    var selectedButton by remember { mutableStateOf(valueInModel) }

    // Synchronize selectedButton with valueInModel whenever it changes
    LaunchedEffect(valueInModel) {
        selectedButton = valueInModel
    }

    txtOptions.forEach { option ->
        ElevatedButton(
            onClick = {
                selectedButton = option
                onValChangeDo(option) // callback when the button is clicked
            },
            shape = RoundedCornerShape(8.dp), // Slightly rounded corners
            colors = ButtonDefaults.elevatedButtonColors(
                if (selectedButton == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            ),
            enabled = selectedButton != option
        ) {
            Text(
                color = Color.White,
                text = option,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}