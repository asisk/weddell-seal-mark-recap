package weddellseal.markrecap.ui.screens

/*
 * Provides a view of the database records and an option to export to CSV
 * Updated when a new observation is saved
*/

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.ui.components.ConfirmEditDialog
import weddellseal.markrecap.ui.components.ObservationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentObservationsScreen(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel,
    obsViewModel: AddObservationLogViewModel,
) {
    val state = viewModel.uiState
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    context.contentResolver
    var observationToEdit by remember { mutableStateOf<ObservationLogEntry?>(null) }

    LaunchedEffect(Unit) {
        viewModel.observationsFlow.collect {
            state.observations = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Saved Observations",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp)
                    .border(1.dp, Color.Gray)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = true
                ) {
                    items(state.observations) { observation ->
                        ObservationItem(
                            onEditDo = {
                                if (!obsViewModel.primarySeal.isStarted) {
                                    showEditDialog = true
                                    observationToEdit = observation
                                } else {
                                    // Show a Toast message if the seal is already started
                                    Toast.makeText(context, "Looks like you're already editing another seal! Finish or delete to edit this record.", Toast.LENGTH_LONG).show()
                                }
                            },
                            onViewDo = {
                                obsViewModel.updateObservationEntry(observation)
                                navController.navigate(Screens.ObservationViewer.route)
                            },
                            observation = observation
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Show the dialog if showDialog is true
            if (showEditDialog) {
                ConfirmEditDialog(
                    onDismissRequest = {
                        showEditDialog = false
                    },
                    onConfirmation = {
                        showEditDialog = false
                        Toast.makeText(context, "You are about to edit this seal. To edit relatives, select records for editing separately.", Toast.LENGTH_LONG).show()

                        // set the seal in the observationviewmodel & navigate to edit
                        if (observationToEdit != null) {
                            obsViewModel.resetSaved()
                            obsViewModel.populateSealFromObservation(observationToEdit)
                            navController.navigate(Screens.AddObservationLog.route)
                        }
                    },
                )
            }
        }
    }
}