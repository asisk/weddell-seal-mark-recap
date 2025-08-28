package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.sp

@Composable
fun PupWeightOutlinedTextField(
    value: String,
    onFocusChange: (Boolean, String) -> Unit,
    onClearValueDo: () -> Unit,
) {
    val focusManager =
        LocalFocusManager.current // State to manage whether the text field should lose focus
    val focusRequester =
        remember { FocusRequester() } // FocusRequester to manage focus programmatically
    var isFocused by remember { mutableStateOf(false) } // Track focus state

    val keyboardController = LocalSoftwareKeyboardController.current

    var text by remember { mutableStateOf(if (value == "0") "" else value) } //used to prevent the model update until the user is done typing

    LaunchedEffect(value) {
        text = if (value == "0") "" else value
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            val sanitized = it.replace(Regex("[^0-9]"), "") // only allow numeric characters
            text = sanitized.trim()
        },
        label = { Text("Weight in lbs") },
        placeholder = { "Enter weight in lbs" },
        textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (text.isNotEmpty() && text.length !in 2..4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (text.isNotEmpty() && text.length !in 2..4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        ),
        trailingIcon = {
            if (text.isNotEmpty()) {
                Icon(
                    Icons.Filled.Clear, contentDescription = "Clear text",
                    Modifier.clickable {
                        text = ""
                        onClearValueDo()
                    }
                )
            }
        },
        suffix = { Text("lbs") },
        modifier = Modifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused // Update focus state
                onFocusChange(isFocused, text.trim()) // Pass the latest value when focus changes
            }
            .focusRequester(focusRequester)
    )
}