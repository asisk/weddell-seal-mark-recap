package weddellseal.markrecap.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.data.FailedRow
import weddellseal.markrecap.data.FileUploadEntity
import weddellseal.markrecap.data.enums.UploadFileType
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.FileUploadErrorExplanationDialog
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
    val fileUploads by homeViewModel.fileUploads.collectAsState()

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
//            Icons.Outlined.History
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

            if (fileName == "WedCheckFull.csv" || fileName == "WedCheck.csv") {
                wedCheckViewModel.updateLastFileNameLoaded(fileName)
                homeViewModel.setLastFilename(UploadFileType.WEDCHECK, fileName)
                wedCheckViewModel.loadWedCheck(uri, fileName)
            } else if (fileName == expectedFileName) {
                when (expectedFileName) {
                    "observers.csv" -> {
                        homeViewModel.updateLastObserversFileNameLoaded(fileName)
                        homeViewModel.setLastFilename(UploadFileType.OBSERVERS, fileName)
                        homeViewModel.loadObserversFile(uri, fileName)
                    }

                    "Colony_Locations.csv" -> {
                        homeViewModel.updateLastColoniesFileNameLoaded(fileName)
                        homeViewModel.setLastFilename(UploadFileType.COLONIES, fileName)
                        homeViewModel.loadSealColoniesFile(uri, fileName)
                    }
                }
            } else {
                // Show an error message indicating that the selected file is not the expected file
                showExplanationDialogForFileMatchError = true
                Log.e("FileSelection", "File name does not match expected file")
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
        homeViewModel.setUploadHandler(UploadFileType.WEDCHECK) {
            wedCheckFilePicker.launch(arrayOf("*/*"))
        }
        homeViewModel.setUploadHandler(UploadFileType.OBSERVERS) {
            observersFilePicker.launch(arrayOf("*/*"))
        }
        homeViewModel.setUploadHandler(UploadFileType.COLONIES) {
            colonyFilePicker.launch(arrayOf("*/*"))
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

                    //Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(bottom = 20.dp),
                            text = "Administration",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 36.sp,
                        )
                    }

                    //Screens
                    when (selectedItem) {
                        1 -> DashboardScreen(
                            homeViewModel
                        )

                        2 -> UploadDataFileScreen(
                            homeViewModel,
                            fileUploads
                        )

                        3 -> ExportObservationsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun ExportObservationsScreen() {
    TODO("Not yet implemented")
}

@Composable
fun UploadDataFileScreen(
    homeViewModel: HomeViewModel,
    fileUploads: List<FileUploadEntity>
) {

    val uploadStates by homeViewModel.uploadStates.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UploadCard(state = uploadStates[UploadFileType.WEDCHECK]!!)
        UploadCard(state = uploadStates[UploadFileType.OBSERVERS]!!)
        UploadCard(state = uploadStates[UploadFileType.COLONIES]!!)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Last Uploaded", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(20.dp))
            FileUploadList(fileUploads)
        }
    }
}
// Upload Cards
//            FileUploadCard(
//                title = "WedCheck File",
//                onUpload = { wedCheckFilePicker.launch(arrayOf("*/*")) },
//                isLoaded = { wedCheckViewModel.uiState.value.isWedCheckLoaded },
//                isLoading = { wedCheckViewModel.uiState.value.isWedCheckLoading },
//                failedRows = { wedCheckViewModel.uiState.value.failedRows },
//                totalRows = { wedCheckViewModel.uiState.value.totalRows },
//                fileName = {
//                    wedCheckViewModel.uiState.value.lastWedCheckFileLoaded
//                        ?: "WedCheck.csv"
//                }
//            )
//            FileUploadCard(
//                title = "Observer Initials",
//                onUpload = { observersFilePicker.launch(arrayOf("*/*")) },
//                isLoaded = { homeViewModel.uiState.value.isObserversLoaded },
//                isLoading = { homeViewModel.uiState.value.isObserversLoading },
//                failedRows = { homeViewModel.uiState.value.failedObserversRows },
//                totalRows = { homeViewModel.uiState.value.totalObserversRows },
//                fileName = {
//                    homeViewModel.uiState.value.lastObserversFileNameLoaded
//                        ?: "observers.csv"
//                }
//            )
//            FileUploadCard(
//                title = "Seal Colony Locations",
//                onUpload = { colonyFilePicker.launch(arrayOf("*/*")) },
//                isLoaded = { homeViewModel.uiState.value.isColonyLocationsLoaded },
//                isLoading = { homeViewModel.uiState.value.isColonyLocationsLoading },
//                failedRows = { homeViewModel.uiState.value.failedColoniesRows },
//                totalRows = { homeViewModel.uiState.value.totalColoniesRows },
//                fileName = {
//                    homeViewModel.uiState.value.lastColoniesFileNameLoaded
//                        ?: "Colony_Locations.csv"
//                }
//            )


