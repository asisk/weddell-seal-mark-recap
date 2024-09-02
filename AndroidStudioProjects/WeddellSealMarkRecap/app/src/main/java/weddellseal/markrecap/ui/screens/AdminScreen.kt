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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
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
    val scrollState = rememberScrollState()
    val uiStateWedCheck by wedCheckViewModel.uiState.collectAsState()
    val uiStateHome by homeViewModel.uiState.collectAsState()

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
                showExplanationDialogForFileMatchError = false
            },
            onDismiss = { showExplanationDialogForFileMatchError = false },
            title = "Error",
            text = "File name does not match! Please rename your file and try again!"
        )
    }

    //TODO, how to handle partial versus whole update (WedCheck.csv versus WedCheckFull.csv)
    val getWedCheckCSV = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        // Handle the selected locations file here
        if (uri != null) {
            var fileName = ""

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            if (fileName == "WedCheckFull04Aug2024.csv") {
                //TODO, add validation to ensure that the file is right-sized
//                private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB (adjust as needed)
//
//                private fun isFileSizeWithinLimit(file: File): Boolean {
//                    val fileSize = file.length()
//                    return fileSize <= MAX_FILE_SIZE_BYTES
//                }
                wedCheckViewModel.updateLastFileNameLoaded(fileName)
                wedCheckViewModel.loadWedCheck(uri)

                if (wedCheckViewModel.uiState.value.isWedCheckLoaded) {
                    // Display a success message
                    Toast.makeText(
                        context,
                        "WedCheck file loaded successfully!",
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

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            if (fileName == "Colony_Locations.csv") {
                homeViewModel.updateLastColoniesFileNameLoaded(fileName)
                homeViewModel.loadSealColoniesFile(uri)

                if (homeViewModel.uiState.value.isColonyLocationsLoaded) {
                    // Display a success message
                    Toast.makeText(
                        context,
                        "Colonies file loaded successfully!",
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

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }

            if (fileName == "observers.csv") {
                homeViewModel.updateLastObserversFileNameLoaded(fileName)
                homeViewModel.loadObserversFile(uri)

                if (homeViewModel.uiState.value.isObserversLoaded) {
                    // Display a success message
                    Toast.makeText(
                        context,
                        "Observers file loaded successfully!",
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
                            style = MaterialTheme.typography.titleLarge
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
//                    IconButton(onClick = { navController.navigate(Screens.HomeScreen.route) }) {
//                        Icon(
//                            imageVector = Icons.Filled.Home,
//                            contentDescription = "Home",
//                            modifier = Modifier.size(48.dp)
//                        )
//                    }
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
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(state = scrollState, enabled = true)
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = {
                                getWedCheckCSV.launch("text/csv")
                            },
                            icon = {
                                Icon(
                                    Icons.Filled.Upload,
                                    "Upload WedCheck File",
                                    Modifier.size(36.dp)
                                )
                            },
                            text = {
                                Text(
                                    "Upload WedCheck File",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            })
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { getObserversCSV.launch("text/csv") },
                            icon = {
                                Icon(
                                    Icons.Filled.Upload,
                                    "Upload Observer Initials",
                                    Modifier.size(36.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Upload Observer Initials",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { homeViewModel.clearObservers() },
                            icon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    "Clear Observers Dropdown",
                                    Modifier.size(36.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Clear Observers Dropdown",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { getColonyLocationsCSV.launch("text/csv") },
                            icon = {
                                Icon(
                                    Icons.Filled.Upload,
                                    "Upload Seal Colony Locations",
                                    Modifier.size(36.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Upload Seal Colony Locations",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }

                    ExtendedFloatingActionButton(modifier = Modifier.padding(16.dp),
                        containerColor = Color.LightGray,
                        onClick = { homeViewModel.clearColonies() },
                        icon = {
                            Icon(
                                Icons.Filled.Delete,
                                "Clear Colonies Dropdown",
                                Modifier.size(36.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "Clear Colonies Dropdown",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )


                    FileLoadingView(
                        isLoading = uiStateWedCheck.isWedCheckLoading,
                        isLoaded = uiStateWedCheck.isWedCheckLoaded,
                        totalRows = uiStateWedCheck.totalRows,
                        failedRows = uiStateWedCheck.failedRows,
                        fileName = uiStateWedCheck.lastWedCheckFileLoaded
                    )

                    FileLoadingView(
                        isLoading = uiStateHome.loading,
                        isLoaded = uiStateHome.isColonyLocationsLoaded,
                        totalRows = uiStateHome.totalColoniesRows,
                        failedRows = uiStateHome.failedColoniesRows,
                        fileName = uiStateHome.lastColoniesFileNameLoaded
                    )

                    FileLoadingView(
                        isLoading = uiStateHome.loading,
                        isLoaded = uiStateHome.isObserversLoaded,
                        totalRows = uiStateHome.totalObserversRows,
                        failedRows = uiStateHome.failedObserversRows,
                        fileName = uiStateHome.lastObserversFileNameLoaded
                    )
                }
            }
            if (recentObservationsViewModel.uiState.isError) {
                Row(
                    modifier = Modifier
                        .padding(6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Error during CSV export!")
                }
            }
        }
    }
}

@Composable
fun FileLoadingView(
    isLoading: Boolean,
    isLoaded: Boolean,
    totalRows: Int,
    failedRows: List<String>,
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FailedRowsDisplay(failedRows)
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
fun FailedRowsDisplay(failedRows: List<String>) {
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

            failedRows.forEachIndexed { index, row ->
                Text(
                    text = "Row ${index + 1}: $row",
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