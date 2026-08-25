package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import weddellseal.markrecap.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

@Composable
fun TagIDOutlinedTextField(
    value: String,
    labelText: String,
    placeholderText: String,
    keyboardType: KeyboardType,
    onClearValueDo: () -> Unit,
    onValueChange: (String) -> Unit = {},
    onFocusChange: (Boolean, String) -> Unit, // Pass both focus state and latest value
    // Changes when the form resets so local text/focus state is recreated (fix #3).
    fieldKey: Any = 0,
) {
    val focusManager =
        LocalFocusManager.current // State to manage whether the text field should lose focus
    val focusRequester =
        remember { FocusRequester() } // FocusRequester to manage focus programmatically
    var isFocused by remember(fieldKey) { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Local buffer while typing; fieldKey reset recreates this from the cleared model value.
    var text by remember(fieldKey) { mutableStateOf(value) }

    // Sync from the model only when not focused; otherwise recomposition can reset in-progress
    // edits (pending tag number) back to the last committed value.
    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            text = value
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            val sanitized = it.replace(Regex("[^0-9]"), "") // only allow numeric characters
            text = sanitized.trim()
            onValueChange(text)
        },
        label = { Text(labelText) },
        placeholder = { Text(placeholderText) },
        textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f // Instead of `ContentAlpha.disabled`, use a manual alpha value
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
            if (text.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_clear),
                    contentDescription = "Clear text",
                    modifier = Modifier.clickable {
                        text = ""
                        onClearValueDo()
                    },
                )
            }
        },
        modifier = Modifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused // Update focus state
                onFocusChange(isFocused, text.trim()) // Pass the latest value when focus changes
            }
            .focusRequester(focusRequester)
    )
}