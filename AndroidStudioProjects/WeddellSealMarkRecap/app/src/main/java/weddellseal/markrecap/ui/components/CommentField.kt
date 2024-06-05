package weddellseal.markrecap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommentField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val focusManager: FocusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    var textEntered by remember { mutableStateOf(value) }
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = textEntered,
        onValueChange = {
            textEntered = it
        },
        label = { Text("Comments") },
        modifier = Modifier
//            .background(color = Color.White)
//            .border(1.dp, color = Color.LightGray)
//            .padding(16.dp)
            .height(80.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(state = scrollState, enabled = true),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
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

                onValueChange(textEntered)
            }
        ),
        trailingIcon = {
            Icon(
                Icons.Filled.Clear, contentDescription = "Clear text",
                Modifier.clickable { textEntered = "" }
            )
        },
        textStyle = TextStyle(fontSize = 16.sp),
        singleLine = false,
        maxLines = 5
    )
}