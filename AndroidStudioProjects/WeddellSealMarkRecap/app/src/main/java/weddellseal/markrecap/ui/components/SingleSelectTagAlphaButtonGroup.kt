package weddellseal.markrecap.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
@Composable
fun SingleSelectTagAlphaButtonGroup(
    txtOptions: List<String>,
    valueInModel: String,
    onValChangeDo: (String) -> Unit
) {
var selectedButton by remember { mutableStateOf(valueInModel) }

    txtOptions.forEach { option ->
        Button(
            onClick = {
                selectedButton = option
                onValChangeDo(option)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = when (option) {
                    "A" -> if (selectedButton == option) Color.Gray else Color.Yellow
                    "C" -> if (selectedButton == option) Color.Gray else Color.Blue
                    "D" -> if (selectedButton == option) Color.Gray else Color.Green
                    else -> Color.Unspecified
                }
            )
        ) {
            Text(
                option,
                color = when (option) {
                    "A" -> if (selectedButton == option) Color.White else Color.Black
                    "C" -> if (selectedButton == option) Color.White else Color.White
                    "D" -> if (selectedButton == option) Color.White else Color.Black
                    else -> Color.Black
                }
            )
        }
    }
}
