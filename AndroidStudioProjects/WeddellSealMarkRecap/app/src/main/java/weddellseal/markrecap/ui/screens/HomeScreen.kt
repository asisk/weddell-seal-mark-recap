package weddellseal.markrecap.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build.getSerial
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.models.HomeViewModelFactory
import weddellseal.markrecap.ui.components.DropdownField
import weddellseal.markrecap.ui.components.TextFieldValidateOnCharCount

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {
    HomeScaffold(navController, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScaffold(navController: NavHostController, viewModel: HomeViewModel) {
    val context = LocalContext.current
    val state = viewModel.uiState
//    var isPermissionGranted by remember { mutableStateOf(false) }

    // Register ActivityResult to request Location permissions
    val requestFilePermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onPermissionChange(Manifest.permission.READ_EXTERNAL_STORAGE, isGranted)
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

//    var colonyLocations by remember {
//        mutableStateOf(
//            listOf(
//                "Default Location A",
//                "Default Location B"
//            )
//        )
//    }

    val pickCsvFile = rememberLauncherForActivityResult(
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

            if (fileName == "Colony_Locations.csv") {

                //TODO, add validation to ensure that the file is right-sized
//                private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB (adjust as needed)
//
//                private fun isFileSizeWithinLimit(file: File): Boolean {
//                    val fileSize = file.length()
//                    return fileSize <= MAX_FILE_SIZE_BYTES
//                }

                viewModel.loadStudyAreaFile(uri)

//                val csvData = readCsvData(context.contentResolver, uri)
//                // Update dropdown with dropdownValues
//                colonyLocations = extractDropdownValues(csvData)
            } else {
                // Show an error message indicating that the selected file is not the expected file
                showExplanationDialogForFileMatchError = true
            }

        }
    }

    Scaffold(
        // region UI - Top Bar & Action Button
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
                                .padding(14.dp),
                            text = "Weddell Seal Mark Recap",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                BottomNavigation(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) {
//                    BottomNavigationItem(
//                        label = { Text(text = "Start Observation") },
//                        selected = false,
//                        onClick = { navController.navigate(Screens.AddObservationLog.route) },
//                        icon = { Icon(Icons.Filled.PostAdd, "Start Observation") }
//                    )
                    BottomNavigationItem(
                        label = { Text(text = "Recent Observations") },
                        selected = false,
                        onClick = { navController.navigate(Screens.RecentObservations.route) },
                        icon = { Icon(Icons.Filled.Dataset, null) }
                    )
                    BottomNavigationItem(
                        label = { Text(text = "Admin") },
                        selected = false,
                        onClick = { navController.navigate(Screens.AdminScreen.route) },
                        icon = { Icon(Icons.Filled.Person, null) }
                    )
//                    val contentColor =
//                        MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
//                    BottomNavigationItem(
//                        label = { Text(text = "Start Census", color = contentColor) },
//                        selected = false,
//                        onClick = { /*TODO */ },
//                        icon = { Icon(Icons.Filled.PostAdd, "Start Census", tint = contentColor) }
//                    )
//                    BottomNavigationItem(
//                        label = { Text(text = "Upload Locations") },
//                        selected = false,
//                        onClick = { if (!isPermissionGranted) {
//                            showExplanationDialogForReadAccessPermission = true
//                        } else {
//                            // Launch file picker
//                            launcher.launch("text/csv") // Mime type for plain text files, change as per requirement
//                        } },
//                        icon = { Icon(Icons.Filled.UploadFile, "Upload Locations") }
//                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .scrollable(rememberScrollState(), Orientation.Vertical)
                .fillMaxWidth(),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seal Pup Image
            Row {
                val image = painterResource(R.drawable.pup1_2)
                Image(painter = image, contentDescription = null)
            }
            Card(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(.7f)
                    .align(Alignment.CenterHorizontally)
            ) {
                // OBSERVER
                Row(
                    modifier = Modifier
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var observerInitials by remember { mutableStateOf("") }
                    TextFieldValidateOnCharCount(
                        charNumber = 3,
                        fieldLabel = "Observer Initials",
                        placeHolderTxt = "",
                        leadIcon = null,
                        onChangeDo = { newText ->
                            observerInitials = newText
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var observationSiteSelected by remember { mutableStateOf("") }
                    //TODO, read the locations from a CSV
                    // Function to update dropdown values from CSV
                    Column(modifier = Modifier.padding(4.dp).fillMaxWidth(.3f)) {
                        Text(text = "Location")
                    }

                    Column(modifier = Modifier.padding(4.dp).fillMaxWidth(.5f)) {
                        DropdownField(state.colonyLocations) { valueSelected ->
                            observationSiteSelected = valueSelected
                        }
                    }

                    // Spacer to push the IconButton to the right
                    Spacer(modifier = Modifier.weight(1f))

                    Column(modifier = Modifier.padding(4.dp)) {
                        IconButton(
                            onClick = {
                            // Show file picker to select CSV file
                            pickCsvFile.launch("text/csv") // Mime type for plain text files, change as per requirement
                            },
                            modifier = Modifier.size(48.dp) // Adjust size as needed
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Upload CSV"
                            )
                        }
                    }

                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val censusOptions = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8")
                    var selection = "0"
                    Column(modifier = Modifier.padding(4.dp).fillMaxWidth(.3f)) {
                        Text(text = "Census #")
                    }
                    Column(modifier = Modifier.padding(4.dp).fillMaxWidth(.5f)) {
                        DropdownField(censusOptions) { newText ->
                            selection = newText
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //TODO, pull a system field and use it in place of This
//                    SerialNumberDisplay()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color.LightGray,
                    onClick = { navController.navigate(Screens.SealLookupScreen.route) },
                    icon = { Icon(Icons.Filled.Search, "Search for SpeNo") },
                    text = { Text(text = "Seal Lookup") }
                )
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color.LightGray,
                    onClick = { navController.navigate(Screens.AddObservationLog.route)},
                    icon = { Icon(Icons.Filled.PostAdd, "Enter a new observation") },
                    text = { Text(text = "Tag/Retag") }
                )
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color.LightGray,
                    onClick = {},// navController.navigate(Screens.SealLookupScreen.route) },
                    icon = { Icon(Icons.Filled.Checklist, "Enter a new observation") },
                    text = { Text(text = "Census") }
                )
            }
        }
    }
}

data class LocationInfo(
    val location: String,
    val latitude: Double,
    val longitude: Double
)

//private fun readCsvData(contentResolver: ContentResolver, uri: Uri): List<LocationInfo> {
//    val locationInfoList = mutableListOf<LocationInfo>()
//
//    contentResolver.openInputStream(uri)?.use { stream ->
//        InputStreamReader(stream).buffered().use { reader ->
//            val headerRow = reader.readLine()?.split(",") ?: emptyList()
//            val locationIndex = headerRow.indexOf("Location")
//            val latitudeIndex = headerRow.indexOf("Adj_Lat")
//            val longitudeIndex = headerRow.indexOf("Adj_Long")
//
//            reader.forEachLine { line ->
//                val row = line.split(",")
//                if (row.size >= 3 && locationIndex != -1 && latitudeIndex != -1 && longitudeIndex != -1) {
//                    val location = row.getOrNull(locationIndex)
//                    val latitude = row.getOrNull(latitudeIndex)?.toDoubleOrNull() ?: 0.0
//                    val longitude = row.getOrNull(longitudeIndex)?.toDoubleOrNull() ?: 0.0
//                    if (location != null) {
//                        val locationInfo = LocationInfo(location, latitude, longitude)
//                        locationInfoList.add(locationInfo)
//                    }
//                } else {
//                    // Handle invalid row or missing columns
//                }
//            }
//        }
//    }
//
//    return locationInfoList
//}
//
//private fun extractDropdownValues(locationInfoList: List<LocationInfo>): List<String> {
//    return locationInfoList.map { it.location }
//}

@Composable
fun FileAccessExplanationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    text: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        icon = {
            Icon(
                Icons.Filled.FileUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun SerialNumberDisplay() {
    val serialNumber = getSerial()

    Column {
        // Display the serial number in your Composable
        Text(text = "Serial Number: $serialNumber")
    }
}
//
//@Composable
//fun CardWithClickableImages() {
//    var clickedImage by remember { mutableStateOf(0) }
//
//    Card(
//        modifier = Modifier
//            .padding(16.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            verticalArrangement = Arrangement.Center
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 1 })
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 2 })
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 3 })
//            }
//
//            // Optionally, display some content based on the clickedImage value
//            when (clickedImage) {
//                1 -> Text("You clicked Image 1")
//                2 -> Text("You clicked Image 2")
//                3 -> Text("You clicked Image 3")
//            }
//        }
//    }
//}
//
//@Composable
//fun ClickableImage(imageResId: Int, onClick: () -> Unit) {
//    Image(
//        painter = painterResource(id = imageResId),
//        contentDescription = null, // Provide a proper content description
//        modifier = Modifier
//            .clickable { onClick() }
//            .padding(8.dp)
//    )
//}

//@PreviewParameter
//@Composable
//fun HomeModelProvider(): HomeModel {
//    // Provide a default instance of HomeModel for preview
//    val observationRepo: ObservationRepository
//    return HomeModel(Application(), observationRepo)
//}
//@Preview
//@Composable
//fun HomeScreenPreview(@PreviewParameter(HomeModelProvider::class) viewModel: HomeModel) {
//    val navController = rememberNavController()
//    HomeScreen(viewModel, navController)
//}



//@Preview
//@Composable
//fun ImageCard() {
//    WeddellSealMarkRecapTheme {
//    }
//}