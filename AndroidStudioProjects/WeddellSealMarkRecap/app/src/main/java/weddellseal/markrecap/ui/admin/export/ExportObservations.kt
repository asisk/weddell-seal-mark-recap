package weddellseal.markrecap.ui.admin.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.models.AdminViewModel
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.ui.admin.ExportType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportObservations(
    adminViewModel: AdminViewModel,
    recentObservationsViewModel: RecentObservationsViewModel
) {
    val context = LocalContext.current

    val wedDataCurrentExportState by recentObservationsViewModel.wedDataCurrentExportState.collectAsState()
    val wedDataFullExportState by recentObservationsViewModel.wedDataFullExportState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf(ExportType.CURRENT) }

    val createCurrentObservationsDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
            exportType = ExportType.CURRENT
            recentObservationsViewModel.resetWedDataCurrentFileState()

            if (uri != null) {
                recentObservationsViewModel.updateURI(uri)
                recentObservationsViewModel.exportCurrentRecords(context)
                showExportDialog = true
            } else {
                recentObservationsViewModel.setWedDataCurrentFileErrorStatus("No file location selected for export.")
            }
        }

    val createFullObservationsDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
            exportType = ExportType.ALL
            recentObservationsViewModel.resetWedDataFullFileState()

            if (uri != null) {
                recentObservationsViewModel.updateURI(uri)
                recentObservationsViewModel.exportAllRecords(context)
            } else {
                recentObservationsViewModel.setWedDataFullFileErrorStatus("No file location selected for export.")
            }
        }

    // set the upload handlers for each file type when the screen is loaded
    LaunchedEffect(Unit) {
        val dateTimeFormat =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val currentDateTime = dateTimeFormat.format(Date())

        recentObservationsViewModel.setWedDataCurrentExportHandler {
            createCurrentObservationsDocument.launch("observations_$currentDateTime.csv")
        }
        recentObservationsViewModel.setWedDataFullExportHandler {
            createFullObservationsDocument.launch("all_observations_$currentDateTime.csv")
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row {
            Text(
                modifier = Modifier.padding(bottom = 20.dp),
                text = "Export WedData",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 36.sp,
            )
        }

        Row {
            ExportObservationsCard(
                state = wedDataCurrentExportState,
                instructions = "Export Current Observations",
                recentObservationsViewModel = recentObservationsViewModel,
                ExportType.CURRENT
            )
            ExportObservationsCard(
                state = wedDataFullExportState,
                instructions = "Export All Observations",
                recentObservationsViewModel = recentObservationsViewModel,
                ExportType.ALL
            )
        }

        // EXPORT DIALOG
        // report the records exported
        // ask the user for confirmation of archiving the current observations
        if (showExportDialog) {
            ExportDialog(
                onDismissRequest = { showExportDialog = false },
                fileState = wedDataCurrentExportState,
                onConfirmArchive = {
                    showExportDialog = false
                    adminViewModel.navToArchiveView(4)
                },
            )
        }
    }
}