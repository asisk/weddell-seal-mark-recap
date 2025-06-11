package weddellseal.markrecap.ui.admin.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

@Composable
fun ArchiveCurrentObservationsCard(
    recentObservationsViewModel: RecentObservationsViewModel,
    instructions: String
) {
    val currentObservationsCount by recentObservationsViewModel.currentObservationsCount.collectAsState()
    val recordCountText = "Total Current Observations: $currentObservationsCount"
    var showArchiveDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(275.dp)
            .height(300.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                instructions,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = recordCountText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showArchiveDialog = true },
                modifier = Modifier
                    .padding(start = 16.dp)
            ) {
                Text("Archive")
            }
        }
        // CONFIRM ARCHIVE DIALOG
        // ask the user for confirmation of archiving the current observations
        if (showArchiveDialog) {
            ArchiveDialog(
                onDismissRequest = { showArchiveDialog = false },
                onConfirmation = {
                    showArchiveDialog = false
                    recentObservationsViewModel.markObservationsAsDeleted()
                },
                currentObservationsCount = currentObservationsCount
            )
        }
    }
}