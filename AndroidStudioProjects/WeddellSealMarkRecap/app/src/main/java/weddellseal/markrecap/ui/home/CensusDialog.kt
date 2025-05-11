package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import weddellseal.markrecap.models.TagRetagModel

@Composable
fun CensusDialog(
    obsViewModel: TagRetagModel,
    onClearRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { onClearRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp)
                .verticalScroll(state = scrollState, enabled = true),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = "Select Census Number",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val censusOptions = listOf("1", "2", "3", "4")

                    CensusButtonGroupSquare(
                        txtOptions = censusOptions,
                        valueInModel = obsViewModel.uiState.censusNumber,
                        onValChangeDo = { obsViewModel.updateCensusNumber(it) },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val censusOptions = listOf("5", "6", "7", "8")

                    CensusButtonGroupSquare(
                        txtOptions = censusOptions,
                        valueInModel = obsViewModel.uiState.censusNumber,
                        onValChangeDo = { obsViewModel.updateCensusNumber(it) },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        containerColor = Color.LightGray,
                        onClick = { onClearRequest()},
                        icon = { // no icon
                        },
                        text = {
                            Text(
                                "Clear Selection",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        containerColor = Color.LightGray,
                        onClick = { onConfirmation() },
                        icon = { // no icon
                        },
                        text = {
                            Text(
                                "Begin Data Entry",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                }
            }
        }
    }
}