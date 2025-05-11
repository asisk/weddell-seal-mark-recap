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
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.utils.getFileExportDateTime

@Composable
fun ExportObservations(
    adminViewModel: AdminViewModel,
    recentObservationsViewModel: RecentObservationsViewModel
) {
    val context = LocalContext.current

    val uiState by recentObservationsViewModel.uiState.collectAsState()
    val wedDataCurrentExportState by recentObservationsViewModel.wedDataCurrentExportState.collectAsState()
    val wedDataFullExportState by recentObservationsViewModel.wedDataFullExportState.collectAsState()
    var exportType by remember { mutableStateOf(ExportType.CURRENT) }
    var fileAction by remember { mutableStateOf("") }
    var exportFilename by remember { mutableStateOf("") }
    var errMessage by remember { mutableStateOf("") }
    var errTitle by remember { mutableStateOf("") }

    var showDialogForFileExportError by remember { mutableStateOf(false) }
    if (showDialogForFileExportError) {
        FileExportErrorExplanationDialog(
            onDismiss = {
                showDialogForFileExportError = false
                errMessage = ""
                recentObservationsViewModel.setErrAcked(true)
            },
            title = errTitle,
            text = errMessage
        )
    }

    var showDialogArchiveCurrentObservations by remember { mutableStateOf(false) }
    if (showDialogArchiveCurrentObservations) {
        ExportDialogCurrentObservations(
            onDismissRequest = {
                showDialogArchiveCurrentObservations = false
                recentObservationsViewModel.setArchiveAcked(true)
            },
            fileState = wedDataCurrentExportState,
            onConfirmArchive = {
                showDialogArchiveCurrentObservations = false
                recentObservationsViewModel.setArchiveAcked(true)
                adminViewModel.navToArchiveView(4)
            },
        )
    }

    val createCurrentObservationsDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            fileAction = "Exporting Current Observations"
            exportType = ExportType.CURRENT

            if (uri != null) {
                recentObservationsViewModel.resetWedDataCurrentFileState()
                recentObservationsViewModel.updateURI(uri)
                recentObservationsViewModel.exportRecords(context, exportType)
            } else {
                errTitle = "Error $fileAction"
                errMessage = "No file location selected for export."
                recentObservationsViewModel.setWedDataCurrentFileErrorStatus("No file location selected for export.")
            }
        }

    val createFullObservationsDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            fileAction = "Exporting All Observations"
            exportType = ExportType.ALL

            if (uri != null) {
                recentObservationsViewModel.resetWedDataFullFileState()
                recentObservationsViewModel.updateURI(uri)
                recentObservationsViewModel.exportRecords(context, exportType)
            } else {
                errTitle = "Error $fileAction"
                errMessage = "No file location selected for export."
                recentObservationsViewModel.setWedDataFullFileErrorStatus("No file location selected for export.")
            }
        }

    LaunchedEffect(wedDataCurrentExportState.status) {
        if (wedDataCurrentExportState.status == FileStatus.ERROR) {
            if (wedDataCurrentExportState.message != null
                && wedDataCurrentExportState.message != ""
                && !recentObservationsViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $fileAction"
                errMessage = wedDataCurrentExportState.message.toString()
                exportFilename = wedDataCurrentExportState.exportFilename.toString()

                showDialogForFileExportError = true
            }
        } else if (wedDataCurrentExportState.status == FileStatus.SUCCESS && uiState.archiveAcked == false) {
            showDialogArchiveCurrentObservations = true
        }
    }

    LaunchedEffect(wedDataFullExportState.status) {
        if (wedDataFullExportState.status == FileStatus.ERROR) {
            if (wedDataFullExportState.message != null
                && wedDataFullExportState.message != ""
                && !recentObservationsViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $fileAction"
                errMessage = wedDataFullExportState.message.toString()
                exportFilename = wedDataFullExportState.exportFilename.toString()

                showDialogForFileExportError = true
            }
        }
    }

    // set the upload handlers for each file type when the screen is loaded
    LaunchedEffect(Unit) {
        val fileDate = getFileExportDateTime()

        recentObservationsViewModel.setWedDataCurrentExportHandler {
            createCurrentObservationsDocument.launch("observations_$fileDate.csv")
        }
        recentObservationsViewModel.setWedDataFullExportHandler {
            createFullObservationsDocument.launch("all_observations_$fileDate.csv")
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
    }
}