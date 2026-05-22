package weddellseal.markrecap.ui.admin.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.files.data.FileState
import weddellseal.markrecap.domain.files.data.color
import weddellseal.markrecap.domain.files.data.iconRes
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

@Composable
fun ExportCurrentObservationsCard(
    state: FileState,
    recentObservationsViewModel: RecentObservationsViewModel,
) {
    val currentObservationsCount by recentObservationsViewModel.currentObservationsCount.collectAsState()

    var errMessage by remember { mutableStateOf("") }
    var exportedFilename by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color(0xFF5884fa)) }
    var statusIcon by remember { mutableIntStateOf(R.drawable.ic_pending) }

    LaunchedEffect(state) {
        errMessage = state.message.toString()
        exportedFilename = state.exportFilename.toString()
        statusColor = state.status.color()
        statusIcon = state.status.iconRes()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(400.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Current WedData",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Text(text = "Current record count: $currentObservationsCount")

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = state.onExportClick,
                enabled = currentObservationsCount > 0
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_file_download),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Export",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export status
            if (state.status == FileStatus.ERROR || state.status == FileStatus.SUCCESS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(statusIcon),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 8.dp)
                    )
                    if (state.status == FileStatus.SUCCESS) {
                        Text(
                            text = exportedFilename + "\nTotal Records Exported: ${state.recordCount}",
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            text = errMessage,
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // RECENT OBSERVATIONS VIEW
            RecentObservationsBrief(recentObservationsViewModel)
        }
    }
}