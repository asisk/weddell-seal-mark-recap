package weddellseal.markrecap.ui.screens

import android.Manifest
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Upload
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
import weddellseal.markrecap.models.WedCheckViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavHostController,
    wedCheckViewModel: WedCheckViewModel,
    homeViewModel: HomeViewModel
) {
    val context = LocalContext.current

    // Register ActivityResult to request read file permissions
    val requestFilePermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                wedCheckViewModel.onPermissionChange(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    isGranted
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
            text = "File name does not match! Please rename your file to Colony_Locations.csv and try again!"
        )
    }

    //TODO, how to handle partial versus whole update (WedCheck.csv versus WedCheckFull.csv)
    val getCSV = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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

            if (fileName == "AugustWedCheckTest.csv") {

                //TODO, add validation to ensure that the file is right-sized
//                private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB (adjust as needed)
//
//                private fun isFileSizeWithinLimit(file: File): Boolean {
//                    val fileSize = file.length()
//                    return fileSize <= MAX_FILE_SIZE_BYTES
//                }

                wedCheckViewModel.loadWedCheck(uri)
                Toast.makeText(context, "WedCheck file loaded successfully!", Toast.LENGTH_SHORT).show()
                // Display a success message & navigate the user back to the Home Screen

            } else if (fileName == "Colony_Locations.csv") {
                homeViewModel.loadSealColoniesFile(uri)
                Toast.makeText(context, "Seal Colony Locations file loaded successfully!", Toast.LENGTH_SHORT).show()

            } else if (fileName == "Observer_Initials.csv") {
                homeViewModel.loadObserversFile(uri)
                Toast.makeText(context, "Observer Initials file loaded successfully!", Toast.LENGTH_SHORT).show()

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
                        Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(20.dp),
                            text = "Administration",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screens.HomeScreen.route) }) {
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
                .scrollable(rememberScrollState(), Orientation.Vertical)
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Seal Pup Image
                Image(
                    painter = painterResource(R.drawable.pup1_2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.5f // Adjust this value for desired transparency
                        }
                )
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
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = {
                                getCSV.launch("text/csv")
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
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { getCSV.launch("text/csv") },
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
                        ExtendedFloatingActionButton(
                            modifier = Modifier.padding(16.dp),
                            containerColor = Color.LightGray,
                            onClick = { getCSV.launch("text/csv") },
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
                }
            }
        }
    }
}

@Composable
fun FileLoadedToast() {
    val context = LocalContext.current
    Toast.makeText(context, "File Loaded", Toast.LENGTH_LONG).show()
}
