package weddellseal.markrecap.ui.admin.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pending
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.frameworks.room.files.color
import weddellseal.markrecap.frameworks.room.files.icon
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.ui.admin.ExportType
import weddellseal.markrecap.ui.admin.FileStatus

@Composable
fun ExportObservationsCard(
    state: FileState,
    instructions: String,
    recentObservationsViewModel: RecentObservationsViewModel,
    exportType: ExportType,
) {
    val currentObservationsCount by recentObservationsViewModel.currentObservationsCount.collectAsState()
    val allObservationsCount by recentObservationsViewModel.allObservationsCount.collectAsState()

    var text by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color(0xFF5884fa)) }
    var statusIcon by remember { mutableStateOf(Icons.Default.Pending) }

    LaunchedEffect(state.status) {
        text = state.message + "\n" + state.exportFilename
        statusColor = state.status.color()
        statusIcon = state.status.icon()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(275.dp)
            .height(425.dp)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.export_notes),
                    null,
                    modifier = Modifier.size(48.dp),
                    colorFilter = ColorFilter.tint(Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = state.fileType)
            }

            var recordCount = if (exportType == ExportType.ALL) {
                allObservationsCount
            } else if (exportType == ExportType.CURRENT) {
                currentObservationsCount
            } else {
                "0"
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Records available for export: $recordCount")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = state.onExportClick,
                enabled = if (exportType == ExportType.ALL && allObservationsCount > 0) {
                    true
                } else if (exportType == ExportType.CURRENT && currentObservationsCount > 0) {
                    true
                } else {
                    false
                },
                modifier = Modifier
                    .padding(start = 16.dp)
            ) {
                Text("Export")
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
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = text,
                        color = statusColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}