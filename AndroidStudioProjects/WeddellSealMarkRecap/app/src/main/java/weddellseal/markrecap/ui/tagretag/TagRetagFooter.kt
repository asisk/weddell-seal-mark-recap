package weddellseal.markrecap.ui.tagretag

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.ui.ConfirmEditDialog
import weddellseal.markrecap.ui.ObservationItem
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

@Composable
fun TagRetagFooter(
    viewModel: TagRetagModel,
    homeViewModel: HomeViewModel,
    recentObsViewModel: RecentObservationsViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current
    context.contentResolver

    val currentObservations by recentObsViewModel.currentObservations.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var observationToEdit by remember { mutableStateOf<ObservationLogEntry?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    val location by homeViewModel.currentLocation.collectAsState()

    val primarySeal by viewModel.primarySeal.collectAsState()
    val pupOneSeal by viewModel.pupOne.collectAsState()
    val pupTwoSeal by viewModel.pupTwo.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(.9f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(.65f)
        ) {
            // RECENT OBSERVATIONS VIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recently \nEntered",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp) // Limit the height
                            .padding(10.dp)
                            .border(1.dp, Color.LightGray) // Add border for visual purposes
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            userScrollEnabled = true
                        ) {
                            items(currentObservations) { observation ->
                                ObservationItem(
                                    onEditDo = {
                                        if (!primarySeal.isStarted) {
                                            showEditDialog = true
                                            observationToEdit = observation
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
                                        viewModel.updateObservationEntry(observation)
                                        navController.navigate(Screens.ObservationViewer.route)
                                    },
                                    observation = observation
                                )

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        // SAVE
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SAVE DISABLED REASON
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (!uiState.isSaveEnabled && uiState.ineligibleForSaveReason.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Save disabled",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Save disabled!",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            // SAVE BUTTON
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .wrapContentWidth()
                    .alpha(if (uiState.isSaveEnabled && !uiState.entryNeedsConfirmation) 1f else 0.4f), // visually "disabled"
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                onClick = {
                    if (!uiState.isSaveEnabled || uiState.entryNeedsConfirmation) return@ExtendedFloatingActionButton  // guard early exit

                    viewModel.setIsSaving()

                    if (primarySeal.isValid && pupOneSeal.isValid && pupTwoSeal.isValid) {
                        viewModel.setMetadata(
                            TagRetagModel.ObservationMetadata(
                                selectedColony = homeUiState.selectedColony,
                                selectedObservers = homeUiState.selectedObservers,
                                censusNumber = homeUiState.selectedCensusNumber
                            )
                        )
                        viewModel.createLog(
                            location
                        )
                    } else {
                        viewModel.updateValidationErrors(
                            primarySeal.validationErrors,
                            pupOneSeal.validationErrors,
                            pupTwoSeal.validationErrors
                        )
                    }
                },
                icon = { Icon(Icons.Filled.Save, "Save Seal") },
                text = {
                    Text(
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                        text = "Save",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            )

            if (!uiState.isSaveEnabled && uiState.ineligibleForSaveReason.isNotBlank()) {
                var expanded by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { expanded = !expanded }
                ) {
                    Text(
                        text = if (expanded) uiState.ineligibleForSaveReason else "These required fields are missing!",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand error details",
                        tint = Color.Red
                    )
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
                    "You are about to edit this seal. To edit relatives, select records for editing separately.",
                    Toast.LENGTH_LONG
                ).show()

                // set the seal in the observation view model & navigate to edit
                if (observationToEdit != null) {
                    viewModel.resetStateOnSaved()
                    viewModel.populateSealFromObservation(observationToEdit)
                    navController.navigate(Screens.AddObservationLog.route)
                }
            },
        )
    }
}
