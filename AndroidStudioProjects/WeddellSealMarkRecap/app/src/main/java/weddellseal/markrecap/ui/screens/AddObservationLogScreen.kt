package weddellseal.markrecap.ui.screens

/*
 * Main screen for entering seal data
 */

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.RemoveDialog
import weddellseal.markrecap.ui.components.SealCard
import weddellseal.markrecap.ui.components.SealInvalidDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObservationLogScreen(
    navController: NavHostController,
    viewModel: AddObservationLogViewModel,
    wedCheckViewModel: WedCheckViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // vars not "by remember" because the screen needs to respond to input that changes the seal's model values
    var primarySeal = viewModel.primarySeal
    var pupOne = viewModel.pupOne
    var pupTwo = viewModel.pupTwo
    var saveEnabled by remember { mutableStateOf(false) }
    var showConfirmEntryDialog by remember { mutableStateOf(false) }
    var coordinates by remember { mutableStateOf(viewModel.uiState.currentLocation) }
    var colony by remember { mutableStateOf(viewModel.uiState.colonyLocation) }
    var census by remember { mutableStateOf(viewModel.uiState.censusNumber) }
    var isObservationMode by remember { mutableStateOf(true) }

    // Register ActivityResult to request Location permissions
    val requestLocationPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onPermissionChange(ACCESS_FINE_LOCATION, isGranted)
                viewModel.fetchCurrentLocation()
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
                }
            }
        }

    // Add explanation dialog for Location permissions
    var showExplanationDialogForLocationPermission by remember { mutableStateOf(false) }
    if (showExplanationDialogForLocationPermission) {
        LocationExplanationDialog(
            onConfirm = {
                requestLocationPermissions.launch(ACCESS_FINE_LOCATION)
                showExplanationDialogForLocationPermission = false
                viewModel.fetchCurrentLocation()
            },
            onDismiss = { showExplanationDialogForLocationPermission = false },
        )
    }

    // method called on the initial load of the ObservationLog Screen
    // if permissions are in place it gathers information about the
    // current and last known locations to populate location fields
    fun canAddLocation() {
        if (viewModel.hasPermission(ACCESS_FINE_LOCATION)) {
            viewModel.fetchCurrentLocation()
        } else {
            showExplanationDialogForLocationPermission = true
        }
    }

    // this should be current for every observation
    LaunchedEffect(Unit) {
        // preload the model with location data
        canAddLocation()
    }

    LaunchedEffect(viewModel.uiState.currentLocation) {
        coordinates = viewModel.uiState.currentLocation
    }

    LaunchedEffect(viewModel.uiState.colonyLocation) {
        colony = viewModel.uiState.colonyLocation
    }

    LaunchedEffect(viewModel.uiState.censusNumber) {
        census = viewModel.uiState.censusNumber
        if (census != "Select an option") {
            isObservationMode = false
        }
    }

    // this should be activated after the saveAction() is triggered
    LaunchedEffect(viewModel.uiState.isValidated) {
        var showDialog = false
        if (viewModel.uiState.isValidated && !viewModel.uiState.validEntry) {
            showDialog = true
        }
        showConfirmEntryDialog = showDialog
    }

    LaunchedEffect(viewModel.uiState.isSaved) {
        if (viewModel.uiState.isSaved) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Entry successfully saved!")
            }
            viewModel.resetSaved()
        }
    }

    fun checkSaveEnabled(): Boolean {
        // High-level checks
        if (!isObservationMode && census == "Select an option") return false
        if (viewModel.uiState.observerInitials == "Select an option") return false
        if (viewModel.uiState.colonyLocation == "Select an option") return false

        // Seal checks
        if (primarySeal.isStarted) {
            if (primarySeal.age.isEmpty()) {
                return false
            } else if (primarySeal.age == "Pup" && primarySeal.condition.isEmpty()) {
                return false
            }
            if (primarySeal.age.isEmpty() ||
                primarySeal.sex.isEmpty() ||
                primarySeal.numRelatives.isEmpty() ||
                primarySeal.tagEventType.isEmpty()
            ) return false
        }

        if (pupOne.isStarted) {
            if (pupOne.age.isEmpty() ||
                pupOne.sex.isEmpty() ||
                pupOne.numRelatives.isEmpty() ||
                pupOne.tagEventType.isEmpty() ||
                pupOne.condition.isEmpty()
            ) return false
        }

        if (pupTwo.isStarted) {
            if (pupTwo.age.isEmpty() ||
                pupTwo.sex.isEmpty() ||
                pupTwo.numRelatives.isEmpty() ||
                pupTwo.tagEventType.isEmpty() ||
                pupTwo.condition.isEmpty()
            ) return false
        }

        // All conditions passed
        return true
    }

    LaunchedEffect(
        viewModel.uiState.observerInitials,
        viewModel.uiState.colonyLocation,
        primarySeal.age,
        primarySeal.sex,
        primarySeal.numRelatives,
        primarySeal.tagEventType,
        primarySeal.condition,
        pupOne.age,
        pupOne.sex,
        pupOne.numRelatives,
        pupOne.tagEventType,
        pupOne.condition,
        pupTwo.age,
        pupTwo.sex,
        pupTwo.numRelatives,
        pupTwo.tagEventType,
        pupTwo.condition
    ) {
        saveEnabled = checkSaveEnabled()
    }

    // validates the seal against the wedcheck seal if present
    fun saveAction() {

        // TODO, update the location

        if (primarySeal.isStarted) {
            viewModel.validate(primarySeal)
        }

        if (pupOne.isStarted) {
            viewModel.validate(pupOne)
        }

        if (pupTwo.isStarted) {
            viewModel.validate(pupTwo)
        }

        if (viewModel.uiState.isValidated && viewModel.uiState.validEntry) {
            viewModel.createLog(primarySeal, pupOne, pupTwo)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(.9f),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isObservationMode) "Observation" else "Census #$census",
                                    fontSize = 36.sp // Adjust this value as needed
                                )
                            }
                        }
                        // TOGGLE MODE
                        if (!isObservationMode) {
                            Box(
                                modifier = Modifier
                                    .weight(.4f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Exit Census",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(end = 10.dp)
                                    )

                                    Switch(
                                        checked = isObservationMode,
                                        onCheckedChange = { isObservationMode = it }
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screens.HomeScreen.route) }) {
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
                    val contentColor =
                        if (primarySeal.isStarted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = ContentAlpha.disabled
                        )
                    IconButton(
                        onClick = {
                            saveAction()
                        },
                        enabled = saveEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save Seal Entry",
                            modifier = Modifier.size(48.dp), // Change the size here
                            tint = if (saveEnabled) contentColor else Color.Gray // Adjust tint based on saveEnabled
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // METADATA SECTION

            // GPS Location & Updated Timestamp
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box() {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewModel.uiState.hasGPS && coordinates != "") {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF1D9C06),
                                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                                        .size(48.dp), // Change the size here

                            )
                        } else {
                            Icon(
                                Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.padding(end = 10.dp)
                                    .size(48.dp), // Change the size here
                            )
                        }
                        Text(
                            text = coordinates,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            // Colony Location
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box() {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Colony:  $colony",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            // CENSUS METADATA
            if (viewModel.uiState.censusNumber != "Select an option" && !isObservationMode) {
                // Prepopulate Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box() {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExtendedFloatingActionButton(
                                icon = {
                                    Icon(
                                        Icons.Filled.BabyChangingStation,
                                        "Mom & Pup",
                                        Modifier.size(36.dp)
                                    )
                                },
                                text = { Text("Mom & Pup") },
                                onClick = { /* Your action here */ }
                            )
                            ExtendedFloatingActionButton(
                                icon = {
                                    Icon(
                                        Icons.Filled.Female,
                                        "Single Female",
                                        Modifier.size(36.dp)
                                    )
                                },
                                text = { Text("Single Female") },
                                onClick = { /* Your action here */ }
                            )
                            ExtendedFloatingActionButton(
                                icon = {
                                    Icon(
                                        Icons.Filled.Male,
                                        "Single Male",
                                        Modifier.size(36.dp)
                                    )
                                },
                                text = { Text("Single Male") },
                                onClick = { /* Your action here */ }
                            )
                        }
                    }
                }

            }

            // SEAL CARDS
            TabbedCards(viewModel, wedCheckViewModel)
        }
        // because this action results in removing any entered data
        // Show the dialog if showDialog is true
        if (showConfirmEntryDialog) {
            SealInvalidDialog(
                viewModel,
                onDismissRequest = {
                    showConfirmEntryDialog = false
                    viewModel.clearValidationState()
                },
                onConfirmation = {
                    // flag seals for review
                    if (!primarySeal.isValid) {
                        viewModel.flagSealForReview(primarySeal.name)
                    }
                    if (!pupOne.isValid) {
                        viewModel.flagSealForReview(pupOne.name)
                    }
                    if (!pupTwo.isValid) {
                        viewModel.flagSealForReview(pupTwo.name)
                    }
                    showConfirmEntryDialog = false

                    viewModel.createLog(primarySeal, pupOne, pupTwo)
                },
            )
        }
    }
}

