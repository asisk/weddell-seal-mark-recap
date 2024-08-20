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
    onValueChangeDo: (String) -> Unit,
    onClearValueDo: () -> Unit
) {
    var text by remember { mutableStateOf(value) }

    val focusManager =
        LocalFocusManager.current // State to manage whether the text field should lose focus
    val keyboardController = LocalSoftwareKeyboardController.current

    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        text = value
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it

            // Ensure the input is exactly 3 or 4 digits
            val isValidLength = text.length == 3 || text.length == 4
            isError = it.isEmpty() || !isValidLength
            if (!isError) {
                // save the input to the model
                onValueChangeDo(it)
            }
        },
        label = { Text(labelText) },
        placeholder = { Text(placeholderText) },
        textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ),
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
                text = "Tag Number is 3 or 4 digits",
                textAlign = TextAlign.End,
            )
        }
    )
}