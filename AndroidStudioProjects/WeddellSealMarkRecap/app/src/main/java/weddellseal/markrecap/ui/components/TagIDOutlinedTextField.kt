package weddellseal.markrecap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun TagIDOutlinedTextField(
    value: String,
    labelText: String,
    placeholderText: String,
    errorMessage: String,
    keyboardType: KeyboardType,
    onValueChangeDo: (String) -> Unit,
    onClearValueDo: () -> Unit,
    onFocusChange: (Boolean, String) -> Unit // Pass both focus state and latest value
) {
    var text by remember { mutableStateOf(value) }
//    var isError by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val focusManager =
        LocalFocusManager.current // State to manage whether the text field should lose focus
    val focusRequester = remember { FocusRequester() } // FocusRequester to manage focus programmatically
    var isFocused by remember { mutableStateOf(false) } // Track focus state

    LaunchedEffect(value) {
        text = value
    }

    // Detect focus changes and trigger the callback
    LaunchedEffect(isFocused) {
        onFocusChange(isFocused, text.trim()) // Pass the latest value when focus changes
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            if (it.isNotEmpty()) {
                // save the input to the model
                onValueChangeDo(it)
            }
        },
        label = { Text(labelText) },
        placeholder = { Text(placeholderText) },
        textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ),
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = keyboardType,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        ),
        trailingIcon = {
            Icon(
                Icons.Filled.Clear, contentDescription = "Clear text",
                Modifier.clickable {
                    text = ""
                    onClearValueDo()
                }
            )
        },
        supportingText = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = errorMessage,
                textAlign = TextAlign.End,
            )
        },
        modifier = Modifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused // Update focus state
            }
            .focusRequester(focusRequester)
    )
}