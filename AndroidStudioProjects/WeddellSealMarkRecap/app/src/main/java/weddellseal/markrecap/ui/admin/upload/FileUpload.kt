package weddellseal.markrecap.ui.admin.upload

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import weddellseal.markrecap.models.ObserversViewModel
import weddellseal.markrecap.models.SealColoniesViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.admin.FileStatus

@Composable
fun FileUpload(
    wedCheckViewModel: WedCheckViewModel,
    sealColoniesViewModel: SealColoniesViewModel,
    observersViewModel: ObserversViewModel,
) {
    val context = LocalContext.current

    val wedCheckUploadFileState by wedCheckViewModel.wedCheckUploadState.collectAsState()
    val observersFileState by observersViewModel.fileState.collectAsState()
    val sealColonyFileState by sealColoniesViewModel.fileState.collectAsState()

    // Track variables for error descriptions
    var expectedFilename by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("") }
    var uploadAction by remember { mutableStateOf("") }
    var errMessage by remember { mutableStateOf("") }
    var errTitle by remember { mutableStateOf("") }

    // Add explanation dialog for WedCheck file upload error
    var showDialogForWedCheckFileUploadError by remember { mutableStateOf(false) }
    if (showDialogForWedCheckFileUploadError) {
        FileUploadErrorExplanationDialog(
            onDismiss = {
                showDialogForWedCheckFileUploadError = false
                errMessage = ""
                wedCheckViewModel.setErrAcked(true)
            },
            title = errTitle,
            text = errMessage
        )
    }

    var showDialogForObserversFileUploadError by remember { mutableStateOf(false) }
    if (showDialogForObserversFileUploadError) {
        FileUploadErrorExplanationDialog(
            onDismiss = {
                showDialogForObserversFileUploadError = false
                errMessage = ""
                observersViewModel.setErrAcked(true)
            },
            title = errTitle,
            text = errMessage
        )
    }

    var showDialogForColonyFileUploadError by remember { mutableStateOf(false) }
    if (showDialogForColonyFileUploadError) {
        FileUploadErrorExplanationDialog(
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
    fun handleWedCheckFileSelection(uri: Uri?) {
        if (uri != null) {
            uploadAction = "Uploading WedCheck"
            var fileName = ""
            expectedFilename = "WedCheck.csv or WedCheckFull.csv"
            Log.d("FileSelection", "URI: $uri")

            wedCheckViewModel.resetWedCheckUploadState()

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
            if (fileName == "WedCheckFull.csv" || fileName == "WedCheck.csv") {
                wedCheckViewModel.loadWedCheck(uri, fileName)
            } else {
                errTitle = "Error $uploadAction"
                errMessage =
                    "File name doesn't match!\nUnexpected file selected:  $selectedFilename\nExpected file:  $expectedFilename.\n"
                wedCheckViewModel.setWedCheckFileErrorStatus(errMessage)
                showDialogForWedCheckFileUploadError = true
                Log.e(
                    "FileSelection",
                    "Failed to load file, unexpected file name: $fileName"
                )
            }
        }
    }

    fun handleObserversFileSelection(uri: Uri?) {
        if (uri != null) {
            uploadAction = "Uploading Observer Initials"
            var fileName = ""
            expectedFilename = "observers.csv"
            Log.d("FileSelection", "URI: $uri")

            observersViewModel.resetFileState()

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            selectedFilename = fileName
            Log.d("FileSelection", "File name: $fileName")
            observersViewModel.setLastFilename(fileName)

            if (expectedFilename != fileName) {
                errTitle = "Error $uploadAction"
                errMessage =
                    "File name doesn't match!\nUnexpected file selected:  $selectedFilename\nExpected file:  $expectedFilename.\n"
                observersViewModel.setFileErrorStatus(errMessage)
                showDialogForObserversFileUploadError = true
                Log.e(
                    "FileSelection",
                    "Failed to load file, unexpected file name: $fileName"
                )
            } else {
                observersViewModel.loadObserversFile(uri, fileName)
            }
        }
    }

    // Function to handle the file selection logic
    fun handleSealColonyFileSelection(uri: Uri?) {
        if (uri != null) {
            uploadAction = "Uploading Colony Locations"
            var fileName = ""
            expectedFilename = "Colony_Locations.csv"
            Log.d("FileSelection", "URI: $uri")

            sealColoniesViewModel.resetFileState()

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            selectedFilename = fileName
            Log.d("FileSelection", "File name: $fileName")

            sealColoniesViewModel.setLastFilename(fileName)

            if (expectedFilename != fileName) {
                errTitle = "Error $uploadAction"
                errMessage =
                    "File name doesn't match!\nUnexpected file selected:  $selectedFilename\nExpected file:  $expectedFilename.\n"
                sealColoniesViewModel.setFileErrorStatus(errMessage)
                showDialogForColonyFileUploadError = true
                Log.e(
                    "FileSelection",
                    "Failed to load file, unexpected file name: $fileName"
                )
            } else {
                sealColoniesViewModel.loadSealColoniesFile(uri, fileName)
            }
        }
    }

    val wedCheckFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleWedCheckFileSelection(uri)
    }

    val observersFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleObserversFileSelection(uri)
    }

    val colonyFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleSealColonyFileSelection(uri)
    }

    // set the upload handlers for each file type when the screen is loaded
    LaunchedEffect(Unit) {
        wedCheckViewModel.setWedCheckUploadHandler {
            wedCheckFilePicker.launch(arrayOf("*/*"))
        }
        observersViewModel.setUploadHandler {
            observersFilePicker.launch(arrayOf("*/*"))
        }
        sealColoniesViewModel.setUploadHandler {
            colonyFilePicker.launch(arrayOf("*/*"))
        }
    }

    LaunchedEffect(wedCheckUploadFileState.status) {
        if (wedCheckUploadFileState.status == FileStatus.ERROR) {
            if (wedCheckUploadFileState.message != null
                && wedCheckUploadFileState.message != ""
                && !wedCheckViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $uploadAction"
                errMessage = wedCheckUploadFileState.message.toString()
                selectedFilename = wedCheckUploadFileState.lastUploadFilename.toString()

                showDialogForWedCheckFileUploadError = true
            }
        }
    }

    LaunchedEffect(observersFileState.status) {
        if (observersFileState.status == FileStatus.ERROR) {
            if (observersFileState.message != null
                && observersFileState.message != ""
                && !observersViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $uploadAction"
                errMessage = observersFileState.message.toString()
                selectedFilename = observersFileState.lastUploadFilename.toString()

                showDialogForObserversFileUploadError = true
            }
        }
    }

    LaunchedEffect(sealColonyFileState.status) {
        if (sealColonyFileState.status == FileStatus.ERROR) {
            if (sealColonyFileState.message != null
                && sealColonyFileState.message != ""
                && !sealColoniesViewModel.uiState.value.errAcked
            ) {
                errTitle = "Error $uploadAction"
                errMessage = sealColonyFileState.message.toString()
                selectedFilename = sealColonyFileState.lastUploadFilename.toString()

                showDialogForColonyFileUploadError = true
            }
        }
    }

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
                text = "Manage Uploads",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 36.sp,
            )
        }
        UploadCard(state = wedCheckUploadFileState)
        UploadCard(state = observersFileState)
        UploadCard(state = sealColonyFileState)
    }
}