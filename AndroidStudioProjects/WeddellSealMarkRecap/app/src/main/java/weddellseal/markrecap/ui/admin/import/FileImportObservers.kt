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
import weddellseal.markrecap.ui.tagretag.ObserversViewModel

@Composable
fun FileImportObservers(
    observersViewModel: ObserversViewModel,
) {
    val context = LocalContext.current

    val state by observersViewModel.fileState.collectAsState()

    var selectedFilename by remember { mutableStateOf("") }
    var fileUploadAction by remember { mutableStateOf("") }

    var errTitle by remember { mutableStateOf("") }
    var errMessage by remember { mutableStateOf("") }
    var showDialogForObserversFileUploadError by remember { mutableStateOf(false) }

    if (showDialogForObserversFileUploadError) {
        FileImportErrorExplanationDialog(
            onDismiss = {
                showDialogForObserversFileUploadError = false
                errMessage = ""
                observersViewModel.setErrAcked(true)
            },
            title = errTitle,
            text = errMessage
        )
    }

    fun handleObserversFileSelection(uri: Uri?) {
        if (uri != null) {
            fileUploadAction = "Importing Observer Initials"
            Log.d("URI", "URI: $uri")
            observersViewModel.resetFileState()

            var fileName = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            selectedFilename = fileName
            Log.d("FileSelection", "File name: $fileName")

            observersViewModel.setLastFilename(fileName)

            val validObserversFile = fileName.startsWith("observers") && fileName.endsWith(".csv")

            if (validObserversFile) {
                observersViewModel.loadObserversFile(uri, fileName)
            } else {
                errTitle = "Error $fileUploadAction"
                errMessage =
                    "File name doesn't match!\nUnexpected file selected:  $selectedFilename\nExpected filename to contain observers.\n"
                observersViewModel.setFileErrorStatus(errMessage)
                showDialogForObserversFileUploadError = true
                Log.e(
                    "FileSelection",
                    "Failed to load file, unexpected file name: $fileName"
                )
            }
        }
    }

    val observersFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleObserversFileSelection(uri)
    }

    // set the upload handlers for each file type when the screen is loaded
    LaunchedEffect(Unit) {
        observersViewModel.setUploadHandler {
            observersFilePicker.launch(arrayOf("*/*"))
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == FileStatus.ERROR) {
            if (state.message != null
                && state.message != ""
                && !observersViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $fileUploadAction"
                errMessage = state.message.toString()
                selectedFilename = state.lastUploadFilename.toString()

                showDialogForObserversFileUploadError = true
            }
        }
    }

    ImportCard(state)
}