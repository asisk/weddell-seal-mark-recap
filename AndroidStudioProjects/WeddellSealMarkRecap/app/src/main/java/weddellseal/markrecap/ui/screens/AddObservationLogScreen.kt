package weddellseal.markrecap.ui.screens

/*
 * Main screen for entering seal data
 */

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.PupCard
import weddellseal.markrecap.ui.components.SealCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObservationLogScreen(
    navController: NavHostController,
    viewModel: AddObservationLogViewModel,
    wedCheckViewModel: WedCheckViewModel
) {
    val state = viewModel.uiState
//    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var adultSeal = viewModel.primarySeal
    var pupOne = viewModel.pupOne
    var pupTwo = viewModel.pupTwo

    // Register ActivityResult to request Location permissions
    val requestLocationPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onPermissionChange(ACCESS_FINE_LOCATION, isGranted)
                viewModel.fetchCurrentLocation()
            } else {
                //coroutineScope.launch {
                //    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
                //}
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

    LaunchedEffect(Unit) {
        // preload the model with location data
        canAddLocation()
    }

    fun saveAction() {
        if (adultSeal.isStarted) {
            if (adultSeal.speNo == 0 ) {
                val speNo = wedCheckViewModel.findSealSpeNo(adultSeal.tagIdOne)
                viewModel.updateSpeNo(adultSeal.name, speNo)
            }

            viewModel.updateNotebookEntry(adultSeal)
            viewModel.updateNotebookEntry(pupOne)
            viewModel.updateNotebookEntry(pupTwo)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Observation",
                            fontSize = 36.sp // Adjust this value as needed
                        )
                    }
                },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Home"
                            )
                        }
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
                    val contentColor =
                        if (adultSeal.isStarted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = ContentAlpha.disabled
                        )
                    IconButton(
                        onClick = {
                            saveAction()
                            if (adultSeal.isStarted) {
                                navController.navigate(Screens.AddObservationSummary.route)
                            }
                        },
                        enabled = adultSeal.isStarted
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Review",
                            modifier = Modifier.size(48.dp), // Change the size here
                            tint = contentColor
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
            val tabItems = mutableListOf<TabItem>().apply {
                add(TabItem("Seal") { SealCard(viewModel, viewModel.primarySeal) })

                if (viewModel.primarySeal.numRelatives >= 1) {
                    add(TabItem("Pup One") { PupCard(viewModel, viewModel.pupOne) })
                }

                if (viewModel.primarySeal.numRelatives >= 2) {
                    add(TabItem("Pup Two") { PupCard(viewModel, viewModel.pupTwo) })
                }
            }
            TabbedCards(tabItems = tabItems)
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

data class TabItem(val title: String, val content: @Composable () -> Unit)

@Composable
fun TabbedCards(tabItems: List<TabItem>) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Column {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabItems.forEachIndexed { index, tabItem ->
                Tab(
                    text = { Text(tabItem.title, style = TextStyle(fontSize = 24.sp)) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(state = scrollState, enabled = true)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                tabItems[selectedTabIndex].content()
            }
        }
    }
}




