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
import weddellseal.markrecap.ui.home.SealColoniesViewModel

@Composable
fun FileImportColonies(
    sealColoniesViewModel: SealColoniesViewModel,
) {
    val context = LocalContext.current

    val state by sealColoniesViewModel.fileState.collectAsState()

    var fileUploadAction by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("") }

    var errTitle by remember { mutableStateOf("") }
    var errMessage by remember { mutableStateOf("") }

    var showDialogForColonyFileUploadError by remember { mutableStateOf(false) }
    if (showDialogForColonyFileUploadError) {
        FileImportErrorExplanationDialog(
            onDismiss = {
                showDialogForColonyFileUploadError = false
                errMessage = ""
                sealColoniesViewModel.setErrAcked(true)
            },
            title = errTitle,
            text = errMessage
        )
    }

    // Function to handle the file selection logic
    fun handleSealColonyFileSelection(uri: Uri?) {
        if (uri != null) {
            fileUploadAction = "Importing Colony Locations"
            sealColoniesViewModel.resetFileState()
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

            sealColoniesViewModel.setLastFilename(fileName)

            val validColonyFile = fileName.startsWith("Colony_Locations") && fileName.endsWith(".csv")

            if (validColonyFile) {
                sealColoniesViewModel.loadSealColoniesFile(uri, fileName)
            } else {
                errTitle = "Error $fileUploadAction"
                errMessage =
                    "File name doesn't match!\nUnexpected file selected:  $selectedFilename\nExpected filename to contain Colony_Locations.\n"
                sealColoniesViewModel.setFileErrorStatus(errMessage)
                showDialogForColonyFileUploadError = true
                Log.e(
                    "FileSelection",
                    "Failed to load file, unexpected file name: $fileName"
                )
            }
        }
    }

    val colonyFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleSealColonyFileSelection(uri)
    }

    // set the upload handlers for each file type when the screen is loaded
    LaunchedEffect(Unit) {
        sealColoniesViewModel.setUploadHandler {
            colonyFilePicker.launch(arrayOf("*/*"))
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == FileStatus.ERROR) {
            if (state.message != null
                && state.message != ""
                && !sealColoniesViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $fileUploadAction"
                errMessage = state.message.toString()
                selectedFilename = state.lastUploadFilename.toString()

                showDialogForColonyFileUploadError = true
            }
        }
    }

    ImportCard(state)
}