@Composable
fun FileUploadCard(
    title: String,
    onUpload: () -> Unit,
    onDelete: (() -> Unit)? = null, // Optional delete action
    isLoading: () -> Boolean,
    isLoaded: () -> Boolean,
    totalRows: () -> Int,
    failedRows: () -> List<FailedRow>,
    fileName: () -> String
) {
    Card(
        modifier = Modifier
//            .fillMaxWidth()
            .padding(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Use CardDefaults for elevation
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)

            // Upload Button
            ExtendedFloatingActionButton(
                onClick = onUpload,
                containerColor = Color.LightGray,
                icon = {
                    Icon(
                        Icons.Filled.Upload,
                        contentDescription = "Upload",
                        Modifier.size(36.dp)
                    )
                },
                text = { Text(text = "Upload", style = MaterialTheme.typography.bodyLarge) }
            )

            // Optional Delete Button
            onDelete?.let {
                Spacer(modifier = Modifier.height(16.dp))
                ExtendedFloatingActionButton(
                    onClick = onDelete,
                    containerColor = Color.Red,
                    icon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            Modifier.size(36.dp)
                        )
                    },
                    text = { Text(text = "Clear", style = MaterialTheme.typography.bodyLarge) }
                )
            }
        }

        // File Status
        Column(
            modifier = Modifier
                .padding(10.dp)
                .border(1.dp, Color.LightGray), // Add border for visual purposes

            horizontalAlignment = Alignment.End
        ) {
            FileLoadingView(
                isLoading,
                isLoaded,
                totalRows,
                failedRows,
                fileName.toString()
            )
        }
    }
}

@Composable
fun DashboardScreen(
    homeViewModel: HomeViewModel
) {
    LoadedFilesCard(homeViewModel.fileUploads.collectAsState().value)
}

@Composable
fun LoadedFilesCard(
    fileUploads: List<FileUploadEntity>,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Use CardDefaults for elevation
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Last Uploaded", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(20.dp))
            FileUploadList(fileUploads)
        }
    }
}

@Composable
fun ErrorText(message: String) {
    Row(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = Color.Red, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FileUploadList(fileUploads: List<FileUploadEntity>) {
    if (fileUploads.isEmpty()) {
        Text("No file uploads found.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = true
        ) {
            items(fileUploads) { file ->
                FileUploadItem(fileUpload = file)
            }
        }
    }
}

@Composable
fun FileUploadItem(fileUpload: FileUploadEntity) {
    // Row to display the observation and the three-dot menu
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 8.dp)
            )
            // filename
            Text(
                fileUpload.filename,
                style = MaterialTheme.typography.titleLarge,
            )

            // datetime of upload
            Text(
                "${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                    ).format(Date(fileUpload.createdAt))
                }",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }

        // Upload status
        val statusColor = when (fileUpload.status.lowercase()) {
            "successful" -> Color(0xFF0dbe0d)
            "error" -> Color(0xFFd90101)
            else -> Color(0xFF5884fa)
        }
        val statusIcon = when (fileUpload.status.lowercase()) {
            "successful" -> Icons.Default.CheckCircle
            "error" -> Icons.Default.ErrorOutline
            else -> Icons.Default.Pending
        }
        Text(
            fileUpload.status.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            modifier = Modifier.padding(top = 2.dp)
        )
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier
                .size(48.dp)
                .padding(start = 12.dp, end = 8.dp, top = 2.dp)
        )
    }
}

@Composable
fun FileLoadingView(
    isLoading: () -> Boolean,
    isLoaded: () -> Boolean,
    totalRows: () -> Int,
    failedRows: () -> List<FailedRow>,
    fileName: String,
) {
    if (isLoading()) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (isLoaded()) {

        Card(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(30.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CSV Loaded: $fileName")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 30.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Total Rows: $totalRows")
                }
                if (failedRows().isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 30.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Failed Rows: ${failedRows().joinToString(", ")}")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 15.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Successfully Loaded Rows: ${totalRows() - failedRows().size}")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("All rows loaded successfully.")
                    }
                }
            }
        }
    }
}

@Composable
fun FailedRowsDisplay(failedRows: List<FailedRow>) {
    if (failedRows.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Failed Rows",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )

            failedRows.forEach { row ->
                Text(
                    text = "Row ${row.rowNumber}: ${row.errorMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    } else {
        Text(
            text = "No failed rows found.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
