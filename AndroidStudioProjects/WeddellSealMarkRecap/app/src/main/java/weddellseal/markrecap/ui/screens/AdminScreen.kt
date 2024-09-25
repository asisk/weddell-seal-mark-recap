package weddellseal.markrecap.ui.screens

import android.Manifest
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
    val uiStateWedCheck by wedCheckViewModel.uiState.collectAsState()
    val uiStateHome by homeViewModel.uiState.collectAsState()
    val fileUploads by homeViewModel.fileUploads.collectAsState()
    // Track which file name failed
    var filenameStr by remember { mutableStateOf("") }

    val createDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
            // Handle the created document URI
            if (uri != null) {
                recentObservationsViewModel.updateURI(uri)
                recentObservationsViewModel.exportLogs(context)
            }
        }

    // Register ActivityResult to request read file permissions
    val requestFilePermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                wedCheckViewModel.onPermissionChange(
                    Manifest.permission.READ_EXTERNAL_STORAGE, isGranted
                )
//                viewModel.fetchCurrentLocation()
            } else {
                //coroutineScope.launch {
                //    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
                //}
            }
        }

    // Add explanation dialog for File permissions
    var showExplanationDialogForReadAccessPermission by remember { mutableStateOf(false) }
    if (showExplanationDialogForReadAccessPermission) {
        FileAccessExplanationDialog(
            onConfirm = {
                requestFilePermissions.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                showExplanationDialogForReadAccessPermission = false
//                isPermissionGranted = true
            },
            onDismiss = {
                showExplanationDialogForReadAccessPermission = false
//                isPermissionGranted = true
            },
            title = "File access",
            text = "Weddell Seal Mark Recap app would like access to your stored files",
        )
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
            text = "File name does not match expected value: $filenameStr! Please rename your file and try again!"
        )
    }

    //TODO, how to handle partial versus whole update (WedCheck.csv versus WedCheckFull.csv)
    val getWedCheckCSV = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        // Handle the selected locations file here
        if (uri != null) {
            var fileName = ""
            filenameStr = "WedCheckFull.csv or WedCheck.csv"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            // Support uploading the full wedcheck file
            if (fileName == "WedCheckFull.csv" || fileName == "WedCheck.csv") {
                //TODO, add validation to ensure that the file is right-sized
//                private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB (adjust as needed)
//
//                private fun isFileSizeWithinLimit(file: File): Boolean {
//                    val fileSize = file.length()
//                    return fileSize <= MAX_FILE_SIZE_BYTES
//                }
                wedCheckViewModel.updateLastFileNameLoaded(fileName)
                wedCheckViewModel.loadWedCheck(uri, fileName)

                if (wedCheckViewModel.uiState.value.isWedCheckLoaded) {
                    // Display a success message
                    Toast.makeText(
                        context,
                        "$fileName loaded successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                // Show an error message indicating that the selected file is not the expected file
                showExplanationDialogForFileMatchError = true
            }
        }
    }

    val getColonyLocationsCSV = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        // Handle the selected locations file here
        if (uri != null) {
            var fileName = ""
            filenameStr = "Colony_Locations.csv"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            if (fileName == "Colony_Locations.csv") {
                homeViewModel.updateLastColoniesFileNameLoaded(fileName)
                homeViewModel.loadSealColoniesFile(uri, fileName)

                if (homeViewModel.uiState.value.isColonyLocationsLoaded) {
                    // Display a success message
                    Toast.makeText(
                        context,
                        "$fileName loaded successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                // Show an error message indicating that the selected file is not the expected file
                showExplanationDialogForFileMatchError = true
            }
        }
    }

    val getObserversCSV = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        // Handle the selected locations file here
        if (uri != null) {
            var fileName = ""
            filenameStr = "observers.csv"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            if (fileName == "observers.csv") {
                homeViewModel.updateLastObserversFileNameLoaded(fileName)
                homeViewModel.loadObserversFile(uri, fileName)

                if (homeViewModel.uiState.value.isObserversLoaded) {
                    // Display a success message
                    Toast.makeText(
                        context,
                        "$fileName loaded successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                // Show an error message indicating that the selected file is not the expected file
                showExplanationDialogForFileMatchError = true
            }
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
                actions = {
                    IconButton(
                        onClick = {
                            val dateTimeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                            val currentDateTime = dateTimeFormat.format(Date())
                            val filename = "observations_$currentDateTime.csv"

                            createDocument.launch(filename)
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.export_notes),
                            null,
                            modifier = Modifier.size(36.dp),
                            colorFilter = ColorFilter.tint(Color.DarkGray) // Change the color here
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
//                .verticalScroll(state = scrollState, enabled = true)
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Seal Pup Image
                Image(painter = painterResource(R.drawable.pup1_2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.5f // Adjust this value for desired transparency
                        })
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // File Upload Table Viewer
                    FileUploadList(fileUploads)

                    // Card for WedCheck Upload
                    FileUploadCard(
                        title = "Upload WedCheck File",
                        onUpload = { getWedCheckCSV.launch("text/csv") }
                    )

                    // Card for Observers Upload and Clear
                    FileUploadCard(
                        title = "Upload Observer Initials",
                        onUpload = { getObserversCSV.launch("text/csv") },
                        onDelete = { homeViewModel.clearObservers() }
                    )

                    // Card for Colony Locations Upload and Clear
                    FileUploadCard(
                        title = "Upload Seal Colony Locations",
                        onUpload = { getColonyLocationsCSV.launch("text/csv") },
                        onDelete = { homeViewModel.clearColonies() }
                    )

                    // Card for Loaded Files
                    LoadedFilesCard(
                        wedCheckState = uiStateWedCheck,
                        homeViewModelState = uiStateHome,
                    )

                    // Handle Errors in CSV Export
                    if (recentObservationsViewModel.uiState.isError) {
                        ErrorText("Error during CSV export!")
                    }

//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 30.dp),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
//                            containerColor = Color.LightGray,
//                            onClick = {
//                                getWedCheckCSV.launch("text/csv")
//                            },
//                            icon = {
//                                Icon(
//                                    Icons.Filled.Upload,
//                                    "Upload WedCheck File",
//                                    Modifier.size(36.dp)
//                                )
//                            },
//                            text = {
//                                Text(
//                                    "Upload WedCheck File",
//                                    style = MaterialTheme.typography.titleLarge
//                                )
//                            })
//                    }
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 30.dp),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
//                            containerColor = Color.LightGray,
//                            onClick = { getObserversCSV.launch("text/csv") },
//                            icon = {
//                                Icon(
//                                    Icons.Filled.Upload,
//                                    "Upload Observer Initials",
//                                    Modifier.size(36.dp)
//                                )
//                            },
//                            text = {
//                                Text(
//                                    text = "Upload Observer Initials",
//                                    style = MaterialTheme.typography.titleLarge
//                                )
//                            }
//                        )
//                    }
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 30.dp),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
//                            containerColor = Color.LightGray,
//                            onClick = { homeViewModel.clearObservers() },
//                            icon = {
//                                Icon(
//                                    Icons.Filled.Delete,
//                                    "Clear Observers Dropdown",
//                                    Modifier.size(36.dp)
//                                )
//                            },
//                            text = {
//                                Text(
//                                    text = "Clear Observers Dropdown",
//                                    style = MaterialTheme.typography.titleLarge
//                                )
//                            }
//                        )
//                    }
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 30.dp),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
//                            containerColor = Color.LightGray,
//                            onClick = { getColonyLocationsCSV.launch("text/csv") },
//                            icon = {
//                                Icon(
//                                    Icons.Filled.Upload,
//                                    "Upload Seal Colony Locations",
//                                    Modifier.size(36.dp)
//                                )
//                            },
//                            text = {
//                                Text(
//                                    text = "Upload Seal Colony Locations",
//                                    style = MaterialTheme.typography.titleLarge
//                                )
//                            }
//                        )
//                    }
//
//                    ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
//                        containerColor = Color.LightGray,
//                        onClick = { homeViewModel.clearColonies() },
//                        icon = {
//                            Icon(
//                                Icons.Filled.Delete,
//                                "Clear Colonies Dropdown",
//                                Modifier.size(36.dp)
//                            )
//                        },
//                        text = {
//                            Text(
//                                text = "Clear Colonies Dropdown",
//                                style = MaterialTheme.typography.titleLarge
//                            )
//                        }
//                    )
//
//                    FileLoadingView(
//                        isLoading = uiStateWedCheck.isWedCheckLoading,
//                        isLoaded = uiStateWedCheck.isWedCheckLoaded,
//                        totalRows = uiStateWedCheck.totalRows,
//                        failedRows = uiStateWedCheck.failedRows,
//                        fileName = uiStateWedCheck.lastWedCheckFileLoaded
//                    )
//
//                    FileLoadingView(
//                        isLoading = uiStateHome.loading,
//                        isLoaded = uiStateHome.isColonyLocationsLoaded,
//                        totalRows = uiStateHome.totalColoniesRows,
//                        failedRows = uiStateHome.failedColoniesRows,
//                        fileName = uiStateHome.lastColoniesFileNameLoaded
//                    )
//
//                    FileLoadingView(
//                        isLoading = uiStateHome.loading,
//                        isLoaded = uiStateHome.isObserversLoaded,
//                        totalRows = uiStateHome.totalObserversRows,
//                        failedRows = uiStateHome.failedObserversRows,
//                        fileName = uiStateHome.lastObserversFileNameLoaded
//                    )
//                }
//            }
//            if (recentObservationsViewModel.uiState.isError) {
//                Row(
//                    modifier = Modifier
//                        .padding(6.dp)
//                        .fillMaxWidth(),
//                    horizontalArrangement = Arrangement.Center,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text("Error during CSV export!")
//                }
//            }
                }
            }
        }
    }
}

