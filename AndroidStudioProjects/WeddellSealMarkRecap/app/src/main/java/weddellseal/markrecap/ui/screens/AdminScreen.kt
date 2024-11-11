package weddellseal.markrecap.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.data.FailedRow
import weddellseal.markrecap.data.FileUploadEntity
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.WedCheckViewModel
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
    var selectedDirectoryUri by remember { mutableStateOf<Uri?>(null) }

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
        FileAccessExplanationDialog(
            onConfirm = {
//                showExplanationDialogForFileMatchError = false
            },
            onDismiss = { showExplanationDialogForFileMatchError = false },
            title = "Error",
            text = "File name: $selectedFilename does not match expected value: $filenameStr! Please rename your file and try again!"
        )
    }

    // Function to handle the file selection logic
    fun handleFileSelection(uri: Uri?, expectedFileName: String) {
        if (uri != null) {
            var fileName = ""
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
//
//                private fun isFileSizeWithinLimit(file: File): Boolean {
//                    val fileSize = file.length()
//                    return fileSize <= MAX_FILE_SIZE_BYTES
//                }

            if (fileName == expectedFileName) {
                when (expectedFileName) {
                    "observers.csv" -> {
                        homeViewModel.updateLastObserversFileNameLoaded(fileName)
                        homeViewModel.loadObserversFile(uri, fileName)
                    }

                    "WedCheck.csv" -> {
                        wedCheckViewModel.updateLastFileNameLoaded(fileName)
                        wedCheckViewModel.loadWedCheck(uri, fileName)
                    }

                    "WedCheckFull.csv" -> {
                        wedCheckViewModel.updateLastFileNameLoaded(fileName)
                        wedCheckViewModel.loadWedCheck(uri, fileName)
                    }

                    "Colony_Locations.csv" -> {
                        homeViewModel.updateLastColoniesFileNameLoaded(fileName)
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

    val observersLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleFileSelection(uri, "observers.csv")
    }

    val wedCheckLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleFileSelection(uri, "WedCheck.csv")
    }

    val colonyLocationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handleFileSelection(uri, "Colony_Locations.csv")
    }

    fun handleDirectorySelection(uri: Uri) {
        val directory = DocumentFile.fromTreeUri(context, uri)

        if (directory != null && directory.isDirectory) {
            // Iterate through files in the directory
            val files = directory.listFiles()

            // Find the file you are interested in, e.g., "observers.csv"
            val targetFile = files.firstOrNull { it.name == "observers.csv" }

            if (targetFile != null && targetFile.isFile) {
                // Process the file, e.g., by opening an input stream
//                openFile(targetFile.uri)
                handleFileSelection(targetFile.uri, "observers.csv")
            } else {
                Log.e("FileSelection", "Expected file not found in directory")
                // Show an error message or handle the case where the file isn't found
            }
        } else {
            Log.e("FileSelection", "Selected URI is not a directory or is inaccessible")
        }
    }

    // Launcher for picking a file in a specific directory
    val observersDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Persist URI permissions for the selected directory
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Store the selected directory URI in ViewModel
            selectedDirectoryUri = uri

            handleDirectorySelection(uri)
        }
    }

    Scaffold(
        // region UI - Top Bar & Action Buttons
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(20.dp),
                            text = "Administration",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 36.sp // Adjust this value as needed
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screens.HomeScreen.route)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Seal Pup Image
                Image(painter = painterResource(R.drawable.pup1_2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.5f // Adjust this value for desired transparency
                        }
                )

                // File uploads
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()), // Use verticalScroll to scroll the entire content

                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp)
                    ) {

                        // Handle Errors in CSV Export
                        if (recentObservationsViewModel.uiState.isError) {
                            ErrorText("Error during CSV export!")
                            ErrorText(recentObservationsViewModel.uiState.errorMessage)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
//                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = {
                                val dateTimeFormat =
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                val currentDateTime = dateTimeFormat.format(Date())
                                val filename = "observations_$currentDateTime.csv"

                                createCurrentObservationsDocument.launch(filename)
                            },
                            icon = {
                                Image(
                                    painter = painterResource(id = R.drawable.export_notes),
                                    null,
                                    modifier = Modifier.size(36.dp),
                                    colorFilter = ColorFilter.tint(Color.DarkGray) // Change the color here
                                )
                            },
                            text = {
                                Text(
                                    "Export Current Observations",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
//                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { recentObservationsViewModel.markObservationsAsDeleted() },
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
//                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = {
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                val dateTimeFormat =
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                val currentDateTime = dateTimeFormat.format(Date())
                                val filename = "all_observations_$currentDateTime.csv"

                                createFullObservationsDocument.launch(filename)
                            },
                            icon = {
                                Image(
                                    painter = painterResource(id = R.drawable.export_notes),
                                    null,
                                    modifier = Modifier.size(36.dp),
                                    colorFilter = ColorFilter.tint(Color.DarkGray) // Change the color here
                                )
                            },
                            text = {
                                Text(
                                    "Export All Observations",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }


                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp)
                    ) {
                        // Card for WedCheck Upload
                        FileUploadCard(
                            title = "Upload WedCheck File",
                            onUpload = {
                                wedCheckLauncher.launch(arrayOf("text/csv")) //verify that permissions have been granted
                            },
                            isLoaded = { wedCheckViewModel.uiState.value.isWedCheckLoaded },
                            isLoading = { wedCheckViewModel.uiState.value.isWedCheckLoading },
                            failedRows = { wedCheckViewModel.uiState.value.failedRows },
                            totalRows = { wedCheckViewModel.uiState.value.totalRows },
                            fileName = { "WedCheck" },
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp)
                    ) {
                        // Card for Observers Upload and Clear
                        FileUploadCard(
                            title = "Upload Observer Initials",
                            onUpload = {
//                                observersLauncher.launch(arrayOf("text/csv"))
                                observersDirectoryPicker.launch(null)
                            },
//                                onDelete = { homeViewModel.clearObservers() },
                            isLoaded = { homeViewModel.uiState.value.isObserversLoaded },
                            isLoading = { homeViewModel.uiState.value.isObserversLoading },
                            failedRows = { homeViewModel.uiState.value.failedObserversRows },
                            totalRows = { homeViewModel.uiState.value.totalObserversRows },
                            fileName = { "Observers" },
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp)
                    ) {
                        // Card for Colony Locations Upload and Clear
                        FileUploadCard(
                            title = "Upload Seal Colony Locations",
                            onUpload = {
                                colonyLocationsLauncher.launch(arrayOf("text/csv"))
                            },
//                                onDelete = {   homeViewModel.clearColonies() },
                            isLoaded = { homeViewModel.uiState.value.isColonyLocationsLoaded },
                            isLoading = { homeViewModel.uiState.value.isColonyLocationsLoading },
                            failedRows = { homeViewModel.uiState.value.failedColoniesRows },
                            totalRows = { homeViewModel.uiState.value.totalColoniesRows },
                            fileName = { "Colonies" },
                        )
                    }

                    // RECENT OBSERVATIONS VIEW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recently \nUploaded",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp) // Limit the height
                                .padding(10.dp)
                                .border(1.dp, Color.LightGray) // Add border for visual purposes
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                userScrollEnabled = true
                            ) {
                                items(fileUploads) { file ->

                                    FileUploadItem(fileUpload = file)

                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


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
            .fillMaxWidth()
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
            Text(text = "Loaded Files", style = MaterialTheme.typography.headlineLarge)
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display the observation details
        Column {

            Text(
                "File Name: ${fileUpload.filename}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                " : ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                    ).format(Date(fileUpload.createdAt))
                }",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(" : ${fileUpload.status}", style = MaterialTheme.typography.bodyMedium)
        }
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