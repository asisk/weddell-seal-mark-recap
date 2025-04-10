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

@Composable
fun WeightSwitch(
    valueInModel: Boolean,
    onValChangeDo: (Boolean) -> Unit
){
    var checked by remember { mutableStateOf(valueInModel) }

    // Update checked state when valueInModel changes
    LaunchedEffect(valueInModel) {
        checked = valueInModel
    }

    Text(
        text = "Weight",
        style = MaterialTheme.typography.titleLarge
    )

    Switch(
        checked = checked,
        onCheckedChange = {
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