@Composable
fun LocationExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location access") },
        text = { Text("Weddell Seal Mark Recap app would like access to your location to save it when creating a log") },
        icon = {
            Icon(
                Icons.Filled.Explore,
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

data class TabItem(
    val title: String,
    val sealName: String,
    val content: @Composable () -> Unit
)

@Composable
fun TabbedCards(
    viewModel: AddObservationLogViewModel,
    wedCheckViewModel: WedCheckViewModel
) {
    val scrollState = rememberScrollState()
    val showDeleteDialog = remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var tabItems by remember {
        mutableStateOf(
            createTabItems(viewModel, wedCheckViewModel)
        )
    }

    // Render the tabs list based on changes with number of relatives or pups started
    LaunchedEffect(
        viewModel.primarySeal.numRelatives,
        viewModel.pupOne.isStarted,
        viewModel.pupTwo.isStarted
    ) {
        tabItems = createTabItems(viewModel, wedCheckViewModel)

        // Ensure selectedTabIndex is within bounds after updating the list
        if (selectedTabIndex >= tabItems.size) {
            selectedTabIndex = tabItems.lastIndex.coerceAtLeast(0)
        }
    }

    Column {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabItems.forEachIndexed { index, tabItem ->
                Tab(
                    text = {
                        Text(
                            tabItem.title, style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(horizontal = 20.dp) // Adjust padding here
                        )
                    },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(state = scrollState, enabled = true),
            colors = CardColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color.White,
                disabledContentColor = Color.Gray
            ),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Gray,
                        Color.Gray
                    ) // Specify your gradient colors here
                )
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
//                    .border(2.dp, Color.Red) // Adding a red outline for debugging
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            showDeleteDialog.value = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Tab",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (tabItems.isNotEmpty()) {
                        tabItems[selectedTabIndex].content()
                    }
                }

                // Show the dialog if showDialog is true
                if (showDeleteDialog.value) {
                    RemoveDialog(
                        onDismissRequest = { showDeleteDialog.value = false },
                        onConfirmation = {
                            if (tabItems.isNotEmpty()) {
                                // remove the current seal
                                viewModel.resetSeal(tabItems[selectedTabIndex].sealName)
                                showDeleteDialog.value = false
                            }
                        },
                    )
                }
            }
        }
    }
}

fun createTabItems(
    viewModel: AddObservationLogViewModel,
    wedCheckViewModel: WedCheckViewModel
): List<TabItem> {
    val items = mutableListOf<TabItem>()
    items.add(TabItem("Seal", viewModel.primarySeal.name) {
        SealCard(
            viewModel,
            viewModel.primarySeal,
            wedCheckViewModel
        )
    })

    if (viewModel.pupOne.isStarted) {
        items.add(TabItem("Pup One", viewModel.pupOne.name) {
            SealCard(
                viewModel,
                viewModel.pupOne,
                wedCheckViewModel
            )
        })
    }

    if (viewModel.pupTwo.isStarted) {
        items.add(TabItem("Pup Two", viewModel.pupTwo.name) {
            SealCard(
                viewModel,
                viewModel.pupTwo,
                wedCheckViewModel
            )
        })
    }

    return items
}
