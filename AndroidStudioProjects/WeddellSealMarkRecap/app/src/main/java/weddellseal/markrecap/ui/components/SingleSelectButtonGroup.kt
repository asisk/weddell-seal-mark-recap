package weddellseal.markrecap.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Composable
fun SingleSelectButtonGroup(
    txtOptions: List<String>,
    valueInModel: String,
    onValChangeDo: (String) -> Unit
) {
    var selectedButton by remember { mutableStateOf(valueInModel) }
    txtOptions.forEach { option ->
        ElevatedButton(
            onClick = {
                selectedButton = option
                onValChangeDo(option) // callback when the button is clicked
            },
            colors = ButtonDefaults.elevatedButtonColors(MaterialTheme.colorScheme.tertiary),
            enabled = selectedButton != option
        ) {
            Text(
                color = Color.White,
                text = option
            )
        }
    }
}