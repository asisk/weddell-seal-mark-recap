package weddellseal.markrecap.ui.tagretag

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommentField(
    value: String,
    onClearValueDo: () -> Unit,
    onFocusChange: (Boolean, String) -> Unit // Pass both focus state and latest value
) {
    val scrollState = rememberScrollState()

    val focusManager: FocusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) } // Track focus state
    val keyboardController = LocalSoftwareKeyboardController.current

    var text by remember { mutableStateOf(value) } //used to prevent the model update until the user is done typing

    LaunchedEffect(value) {
        text = value
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            val sanitized = it.replace(Regex("[^A-Za-z0-9;! ]"), "") // only allow numeric characters
            text = sanitized.trim()
        },
        label = { Text("Comments") },
        supportingText = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Comment field allows letters, numbers, semicolons, and exclamation points!",
                textAlign = TextAlign.End,
            )
        },
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(state = scrollState, enabled = true)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused // Update focus state
                Log.d(
                    "CommentField",
                    "Focus change detected isFocused: $isFocused, calling onFocusChange lambda"
                )
                onFocusChange(isFocused, text.trim()) // Pass the latest value when focus changes
            },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f // Instead of `ContentAlpha.disabled`, use a manual alpha value
            ),
        ),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Handle "Done" button action
                // change the focus
                focusManager.clearFocus()

                //hide the keyboard
                keyboardController?.hide()
            }
        ),
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Filled.Clear, contentDescription = "Clear text",
                    Modifier.clickable {
                        text = ""
                        onClearValueDo()
                    }
                )
            }
        },
        textStyle = TextStyle(fontSize = 16.sp),
        singleLine = false,
        maxLines = 5
    )
}