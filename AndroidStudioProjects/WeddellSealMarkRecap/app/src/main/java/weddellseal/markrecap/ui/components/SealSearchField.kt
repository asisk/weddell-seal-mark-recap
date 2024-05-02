package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import weddellseal.markrecap.models.AddObservationLogViewModel

class SealSearchField {

}
@Composable
fun SealSearchField(viewModel: AddObservationLogViewModel, onValueChanged: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = text,
        placeholder = { "SpeNo" },
        onValueChange = {
            text = it
            onValueChanged(text)
        },
        label = { Text("Seal SpeNo") },
        singleLine = true,
        modifier = Modifier
            .background(color = Color.Transparent),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions {
            onSearchClicked(viewModel)
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
                text = "Numeric value",
                textAlign = TextAlign.End,
            )
        }
    )
}

fun onSearchClicked(viewModel: AddObservationLogViewModel) {
    viewModel.uiState.isLoading = true
}
