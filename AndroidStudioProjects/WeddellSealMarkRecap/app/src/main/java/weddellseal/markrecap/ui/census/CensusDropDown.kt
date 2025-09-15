package weddellseal.markrecap.ui.census

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CensusDropDown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            readOnly = true,
            value = selectedOption.ifEmpty { "" },
            onValueChange = {},
            label = {
                Text(
                    label, style = MaterialTheme.typography.displaySmall
                )
            },
            trailingIcon = {
                if (!selectedOption.isEmpty()) {
                    // show the clear selection icon
                    IconButton(
                        onClick = {
                            onValueChange("")
                            expanded = false
                            focusManager.clearFocus()  // collapses label
                        }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear selection",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    // show the dropdown icon
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            modifier = modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .padding(30.dp),
            textStyle = MaterialTheme.typography.displaySmall.copy(
                textAlign = TextAlign.Center
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            textAlign = TextAlign.Center,
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
