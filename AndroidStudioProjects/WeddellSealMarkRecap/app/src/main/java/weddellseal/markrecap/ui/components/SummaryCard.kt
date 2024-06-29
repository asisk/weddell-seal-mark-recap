package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.models.AddObservationLogViewModel

@Composable
fun SummaryCard(
    viewModel: AddObservationLogViewModel,
    adult: AddObservationLogViewModel.Seal,
    pupOne: AddObservationLogViewModel.Seal,
    pupTwo: AddObservationLogViewModel.Seal
) {
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // NOTEBOOK DISPLAY
            SummaryListItem("Seal", adult.notebookDataString)
//            Text(text = adult.notebookDataString, style = MaterialTheme.typography.titleLarge)

            if (pupOne.isStarted) {
                SummaryListItem("Pup One", pupOne.notebookDataString)
//                Text(text = pupOne.notebookDataString, style = MaterialTheme.typography.titleLarge)
            }

            if (pupTwo.isStarted) {
                SummaryListItem("Pup Two", pupOne.notebookDataString)
//                Text(text = pupTwo.notebookDataString, style = MaterialTheme.typography.titleLarge)
            }

            SummaryListItem("Location", "TBD")

            // GPS DISPLAY
            if (viewModel.uiState.latLong.isNotEmpty()) {
                SummaryListItem("Device GPS", viewModel.uiState.latLong)
            }

            SummaryListItem("Observers", "TBD")

        }
    }
}