@Composable
fun FileUploadCard(
    title: String,
    onUpload: () -> Unit,
    onDelete: (() -> Unit)? = null // Optional delete action
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Use CardDefaults for elevation
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
    }
}

@Composable
fun LoadedFilesCard(
    wedCheckState: WedCheckViewModel.UiState,
    homeViewModelState: HomeViewModel.UiState,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Use CardDefaults for elevation
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Loaded Files", style = MaterialTheme.typography.headlineLarge)

            // WedCheck File Status
            FileLoadingView(
                isLoading = wedCheckState.isWedCheckLoading,
                isLoaded = wedCheckState.isWedCheckLoaded,
                totalRows = wedCheckState.totalRows,
                failedRows = wedCheckState.failedRows,
                fileName = wedCheckState.lastWedCheckFileLoaded
            )

            // Observer File Status
            FileLoadingView(
                isLoading = homeViewModelState.isObserversLoading,
                isLoaded = homeViewModelState.isObserversLoaded,
                totalRows = homeViewModelState.totalObserversRows,
                failedRows = homeViewModelState.failedObserversRows,
                fileName = homeViewModelState.lastObserversFileNameLoaded
            )

            // Colony File Status
            FileLoadingView(
                isLoading = homeViewModelState.isColonyLocationsLoading,
                isLoaded = homeViewModelState.isColonyLocationsLoaded,
                totalRows = homeViewModelState.totalColoniesRows,
                failedRows = homeViewModelState.failedColoniesRows,
                fileName = homeViewModelState.lastColoniesFileNameLoaded
            )
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(fileUploads) { file ->
                FileUploadItem(fileUpload = file)
            }
        }
    }
}

@Composable
fun FileUploadItem(fileUpload: FileUploadEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {

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
    isLoading: Boolean,
    isLoaded: Boolean,
    totalRows: Int,
    failedRows: List<FailedRow>,
    fileName: String,
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (isLoaded) {

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
                if (failedRows.isNotEmpty()) {
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
                            Text("Failed Rows: ${failedRows.joinToString(", ")}")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 15.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Successfully Loaded Rows: ${totalRows - failedRows.size}")
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