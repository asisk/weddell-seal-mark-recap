package weddellseal.markrecap.ui

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
            .padding(8.dp)
            .fillMaxWidth(.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // NOTEBOOK DISPLAY
            val headlineText = adult.age + " Seal Details: "
            SummaryListItem(headlineText, adult.notebookDataString)

            if (pupOne.isStarted) {
                val pupHeadlineText = pupOne.age + " Seal Details: "
                SummaryListItem(pupHeadlineText, pupOne.notebookDataString)
            }

            if (pupTwo.isStarted) {
                val pupTwoHeadlineText = pupTwo.age + " Seal Details: "
                SummaryListItem(pupTwoHeadlineText, pupTwo.notebookDataString)
            }

            // GPS DISPLAY
            if (viewModel.uiState.latLong.isNotEmpty()) {
                SummaryListItem("Device GPS", viewModel.uiState.latLong)
            }
        }
    }
}