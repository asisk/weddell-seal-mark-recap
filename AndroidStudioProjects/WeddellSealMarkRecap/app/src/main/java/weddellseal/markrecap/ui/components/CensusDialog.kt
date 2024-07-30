package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import weddellseal.markrecap.models.AddObservationLogViewModel

@Composable
fun CensusDialog(
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
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "This is a dialog with buttons.",
                    modifier = Modifier.padding(16.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val censusOptions = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8")
                    var selection by remember { mutableStateOf(obsViewModel.uiState.censusNumber) }

                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(.3f)
                    ) {
                        Text(text = "Census #")
                    }
                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(.5f)
                    ) {
                        DropdownField(censusOptions, obsViewModel.uiState.censusNumber) { newText ->
                            selection = newText
                            obsViewModel.updateCensusNumber(newText)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
//                    TextButton(
//                        onClick = { onDismissRequest() },
//                        modifier = Modifier.padding(8.dp),
//                    ) {
//                        Text("Dismiss")
//                    }
                    TextButton(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm & Begin Data Entry")
                    }
                }
            }
        }
    }
}