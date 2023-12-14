package weddellseal.markrecap.ui

/*
 * Provides a view of the database records and an option to export to CSV
 * Updated when a new observation is saved
*/

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.models.HomeViewModelFactory
import weddellseal.markrecap.models.RecentObservationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentObservationsScreen(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel = viewModel(factory = HomeViewModelFactory())
) {
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.observationsFlow.collect {
            state.observations = it
        }
    }

    mainScaffold(navController, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScaffold(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel
) {
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
        // Handle the created document URI
        if (uri != null) {
            viewModel.updateURI(uri)
            viewModel.exportLogs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home", fontFamily = FontFamily.Serif) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(16.dp),
                onClick = {
                        createDocument.launch("observations.csv")
                          },
                icon = {Icon(Icons.Filled.Build, "Build CSV File")},
                text = {Text(text = "Build CSV File")}
            )
        },
        floatingActionButtonPosition = FabPosition.Start,
        bottomBar = {
            Text(
                text = "",
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            Row {
//                ExtendedFloatingActionButton(
//                    modifier = Modifier.padding(16.dp),
//                    onClick = { (navController.navigate(Screens.AddObservationLog.route)) },
//                    icon = { Icon(Icons.Filled.PostAdd, "Start Observation") },
//                    text = { Text(text = "Add Observation") })
//            }
            Text(text = "Observations Collected", modifier = Modifier.padding(15.dp), style = MaterialTheme.typography.headlineSmall)
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp)
                    .border(1.dp, Color.Black)
                    .clip(RoundedCornerShape(16.dp, 0.dp, 0.dp, 16.dp)) // 16dp for top-left and bottom-right corners
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (viewModel.uiState.observations.isEmpty()) {
                        populateObsView(viewModel)
                    }
                    items(viewModel.uiState.observations) { observation ->
                        Text(text = observation.toString(), modifier = Modifier.padding(8.dp,))
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
@Composable
fun EmptyLogMessage(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Hi there \uD83D\uDC4B",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Create a seal observation log by clicking the ✚ icon below \uD83D\uDC47",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun CreateDocumentScreen() {
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
        // Handle the created document URI
        if (uri != null) {
            // Do something with the created document URI
        }
    }

    fun saveFile(suggestedFileName: String) {
        createDocument.launch(suggestedFileName)
    }

    Column {
        Button(
            onClick = {
                createDocument.launch("observations.csv")
            }
        ) {
            Text("Create Document")
        }
    }
}

@Composable
fun FilePickerScreen(viewModel: RecentObservationsViewModel) {
    val openFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Handle the selected file URI
        if (uri != null) {
            // Do something with the selected file URI
            viewModel.updateURI(uri)
        }
    }

    Column {
        Button(
            onClick = {
                openFilePicker.launch("file/*")
            }
        ) {
            Text("Open File Picker")
        }
    }
}

fun populateObsView (viewModel: RecentObservationsViewModel) {
    viewModel.viewModelScope.launch {
        // Fetch observations only if it's not already available
        if (viewModel.observationSaver._observations.isEmpty()) {
            val observations = viewModel.observationSaver.getObservations()
            viewModel.uiState.observations = observations
        }
    }
}
