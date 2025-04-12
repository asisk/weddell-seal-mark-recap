package weddellseal.markrecap.ui.file.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.ui.file.FileType
import weddellseal.markrecap.models.WedCheckViewModel

@Composable
fun ExportObservations(
    wedCheckViewModel: WedCheckViewModel,
) {
    val filesStates by wedCheckViewModel.fileStates.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row {
            Text(
                modifier = Modifier.padding(bottom = 20.dp),
                text = "Manage WedData",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 36.sp,
            )
        }
        Row {
            DownloadCard(
                state = filesStates[FileType.WEDDATACURRENT]!!,
                instructions = "Export Current Observations"
            )
            DownloadCard(
                state = filesStates[FileType.WEDDATAFULL]!!,
                instructions = "Export All Observations"
            )
        }
    }


    ExtendedFloatingActionButton(
        modifier = Modifier.padding(16.dp),
        containerColor = Color.LightGray,
        onClick = { //recentObservationsViewModel.markObservationsAsDeleted()
        },
        icon = {
            Icon(
                Icons.Filled.Delete,
                "Mark Observation Records As Deleted",
                Modifier.size(36.dp)
            )
        },
        text = {
            Text(
                "Delete Current Observations",
                style = MaterialTheme.typography.titleLarge
            )
        }
    )
}