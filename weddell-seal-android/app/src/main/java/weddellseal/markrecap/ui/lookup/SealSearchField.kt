package weddellseal.markrecap.ui.lookup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import weddellseal.markrecap.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SealSearchField(
    viewModel: SealLookupViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchString by rememberSaveable { mutableStateOf("") }
    val focusManager =
        LocalFocusManager.current // State to manage whether the text field should lose focus
    val focusRequester =
        remember { FocusRequester() } // FocusRequester to manage focus programmatically
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = searchString,
        placeholder = { Text("Tag ID or Speno", fontSize = 25.sp) },
        onValueChange = {
            val sanitized = it.replace(Regex("[^A-Za-z0-9]"), "")
            searchString = sanitized.uppercase().trim()
        },
        label = { Text("Tag ID or Speno", fontSize = 25.sp) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 25.sp),
        modifier = Modifier
            .background(color = Color.Transparent)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    // Reset the field in the model when the text field gains focus
                    if (viewModel.uiState.value.sealFound) {
                        searchString = ""
                        viewModel.resetLookupUiState()
                        viewModel.resetLookupSeal()
                    }
                }
            }
            .focusRequester(focusRequester),
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
                viewModel.resetLookupUiState()
                viewModel.resetLookupSeal()

                // engage the search function
                val possiblySpeno = searchString.toIntOrNull()
                if (possiblySpeno != null) {
                    viewModel.findSealbySpeno(possiblySpeno)
                    searchString = possiblySpeno.toString()
                } else {
                    viewModel.findSealbyTagID(searchString)
                }
            }
        ),
        trailingIcon = {
            if (searchString.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_clear),
                    contentDescription = "Clear text",
                    modifier = Modifier
                        .clickable {
                            searchString = ""
                            viewModel.resetLookupUiState()
                            viewModel.resetLookupSeal()
                        }
                        .size(35.dp),
                )
            }
        },
        supportingText = {
            Text(
                text = "ex. 4932A or 30023",
                textAlign = TextAlign.Start,
                fontSize = 20.sp
            )
        }
    )

    if (!uiState.sealFound) {
        IconButton(
            onClick = {
                focusManager.clearFocus()
                val possiblySpeno = searchString.toIntOrNull()
                if (possiblySpeno != null) {
                    viewModel.findSealbySpeno(possiblySpeno)
                    searchString = possiblySpeno.toString()
                } else {
                    viewModel.findSealbyTagID(searchString)
                }
            },
            modifier = Modifier.padding(bottom = 15.dp, end = 20.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                modifier = Modifier.size(45.dp),
            )
        }
    }
}
