package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SingleSelectTagAlphaButtonGroup(
    txtOptions: List<String>,
    valueInModel: String,
    onValChangeDo: (String) -> Unit
) {
    var selectedButton by remember { mutableStateOf(valueInModel) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(valueInModel) {
        selectedButton = valueInModel
    }

    txtOptions.forEach { option ->
        Button(
            onClick = {
                // this hides the keyboard when a user selects the alpha button without selecting done on the keyboard
                focusManager.clearFocus()

                selectedButton = option
                onValChangeDo(option)
            },
            shape = RoundedCornerShape(8.dp), // Slightly rounded corners
            modifier = Modifier
                .width(60.dp)  // Set desired width
                .height(60.dp)  // Set desired height
                .border(
                width = if (selectedButton == option) 8.dp else 0.dp,
                color = if (selectedButton == option) Color.DarkGray else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (option) {
                    "A" -> Color(0xFFEED11E)
                    "C" -> Color(0xFF379DCD)
                    "D" -> Color(0xFFB7CF83)
                    else -> Color.Unspecified
                }
            )
        ) {
            Text(
                text = option,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
                                    color = Color.Black,
            )
        }
    }
}
