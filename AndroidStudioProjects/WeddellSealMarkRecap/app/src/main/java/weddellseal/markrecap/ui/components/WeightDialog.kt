package weddellseal.markrecap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import weddellseal.markrecap.models.AddObservationLogViewModel

@Composable
fun WeightDialog(
    obsViewModel: AddObservationLogViewModel,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            //TAG ID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(.3f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        val focusManager: FocusManager = LocalFocusManager.current
                        var weightRecorded by remember { mutableStateOf("") }

                        Text(
                            "Weight",
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedTextField(
                            value = weightRecorded,
                            onValueChange = {
                                weightRecorded = it
                            },
                            label = {
                                Text(
                                    "Weight",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            textStyle = TextStyle(fontSize = 20.sp), // Set custom text size here
                            colors = OutlinedTextFieldDefaults.colors(
                                MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = ContentAlpha.disabled
                                ),
                            ),
                            placeholder = { Text("Enter Weight in lbs") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Handle "Done" button action

                                    // change the focus
                                    focusManager.clearFocus()

                                    // save the input to the model
//                                    val number: Int? = weightRecorded.toIntOrNull()
//                                    if (number != null && !isError) {
//                                        viewModel.updateTagOneNumber(seal, number)
//                                    }
                                },
                            ),
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Clear, contentDescription = "Clear text",
                                    Modifier.clickable {
//                                        weightRecorded = 0
//                                        viewModel.clearTagOne(seal)
                                    })
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = { onConfirmation() },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Yes, delete entry")
                }
            }
        }
    }
}