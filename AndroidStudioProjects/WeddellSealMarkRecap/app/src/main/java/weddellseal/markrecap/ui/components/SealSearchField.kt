package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.models.WedCheckViewModel

@Composable
fun SealSearchField(viewModel: WedCheckViewModel, onValueChanged: (String) -> Unit) {
    var sealTagID by rememberSaveable { mutableStateOf("") }
    val focusManager: FocusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = sealTagID,
        placeholder = { Text("Tag ID", fontSize = 25.sp) },
        onValueChange = { newText ->
            onValueChanged(sealTagID)
            sealTagID = newText.filter { it != ' ' }
        },
        label = { Text("Seal Tag ID", fontSize = 25.sp) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 25.sp),
        modifier = Modifier
            .background(color = Color.Transparent),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search,
            capitalization = KeyboardCapitalization.Characters,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                // change the focus
                focusManager.clearFocus()
                keyboardController?.hide()

                // reset the current seal for new search
                viewModel.resetState()

                // engage the search function
                viewModel.findSealbyTagID(sealTagID)
            }
        ),
        trailingIcon = {
            Icon(
                Icons.Filled.Clear, contentDescription = "Clear text",
                Modifier
                    .clickable {
                        sealTagID = ""
                        viewModel.resetState()
                    }
                    .size(35.dp) // Adjust the size as needed
            )
        },
        supportingText = {
            Text(
                text = "ex. 4932A",
                textAlign = TextAlign.Start,
                fontSize = 20.sp
            )
        }
    )
}
