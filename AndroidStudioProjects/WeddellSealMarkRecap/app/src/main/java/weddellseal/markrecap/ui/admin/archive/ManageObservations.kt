package weddellseal.markrecap.ui.admin.archive


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

@Composable
fun ManageObservations(
    viewModel: RecentObservationsViewModel,
) {

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.padding(bottom = 20.dp),
                text = "Manage Observations",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 36.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(.4f)
                    .padding(end = 8.dp)
            ) {
                ArchiveCurrentObservationsCard(
                    viewModel,
                    instructions = "Archive Current Observation Records"
                )
            }
            Box(
                modifier = Modifier
                    .weight(.4f)
                    .padding(end = 8.dp)
            ) {
                ClearDatabaseCard(
                    viewModel,
                    instructions = "Clear Database"
                )
            }
        }
    }
}