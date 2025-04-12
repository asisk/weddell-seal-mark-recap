package weddellseal.markrecap.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.ui.file.FileAction
import weddellseal.markrecap.ui.file.FileStatus
import weddellseal.markrecap.ui.file.FileType
import weddellseal.markrecap.models.AdminViewModel
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.models.ObserversViewModel
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.SealColoniesViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.file.DashboardScreen
import weddellseal.markrecap.ui.file.download.ExportObservations
import weddellseal.markrecap.ui.file.upload.FileUploadErrorExplanationDialog
import weddellseal.markrecap.ui.file.upload.UploadDataFileScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavHostController,
    wedCheckViewModel: WedCheckViewModel,
    sealColoniesViewModel: SealColoniesViewModel,
    observersViewModel: ObserversViewModel,
    homeViewModel: HomeViewModel,
    adminViewModel: AdminViewModel,
    recentObservationsViewModel: RecentObservationsViewModel
) {
    val context = LocalContext.current

    // Track which file name failed
    var filenameStr by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("") }
    var uploadAction by remember { mutableStateOf("") }

    // Navigation Rail
    var selectedItem by remember { mutableIntStateOf(1) }
    val items =
        listOf("Home", "Dashboard", "Upload", "Export")
    val selectedIcons = listOf(
        Icons.Default.Home,
        Icons.Default.Dashboard,
        Icons.Default.UploadFile,
        Icons.Default.FileDownload,
    )
    val unselectedIcons =
        listOf(
            Icons.Outlined.Home,
            Icons.Outlined.Dashboard,
            Icons.Outlined.FileUpload,
            Icons.Outlined.FileDownload,
        )

    val createCurrentObservationsDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
            // Handle the created document URI
            if (uri != null) {
                recentObservationsViewModel.updateURI(uri)
                recentObservationsViewModel.exportLogs(context)
            }
        }

    val createFullObservationsDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val dateTimeFormat =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val currentDateTime = dateTimeFormat.format(Date())
            val filename = "all_observations_$currentDateTime.csv"

            // Handle the created document URI
            if (uri != null) {
                recentObservationsViewModel.updateURI(uri)
                recentObservationsViewModel.exportAllLogs(context)
            }
        }

    // Add explanation dialog for file name validation error
    var showExplanationDialogForFileMatchError by remember { mutableStateOf(false) }
    if (showExplanationDialogForFileMatchError) {
        FileUploadErrorExplanationDialog(
            onDismiss = { showExplanationDialogForFileMatchError = false },
            title = "Error $uploadAction\n\n      File name doesn't match!",
            text = "            File selected:  $selectedFilename\n            Expected file:  $filenameStr.\n\n",
        )
    }

    // Function to handle the file selection logic
    fun handleFileSelection(uri: Uri?, expectedFileName: String) {
        if (uri != null) {
            var fileName = ""
            filenameStr = expectedFileName
            Log.d("FileSelection", "URI: $uri")

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

            when (expectedFileName) {
                "WedCheck.csv" -> {
                    if (expectedFileName != fileName) {
                        wedCheckViewModel.setLastFilename(FileType.WEDCHECK, fileName)
                        wedCheckViewModel.setFileErrorStatus(
                            FileType.WEDCHECK,
                            FileStatus.ERROR,
                            "Failed to load file, unexpected file name: $fileName"
                        )

                        showExplanationDialogForFileMatchError = true
                        Log.e(
                            "FileSelection",
                            "Failed to load file, unexpected file name: $fileName"
                        )
                    } else {
                        wedCheckViewModel.setLastFilename(FileType.WEDCHECK, fileName)
                        wedCheckViewModel.loadWedCheck(uri, fileName)
                    }
                }

                "observers.csv" -> {
                    if (expectedFileName != fileName) {
                        observersViewModel.setLastFilename(fileName)
                        observersViewModel.setFileErrorStatus("Failed to load file, unexpected file name: $fileName")

                        showExplanationDialogForFileMatchError = true
                        Log.e(
                            "FileSelection",
                            "Failed to load file, unexpected file name: $fileName"
                        )
                    } else {
                        observersViewModel.setLastFilename(fileName)
                        observersViewModel.loadObserversFile(uri, fileName)
                    }
                }

                "Colony_Locations.csv" -> {
                    if (expectedFileName != fileName) {
                        sealColoniesViewModel.setLastFilename(fileName)
                        sealColoniesViewModel.setFileErrorStatus("Failed to load file, unexpected file name: $fileName")

                        showExplanationDialogForFileMatchError = true
                        Log.e(
                            "FileSelection",
                            "Failed to load file, unexpected file name: $fileName"
                        )
                    } else {
                        sealColoniesViewModel.setLastFilename(fileName)
                        sealColoniesViewModel.loadSealColoniesFile(uri, fileName)
                    }
                }
            }
        }
    }

    val observersFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uploadAction = "Uploading Observer Initials"
        handleFileSelection(uri, "observers.csv")
    }

    val wedCheckFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uploadAction = "Uploading WedCheck"
        handleFileSelection(uri, "WedCheck.csv")
    }

    val colonyFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uploadAction = "Uploading Colony Locations"
        handleFileSelection(uri, "Colony_Locations.csv")
    }

    // set the upload handlers for each file type when the screen is loaded
    LaunchedEffect(Unit) {
        val dateTimeFormat =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val currentDateTime = dateTimeFormat.format(Date())

        wedCheckViewModel.setFileHandler(FileType.WEDCHECK, FileAction.UPLOAD) {
            wedCheckFilePicker.launch(arrayOf("*/*"))
        }
        observersViewModel.setUploadHandler {
            observersFilePicker.launch(arrayOf("*/*"))
        }
        sealColoniesViewModel.setUploadHandler {
            colonyFilePicker.launch(arrayOf("*/*"))
        }
        wedCheckViewModel.setFileHandler(FileType.WEDDATACURRENT, FileAction.DOWNLOAD) {
            createCurrentObservationsDocument.launch("observations_$currentDateTime.csv")
        }
        wedCheckViewModel.setFileHandler(FileType.WEDDATAFULL, FileAction.DOWNLOAD) {
            createFullObservationsDocument.launch("all_observations_$currentDateTime.csv")
        }
    }

    Scaffold { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier
                    .padding(10.dp)
            ) {
                items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                contentDescription = item,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                softWrap = true,
                                maxLines = 2,
                                modifier = Modifier.width(100.dp)
                            )
                        },
                        selected = selectedItem == index,
                        onClick = {
                            if (index == 0) {
                                navController.navigate(Screens.HomeScreen.route)
                            } else {
                                selectedItem = index
                            }
                        },
                        modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                    )
                }
            }

            // Main Content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {

                // Seal Pup Image
                Image(
                    painter = painterResource(R.drawable.pup1_2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.5f
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(10.dp)
                ) {
                    when (selectedItem) {
                        1 -> DashboardScreen(adminViewModel)

                        2 -> UploadDataFileScreen(
                            wedCheckViewModel,
                            sealColoniesViewModel,
                            observersViewModel
                        )

                        3 -> ExportObservations(wedCheckViewModel)
                    }
                }
            }
        }
    }
}