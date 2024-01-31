package weddellseal.markrecap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp


@Composable
fun DropdownField(options : List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Select an option") }

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            border = BorderStroke(1.dp, Color.LightGray), // Border appearance
        ) {
            Row (
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ){
                BasicTextField(
                    value = selectedOption,
                    onValueChange = { onValueChange(selectedOption) },
                    enabled = false,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            expanded = !expanded
                        }
                    )
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Icon",
                    tint = Color.Black
                )
//                // Trailing Icon
//                Icon(
//                    imageVector = Icons.Default.Clear,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .padding(8.dp)
//                        .clickable {
//                            // Handle the trailing icon click (clear action)
//                            selectedOption = "Select an option"
//                            onValueChange(selectedOption)
//                        }
//                )
            }
        }
        if (expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = BorderStroke(1.dp, Color.LightGray), // Border appearance
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(text = { Text(text = "Select an option") },
                        onClick = {
                            selectedOption = "Select an option"
                            onValueChange(selectedOption)
                            expanded = false
                        }
                    )
                    options.forEach { option ->
                        DropdownMenuItem(text = { Text(text = option) },
                            onClick = {
                                selectedOption = option
                                onValueChange(selectedOption)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}