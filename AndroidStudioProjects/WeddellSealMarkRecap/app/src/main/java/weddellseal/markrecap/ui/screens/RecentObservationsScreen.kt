package weddellseal.markrecap.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.RecentObservationsViewModelFactory
import weddellseal.markrecap.ui.utils.notebookEntryValueObservation

@Composable
fun RecentObservationsScreen(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel = viewModel(factory = RecentObservationsViewModelFactory())
) {
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.observationsFlow.collect {
            state.observations = it
        }
    }

    recentObsScaffold(navController, viewModel, state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun recentObsScaffold(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel,
    state: RecentObservationsViewModel.UiState
) {
    val context = LocalContext.current
    context.contentResolver

    // supports writing data to CSV
    val createDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
            // Handle the created document URI
            if (uri != null) {
                viewModel.updateURI(uri)
                viewModel.exportLogs(context)
            }
        }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                BottomNavigation(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    BottomNavigationItem(
                        label = { Text(text = "Home") },
                        selected = false,
                        onClick = { navController.navigate(Screens.HomeScreen.route) },
                        icon = { Icon(Icons.Filled.Home, null) }
                    )
                    BottomNavigationItem(
                        label = { Text(text = "Build CSV File") },
                        selected = false,
                        onClick = { createDocument.launch("observations.csv") },
                        icon = { Icon(Icons.Filled.Build, null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Observations Collected",
                modifier = Modifier.padding(15.dp),
                style = MaterialTheme.typography.headlineSmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp)
                    .border(1.dp, Color.Gray)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
//                    if (state.observations.isEmpty()) {
//                        viewModel.populateObsView()
//                    }
                    items(state.observations) { observation ->
                        Text(text =
                                "ID:  " + observation.id.toString() + "    " +
                                "Entered:  " + observation.date + "    " +
                                "Notebook Entry:  " + notebookEntryValueObservation(observation),
                            modifier = Modifier.padding(8.dp))
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
    val createDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("file/csv")) { uri: Uri? ->
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
    val openFilePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
