package weddellseal.markrecap.ui.admin.archive


import androidx.compose.foundation.layout.Arrangement
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
import weddellseal.markrecap.models.RecentObservationsViewModel

@Composable
fun ArchiveCurrentObservations(
    recentObservationsViewModel: RecentObservationsViewModel,
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

        Row {
            ArchiveCurrentObservationsCard(
                recentObservationsViewModel,
                instructions = "Archive Current Observation Records"
            )
        }
    }
}