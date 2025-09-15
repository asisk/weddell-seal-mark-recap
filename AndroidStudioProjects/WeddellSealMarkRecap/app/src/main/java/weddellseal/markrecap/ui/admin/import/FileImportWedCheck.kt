package weddellseal.markrecap.ui.admin.import

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.WedCheckViewModel

@Composable
fun FileImportWedCheck(
    wedCheckViewModel: WedCheckViewModel,
) {
    val context = LocalContext.current

    val state by wedCheckViewModel.wedCheckUploadState.collectAsState()

    var fileUploadAction by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("") }

    var errTitle by remember { mutableStateOf("") }
    var errMessage by remember { mutableStateOf("") }

    // Add explanation dialog for WedCheck file upload error
    var showDialogForWedCheckFileUploadError by remember { mutableStateOf(false) }
    if (showDialogForWedCheckFileUploadError) {
        FileImportErrorExplanationDialog(
            onDismiss = {
                showDialogForWedCheckFileUploadError = false
                errMessage = ""
                wedCheckViewModel.setErrAcked(true)
            },
            title = errTitle,
            text = errMessage
        )
    }

    // Function to handle the file selection logic
    fun handleWedCheckFileSelection(uri: Uri?) {
        if (uri != null) {
            fileUploadAction = "Importing WedCheck"
            wedCheckViewModel.resetWedCheckUploadState()
            Log.d("URI", "URI: $uri")

            var fileName = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            selectedFilename = fileName
            Log.d("FileSelection", "File name: $fileName")

            //TODO, add validation to ensure that the file is right-sized
//                private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB (adjust as needed)
//                private fun isFileSizeWithinLimit(file: File): Boolean {
//                    val fileSize = file.length()
//                    return fileSize <= MAX_FILE_SIZE_BYTES
//                }

            wedCheckViewModel.setWedCheckLastFilename(fileName)

            val validWedCheck = fileName.startsWith("WedCheck") && fileName.endsWith(".csv")
            val validWedCheckFull = fileName.startsWith("WedCheckFull") && fileName.endsWith(".csv")

            if (validWedCheck || validWedCheckFull) {
                wedCheckViewModel.loadWedCheck(uri, fileName)
            } else {
                errTitle = "Error $fileUploadAction"
                errMessage =
                    "File name doesn't match!\nUnexpected file selected:  $selectedFilename\nExpected filename to contain WedCheck or WedCheckFull.\n"
                wedCheckViewModel.setWedCheckFileErrorStatus(errMessage)
                showDialogForWedCheckFileUploadError = true
                Log.e(
                    "FileSelection",
                    "Failed to load file, unexpected file name: $fileName"
                )
            }
        }
    }

    val wedCheckFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleWedCheckFileSelection(uri)
    }
//
//    LaunchedEffect(state.status) {
//        errMessage = state.message.toString()
//        lastFilename = state.lastUploadFilename.toString()
//        statusColor = state.status.color()
//        statusIcon = state.status.icon()
//    }

    LaunchedEffect(state.status) {
        if (state.status == FileStatus.ERROR) {
            if (state.message != null
                && state.message != ""
                && !wedCheckViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $fileUploadAction"
                errMessage = state.message.toString()
                selectedFilename = state.lastUploadFilename.toString()

                showDialogForWedCheckFileUploadError = true
            }
        }
    }

    // set the upload handlers for each file type when the screen is loaded
    LaunchedEffect(Unit) {
        wedCheckViewModel.setWedCheckUploadHandler {
            wedCheckFilePicker.launch(arrayOf("*/*"))
        }
    }

    ImportCard(state)
}

//@Composable
//fun FailedRowsDisplay(failedRows: List<FailedRow>) {
//    if (failedRows.isNotEmpty()) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            Text(
//                text = "Failed Rows",
//                style = MaterialTheme.typography.titleLarge,
//                color = MaterialTheme.colorScheme.error
//            )
//
//            failedRows.forEach { row ->
//                Text(
//                    text = "Row ${row.rowNumber}: ${row.errorMessage}",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onError
//                )
//            }
//        }
//    } else {
//        Text(
//            text = "No failed rows found.",
//            modifier = Modifier.padding(16.dp),
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.primary
//        )
//    }
//}
