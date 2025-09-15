package weddellseal.markrecap.ui.tagretag

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.ui.ConfirmEditDialog
import weddellseal.markrecap.ui.RecentObservations
import weddellseal.markrecap.ui.UiEvent
import weddellseal.markrecap.ui.UiEvent.ShowEditDialog
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

@Composable
fun TagRetagFooter(
    viewModel: TagRetagViewModel,
    homeViewModel: HomeViewModel,
    recentObsViewModel: RecentObservationsViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current
    context.contentResolver

    val uiEventFlow = viewModel.uiEvent

    val selectedObservation by viewModel.selectedRecentObservation.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val hasEdits by viewModel.hasEdits.collectAsState()

    val homeUiState by homeViewModel.uiState.collectAsState()
    val location by homeViewModel.currentLocation.collectAsState()

    val primarySeal by viewModel.primarySeal.collectAsState()
    val pupOneSeal by viewModel.pupOne.collectAsState()
    val pupTwoSeal by viewModel.pupTwo.collectAsState()


    LaunchedEffect(Unit) {
        uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowEditToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }

                is ShowEditDialog -> {
                    showEditDialog = true
                }

                else -> Unit // ignore all other events
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // SAVE COLUMN
        Column(
            modifier = Modifier.fillMaxWidth(.4f),
            horizontalAlignment = Alignment.End
        ) {

            // SAVE BUTTON
            Row(
                modifier = Modifier.padding(10.dp)
            ) {
                ExtendedFloatingActionButton(
                    elevation = if (!uiState.isSaveEnabled || uiState.entryNeedsConfirmation || (uiState.isEditMode && !hasEdits)) FloatingActionButtonDefaults.elevation(
                        2.dp
                    ) else FloatingActionButtonDefaults.elevation(8.dp),
                    containerColor = if (!uiState.isSaveEnabled || uiState.entryNeedsConfirmation || (uiState.isEditMode && !hasEdits)) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondary,
                    onClick = {
                        if (!uiState.isSaveEnabled) return@ExtendedFloatingActionButton  // guard early exit

                        if (uiState.isEditMode && !hasEdits) return@ExtendedFloatingActionButton

                        viewModel.setIsSaving()

                        if (uiState.allSealsValid) {

                            val colonyLocation = homeUiState.selectedColony?.let {
                                GeoLocation(Coordinates(it.adjLat, it.adjLong))
                            } ?: location

                            viewModel.writeObservationRecord(colonyLocation)
                        } else {
                            // Ensure that the validation error list is current
                            // & mark as needsConfirmation if there are validation errors
                            viewModel.checkNeedsConfirmation(
                                primarySeal.validationErrors,
                                pupOneSeal.validationErrors,
                                pupTwoSeal.validationErrors
                            )
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Filled.Save,
                            "Save Seal",
                            Modifier.size(36.dp),
                            tint = if (!uiState.isSaveEnabled || uiState.entryNeedsConfirmation) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSecondary,
                        )
                    },
                    text = {
                        Text(
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                            text = "Save",
                            color = if (!uiState.isSaveEnabled || uiState.entryNeedsConfirmation) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSecondary,
                            style = MaterialTheme.typography.displaySmall,
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // SAVE DISABLED REASON
            if (primarySeal.isEntryStarted && uiState.ineligibleForSaveReason.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "Missing entries for required fields!",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 28.dp)
                    ) {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand error details",
                                tint = Color.Red,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Dropdown menu with options
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            for (error in uiState.ineligibleForSaveReason.split(",")) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            error,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                    },
                                    onClick = { /* do nothing */ }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.entryNeedsConfirmation) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Requires confirmation or editing!",
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(60.dp))

    Text(
        "Recently Entered",
        modifier = Modifier.padding(start = 40.dp, end = 40.dp, bottom = 40.dp),
        style = MaterialTheme.typography.headlineMedium,
    )

    // RECENT OBSERVATIONS VIEW
    RecentObservations(viewModel, recentObsViewModel, navController)

    // CONFIRM EDIT DIALOG
    if (showEditDialog) {
        ConfirmEditDialog(
            onDismissRequest = {
                showEditDialog = false
            },
            onConfirmation = {
                showEditDialog = false

                // Determine which seals are present and load them in the Tag/Retag Screen for Editing
                selectedObservation?.let { record ->
                    viewModel.resetModelState()
                    viewModel.loadSealForEdit(record)
                    navController.navigate(Screens.TagRetag.route)
                }
            },
        )
    }
}
