package weddellseal.markrecap.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.ColorFilter
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
import weddellseal.markrecap.data.enums.FileAction
import weddellseal.markrecap.data.enums.FileType
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.models.HomeViewModel.FileStatus
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.DownloadCard
import weddellseal.markrecap.ui.components.FileUploadErrorExplanationDialog
import weddellseal.markrecap.ui.components.LastFilesUploadedCard
import weddellseal.markrecap.ui.components.UploadCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavHostController,
    wedCheckViewModel: WedCheckViewModel,
    homeViewModel: HomeViewModel,
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
                        homeViewModel.setLastFilename(FileType.WEDCHECK, fileName)
                        homeViewModel.updateFileStatus(
                            FileType.WEDCHECK,
                            FileStatus.Error("Failed to load file, unexpected file name: $fileName")
                        )
                        showExplanationDialogForFileMatchError = true
                        Log.e(
                            "FileSelection",
                            "Failed to load file, unexpected file name: $fileName"
                        )
                    } else {
                        wedCheckViewModel.updateLastFileNameLoaded(fileName)
                        homeViewModel.setLastFilename(FileType.WEDCHECK, fileName)
                        wedCheckViewModel.loadWedCheck(uri, fileName)
                    }
                }

                "observers.csv" -> {
                    if (expectedFileName != fileName) {
                        homeViewModel.setLastFilename(FileType.OBSERVERS, fileName)
                        homeViewModel.updateFileStatus(
                            FileType.OBSERVERS,
                            FileStatus.Error("Failed to load file, unexpected file name: $fileName")
                        )
                        showExplanationDialogForFileMatchError = true
                        Log.e(
                            "FileSelection",
                            "Failed to load file, unexpected file name: $fileName"
                        )
                    } else {
                        homeViewModel.updateLastObserversFileNameLoaded(fileName)
                        homeViewModel.setLastFilename(FileType.OBSERVERS, fileName)
                        homeViewModel.loadObserversFile(uri, fileName)
                    }
                }

                "Colony_Locations.csv" -> {
                    if (expectedFileName != fileName) {
                        homeViewModel.setLastFilename(FileType.COLONIES, fileName)
                        homeViewModel.updateFileStatus(
                            FileType.COLONIES,
                            FileStatus.Error("Failed to load file, unexpected file name: $fileName")
                        )
                        showExplanationDialogForFileMatchError = true
                        Log.e(
                            "FileSelection",
                            "Failed to load file, unexpected file name: $fileName"
                        )
                    } else {
                        homeViewModel.updateLastColoniesFileNameLoaded(fileName)
                        homeViewModel.setLastFilename(FileType.COLONIES, fileName)
                        homeViewModel.loadSealColoniesFile(uri, fileName)
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

        homeViewModel.setFileHandler(FileType.WEDCHECK, FileAction.UPLOAD) {
            wedCheckFilePicker.launch(arrayOf("*/*"))
        }
        homeViewModel.setFileHandler(FileType.OBSERVERS, FileAction.UPLOAD) {
            observersFilePicker.launch(arrayOf("*/*"))
        }
        homeViewModel.setFileHandler(FileType.COLONIES, FileAction.UPLOAD) {
            colonyFilePicker.launch(arrayOf("*/*"))
        }
        homeViewModel.setFileHandler(FileType.WEDDATACURRENT, FileAction.DOWNLOAD) {
            createCurrentObservationsDocument.launch("observations_$currentDateTime.csv")
        }
        homeViewModel.setFileHandler(FileType.WEDDATAFULL, FileAction.DOWNLOAD) {
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

//                    //Title
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        Text(
//                            modifier = Modifier.padding(bottom = 20.dp),
//                            text = "Administration",
//                            style = MaterialTheme.typography.headlineLarge,
//                            fontSize = 36.sp,
//                        )
//                    }

                    //Screens
                    when (selectedItem) {
                        1 -> DashboardScreen(homeViewModel)

                        2 -> UploadDataFileScreen(homeViewModel)

                        3 -> ExportObservationsScreen(homeViewModel)
                    }
                }
            }
        }
    }
}


@Composable
fun DashboardScreen(
    homeViewModel: HomeViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.padding(bottom = 20.dp),
            text = "Administration",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 36.sp,
        )
    }

    LastFilesUploadedCard(homeViewModel.fileUploads.collectAsState().value)
}

@Composable
fun UploadDataFileScreen(
    homeViewModel: HomeViewModel,
) {

    val filesStates by homeViewModel.fileStates.collectAsState()

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
        UploadCard(state = filesStates[FileType.WEDCHECK]!!)
        UploadCard(state = filesStates[FileType.OBSERVERS]!!)
        UploadCard(state = filesStates[FileType.COLONIES]!!)

    }
}

@Composable
fun ExportObservationsScreen(
    homeViewModel: HomeViewModel,
) {
    val filesStates by homeViewModel.fileStates.collectAsState()

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
                text = "Manage WedData",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 36.sp,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
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