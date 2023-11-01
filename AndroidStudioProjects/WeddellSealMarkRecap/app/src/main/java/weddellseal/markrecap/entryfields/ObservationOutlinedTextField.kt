package weddellseal.markrecap.entryfields

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun ObservationCardOutlinedTextField(placeholderText: String, labelText: String, fieldName: String, onValueChange: (String) -> Unit) {
    val paddingModifier  = Modifier.padding(10.dp)
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = fieldName,
        placeholder = { Text(placeholderText) },
        onValueChange = { onValueChange(it)
            isFocused  = it.isNotBlank()
        },
        label = { Text(labelText) },
        modifier = Modifier
            .background(
                color = if (isFocused) Color.LightGray else Color.Transparent, // Change border color when focused
            ),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                isFocused = fieldName.isNotBlank()
                defaultKeyboardAction((ImeAction.Done))
            }
        )
    )
}