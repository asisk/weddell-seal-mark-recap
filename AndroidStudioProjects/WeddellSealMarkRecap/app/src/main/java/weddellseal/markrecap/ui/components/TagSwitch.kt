package weddellseal.markrecap.ui.components


import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun TagSwitch(
    valueInModel: String,
    onValChangeDo: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(valueInModel == "NoTag") }
    val focusManager = LocalFocusManager.current

    // Update checked state when valueInModel changes
    LaunchedEffect(valueInModel) {
        checked = valueInModel == "NoTag"
    }

    Text(
        text = "No Tag",
        style = MaterialTheme.typography.titleMedium
    )

    Switch(
        checked = checked,
        onCheckedChange = {
            focusManager.clearFocus()
            checked = it
            onValChangeDo(it) // callback when the button is clicked
        },
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        }
    )
}