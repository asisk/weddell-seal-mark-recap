package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ObservationCardOutlinedTextField(
    labelText: String,
    onValueChangeDo: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    // State to manage whether the text field should lose focus
    val keyboardController = LocalSoftwareKeyboardController.current
//    val errorMessage = "Text input too long"
//    var isError by rememberSaveable { mutableStateOf(false) }
    val charLimit = 3

    OutlinedTextField(
        value = text,
        placeholder = { "Observer" },
        onValueChange = {
            text = it
            onValueChangeDo(it)
        },
        label = { Text(labelText) },
        singleLine = true,
        modifier = Modifier
            .background(color = Color.Transparent),
        keyboardActions = KeyboardActions {
            keyboardController?.hide()
        },
        trailingIcon = {
            Icon(
                Icons.Filled.Clear, contentDescription = "Clear text",
                Modifier.clickable { text = "" })
        },
        supportingText = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Limit: ${text.length}/$charLimit",
                textAlign = TextAlign.End,
            )
        }
    )
}