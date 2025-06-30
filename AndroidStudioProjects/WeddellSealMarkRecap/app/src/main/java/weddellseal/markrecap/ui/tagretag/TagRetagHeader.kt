package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.domain.location.data.toLocationString
import weddellseal.markrecap.ui.home.HomeViewModel

@Composable
fun TagRetagHeader(
    viewModel: TagRetagModel,
    homeViewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    val location by homeViewModel.currentLocation.collectAsState()

    val primarySeal by viewModel.primarySeal.collectAsState()
    val pupOneSeal by viewModel.pupOne.collectAsState()
    val pupTwoSeal by viewModel.pupTwo.collectAsState()

    // METADATA SECTION
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // GPS Location
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (location?.coordinates?.longitude != null) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF1D9C06),
                        modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp)
                            .size(40.dp),
                    )
                } else {
                    Icon(
                        Icons.Filled.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp)
                            .size(40.dp),
                    )
                }

                Text(
                    text = location?.toLocationString() ?: "Cannot provide location coordinates!",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier.weight(.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Observers
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                val selectedText =
                    if (uiState.metadata.selectedObservers.isEmpty()) "Select observers"
                    else uiState.metadata.getObserversString()

                Text(
                    text = "Observers:  $selectedText",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.metadata.selectedObservers.isEmpty()) MaterialTheme.colorScheme.error.copy(
                        alpha = 0.9f
                    ) else Color.Black
                )
            }

            // Colony Location
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val selectedText = if (uiState.metadata.selectedColony.isEmpty()) "Select a colony"
                else homeUiState.selectedColony

                Text(
                    text = "Colony:  $selectedText",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (homeUiState.selectedColony == "") MaterialTheme.colorScheme.error.copy(
                        alpha = 0.9f
                    ) else Color.Black
                )
            }
//            // SAVE DISABLED REASON
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.Center,
//                modifier = Modifier.padding(horizontal = 16.dp)
//            ) {
//                if (!uiState.isSaveEnabled && uiState.ineligibleForSaveReason.isNotBlank()) {
//                    Icon(
//                        imageVector = Icons.Filled.Warning,
//                        contentDescription = "Save disabled",
//                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
//                    )
//                    Text(
//                        text = "Save disabled!",
//                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
//                        fontSize = 14.sp,
//                        modifier = Modifier
//                            .padding(horizontal = 8.dp)
//                    )
//                } else {
//                    Spacer(modifier = Modifier.height(22.dp))
//                }
//            }

//            // SAVE BUTTON
//            ExtendedFloatingActionButton(
//                modifier = Modifier
//                    .wrapContentWidth()
//                    .alpha(if (uiState.isSaveEnabled && !uiState.entryNeedsConfirmation) 1f else 0.4f), // visually "disabled"
//                containerColor = MaterialTheme.colorScheme.secondaryContainer,
//                elevation = FloatingActionButtonDefaults.elevation(8.dp),
//                onClick = {
//                    if (!uiState.isSaveEnabled || uiState.entryNeedsConfirmation) return@ExtendedFloatingActionButton  // guard early exit
//
//                    viewModel.setIsSaving()
//
//                    if (primarySeal.isValid && pupOneSeal.isValid && pupTwoSeal.isValid) {
//                        viewModel.setMetadata(
//                            TagRetagModel.ObservationMetadata(
//                                selectedColony = homeUiState.selectedColony,
//                                selectedObservers = homeUiState.selectedObservers,
//                                censusNumber = homeUiState.selectedCensusNumber
//                            )
//                        )
//                        viewModel.createLog(
//                            location
//                        )
//                    } else {
//                        viewModel.updateValidationErrors(
//                            primarySeal.validationErrors,
//                            pupOneSeal.validationErrors,
//                            pupTwoSeal.validationErrors
//                        )
//                    }
//                },
//                icon = { Icon(Icons.Filled.Save, "Save Seal") },
//                text = {
//                    Text(
//                        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
//                        text = "Save",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black
//                    )
//                }
//            )

//            if (!uiState.isSaveEnabled && uiState.ineligibleForSaveReason.isNotBlank()) {
//                var expanded by remember { mutableStateOf(false) }
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.Center,
//                    modifier = Modifier
//                        .padding(horizontal = 8.dp)
//                        .clickable { expanded = !expanded }
//                ) {
//                    Text(
//                        text = if (expanded) uiState.ineligibleForSaveReason else "These required fields are missing!",
//                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
//                        fontSize = 14.sp,
//                        modifier = Modifier
//                            .padding(horizontal = 8.dp)
//                    )
//
//                    Icon(
//                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                        contentDescription = "Expand error details",
//                        tint = Color.Red
//                    )
//                }
//            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Warning Banner displayed when data entered has validation errors
    if (uiState.isSaving && uiState.validationFailureReason.isNotBlank()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(.7f)
            ) {
                // WARNING BANNER
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFFE0B2))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Please review the data you've entered and confirm it is correct before saving.",
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // CONFIRM AND SAVE BUTTON
                ExtendedFloatingActionButton(
                    modifier = Modifier
//                        .wrapContentWidth()
                        .padding(start = 10.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    onClick = {
                        // flag seals for review
                        if (!primarySeal.isValid) {
                            viewModel.flagSealForReview(primarySeal.name)
                        }
                        if (!pupOneSeal.isValid) {
                            viewModel.flagSealForReview(pupOneSeal.name)
                        }
                        if (!pupTwoSeal.isValid) {
                            viewModel.flagSealForReview(pupTwoSeal.name)
                        }

                        viewModel.setMetadata(
                            TagRetagModel.ObservationMetadata(
                                selectedColony = homeUiState.selectedColony,
                                selectedObservers = homeUiState.selectedObservers,
                                censusNumber = homeUiState.selectedCensusNumber
                            )
                        )

                        viewModel.createLog(location)
                    },
                    icon = { Icon(Icons.Filled.Save, "Confirm & Save") },
                    text = {
                        Text(
                            text = "Confirm & Save",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                )
            }
        }
    }

// because this action results in removing any entered data
// Show the dialog if showDialog is true
//    if (showConfirmEntryDialog) {
//        SealInvalidDialog(
//            viewModel,
//            onDismissRequest = {
//                showConfirmEntryDialog = false
//            },
//            onConfirmation = {
//
//                // flag seals for review
//                if (!primarySeal.isValid) {
//                    viewModel.flagSealForReview(primarySeal.name)
//                }
//                if (!pupOneSeal.isValid) {
//                    viewModel.flagSealForReview(pupOneSeal.name)
//                }
//                if (!pupTwoSeal.isValid) {
//                    viewModel.flagSealForReview(pupTwoSeal.name)
//                }
//
//                showConfirmEntryDialog = false
//
//                viewModel.setMetadata(
//                    TagRetagModel.ObservationMetadata(
//                        selectedColony = homeUiState.selectedColony,
//                        selectedObservers = homeUiState.selectedObservers,
//                        censusNumber = homeUiState.selectedCensusNumber
//                    )
//                )
//
//                viewModel.createLog(
//                    location
//                )
//            },
//        )
//
////        if (showIneligibleDialog) {
////            IneligibleForSaveDialog(
////                uiState.ineligibleForSaveReason,
////                onDismissRequest = {
////                    showIneligibleDialog = false
////                }
////            )
////        }
//    }
}
