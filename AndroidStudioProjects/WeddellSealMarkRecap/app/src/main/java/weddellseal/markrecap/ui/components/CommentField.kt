package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val scrollState = rememberScrollState()
    var textEntered by remember { mutableStateOf(value) }
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = textEntered,
        onValueChange = {
            textEntered = it
        },
        modifier = Modifier
            .background(color = Color.White)
            .border(1.dp, color = Color.LightGray)
            .padding(16.dp)
            .height(40.dp)
            .fillMaxWidth()
            .verticalScroll(state = scrollState, enabled = true),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Handle "Done" button action
                onValueChange(textEntered)
                keyboardController?.hide()
            }
        ),
        textStyle = TextStyle(fontSize = 16.sp),
        singleLine = false,
        maxLines = 5
    )
}