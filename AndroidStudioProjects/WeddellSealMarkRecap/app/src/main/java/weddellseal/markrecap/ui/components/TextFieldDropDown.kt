package weddellseal.markrecap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

@Composable
fun TextFieldDropDown(
    charNumber: Int,
    fieldLabel: String,
    placeHolderTxt: String,
    leadIcon: Icons?
) {
    var text by rememberSaveable { mutableStateOf("") }
    val errorMessage = "Text input too long"
    var isError by rememberSaveable { mutableStateOf(false) }
    val charLimit = charNumber

    fun validate(text: String) {
        isError = text.length > charLimit
    }

    TextField(
        value = text,
        onValueChange = {
            text = it
            validate(text)
        },
        label = { Text(fieldLabel) },
        placeholder = { Text(placeHolderTxt) },
        singleLine = true,
        supportingText = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Limit: ${text.length}/$charLimit",
                textAlign = TextAlign.End,
            )
        },
        isError = isError,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        keyboardActions = KeyboardActions { validate(text) },
        modifier = Modifier.semantics {
            // Provide localized description of the error
            if (isError) error(errorMessage)
        },
        leadingIcon = {
            if (leadIcon != null) {
//               Icon(
//                   Icons.Filled.Favorite,
//                   contentDescription = "Localized description"
//               )
            }
        },
        trailingIcon = {
            Icon(Icons.Filled.Clear, contentDescription = "Localized description",
                Modifier.clickable { text = "" })
        }
    )

}