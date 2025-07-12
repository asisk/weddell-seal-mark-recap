package weddellseal.markrecap.ui.recentobservations

/*
 * Provides a view of the database records and an option to export to CSV
 * Updated when a new observation is saved
*/

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
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
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.ui.ConfirmEditDialog
import weddellseal.markrecap.ui.ObservationItem
import weddellseal.markrecap.ui.tagretag.TagRetagModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentObservationsScreen(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel,
    tagRetagViewModel: TagRetagModel,
) {
    val displayObservations by viewModel.displayObservations.collectAsState()
    val allObservations by viewModel.allObservations.collectAsState()

    val context = LocalContext.current
    context.contentResolver
    var observationToEdit by remember { mutableStateOf<ObservationRecord?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

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
                            "Current Observations",
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

            Box(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .border(4.dp, Color.LightGray)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    items(displayObservations) { displayObs ->
                        when (displayObs) {
                            is DisplayObservation.WithPups -> {
                                ObservationItem(
                                    onEditDo = {
                                        // If the technician has a partially complete observation,
                                        // prevent them from editing
                                        if (!tagRetagViewModel.primarySeal.value.isEntryStarted) {
                                            showEditDialog = true
                                            observationToEdit = displayObs.primarySeal
                                        } else {
                                            // Show a Toast message if the seal is already started
                                            Toast.makeText(
                                                context,
                                                "Looks like you're already editing another seal! Save or clear, then edit this record.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onViewDo = {
                                        tagRetagViewModel.loadObservationEntryForView(displayObs.primarySeal)
                                        navController.navigate(Screens.ObservationViewer.route)
                                    },
                                    observation = displayObs.primarySeal,
                                    pupOne = displayObs.pupOne,
                                    pupTwo = displayObs.pupTwo
                                )

                                HorizontalDivider()
                            }

                            is DisplayObservation.Standalone -> {
                                ObservationItem(
                                    onEditDo = {
                                        // If the technician has a partially complete observation,
                                        // prevent them from editing
                                        if (!tagRetagViewModel.primarySeal.value.isEntryStarted) {
                                            showEditDialog = true
                                            observationToEdit = displayObs.primarySeal
                                        } else {
                                            // Show a Toast message if the seal is already started
                                            Toast.makeText(
                                                context,
                                                "Looks like you're already editing another seal! Save or clear, then edit this record.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onViewDo = {
                                        tagRetagViewModel.loadObservationEntryForView(displayObs.primarySeal)
                                        navController.navigate(Screens.ObservationViewer.route)
                                    },
                                    observation = displayObs.primarySeal,
                                    pupOne = null,
                                    pupTwo = null
                                )

                                HorizontalDivider()
                            }
                        }
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
                        Toast.makeText(
                            context,
                            "You are about to edit this seal.",
                            Toast.LENGTH_LONG
                        ).show()

                        observationToEdit?.let { obs ->
                            tagRetagViewModel.resetUiStateIndicators()

                            var pupOne: ObservationRecord? = null
                            obs.relativeTagIDOne.takeIf { it.isNotEmpty() }
                                ?.toIntOrNull()
                                ?.let { relativeId ->
                                    pupOne = allObservations.find { it.id == relativeId }
                                }

                            var pupTwo: ObservationRecord? = null
                            obs.relativeTagIDTwo.takeIf { it.isNotEmpty() }
                                ?.toIntOrNull()
                                ?.let { relativeId ->
                                    pupTwo = allObservations.find { it.id == relativeId }
                                }

                            tagRetagViewModel.loadSealForEdit(obs, pupOne, pupTwo)
                            navController.navigate(Screens.AddObservationLog.route)
                        }
                    },
                )
            }
        }
    }
}