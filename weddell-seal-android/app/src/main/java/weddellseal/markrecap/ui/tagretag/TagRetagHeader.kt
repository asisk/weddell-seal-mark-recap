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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.dialogs.RemoveDialog

@Composable
fun TagRetagHeader(
    viewModel: TagRetagViewModel,
    homeViewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val metadata by homeViewModel.metadata.collectAsState()

    val focusManager = LocalFocusManager.current
    var pendingCensusPrefill by remember { mutableStateOf<CensusPrefill?>(null) }

    fun onCensusPrefillSelected(prefill: CensusPrefill) {
        if (viewModel.requestCensusPrefill(prefill)) {
            pendingCensusPrefill = prefill
        }
    }

    // CENSUS PREPOPULATE
    if (metadata.isCensusMode && !uiState.isEditMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CENSUS PREPOPULATE - MOM & PUP
            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        painter = painterResource(R.mipmap.ic_mom_pup_foreground),
                        contentDescription = "Mom and pup",
                        modifier = Modifier.size(60.dp)
                    )
                },
                text = {
                    Text(
                        text = "Mom &\nPup",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                onClick = { onCensusPrefillSelected(CensusPrefill.MOM_AND_PUP) },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
            )

            // CENSUS PREPOPULATE - SINGLE FEMALE
            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_female),
                        contentDescription = "Single Female",
                        modifier = Modifier.size(36.dp),
                    )
                },
                text = {
                    Text(
                        text = "Single\nFemale",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                onClick = { onCensusPrefillSelected(CensusPrefill.SINGLE_FEMALE) },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
            )

            // CENSUS PREPOPULATE - SINGLE MALE
            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_male),
                        contentDescription = "Single Male",
                        modifier = Modifier.size(36.dp),
                    )
                },
                text = {
                    Text(
                        text = "Single\nMale",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                onClick = { onCensusPrefillSelected(CensusPrefill.SINGLE_MALE) },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
            )
        }

        val prefillToApply = pendingCensusPrefill
        if (prefillToApply != null) {
            RemoveDialog(
                onDismissRequest = { pendingCensusPrefill = null },
                onConfirmation = {
                    viewModel.confirmCensusPrefill(prefillToApply)
                    pendingCensusPrefill = null
                },
                text = "Are you sure you want to start your entry over?",
                buttonText = "Start over",
            )
        }
    }

    // EDIT MODE HEADER
    if (uiState.isEditMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // EDIT OBSERVATION RECORD - CENSUS NUMBER
                    if (uiState.originalMetadata.censusNumber != "") {
                        Text(
                            text = "#${uiState.originalMetadata.censusNumber}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = uiState.originalMetadata.getObserversString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.originalMetadata.selectedColony?.location ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // EDIT OBSERVATION RECORD - GPS LOCATION
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val editModeLocation =
                            "${uiState.observationLocation?.coordinates?.latitude}    " +
                                    "${uiState.observationLocation?.coordinates?.longitude}"

                        Text(
                            text = editModeLocation,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Text(
                            text = uiState.observationTimestamp,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }


            // EDIT OBSERVATION RECORD - CANCEL EDIT BUTTON
            ExtendedFloatingActionButton(
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                onClick = {
                    viewModel.exitEditMode()
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancel_outlined),
                        contentDescription = "Exit Edit Mode",
                        modifier = Modifier.size(36.dp),
                    )
                },
                text = {
                    Text(
                        text = "Cancel Edit",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            )
        }
    }

    // Warning Banner displayed when data entered has validation errors
    if (uiState.isSaveAttempted && uiState.validationFailureReason.isNotBlank()) {
        Spacer(modifier = Modifier.height(20.dp))

        // VALIDATION - WARNING BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFE0B2))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = "Warning",
                tint = Color(0xFFF57C00),
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = "Please review the data you've entered and confirm it is correct before saving.",
                color = Color(0xFF5D4037),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            val confirmActionsEnabled = !uiState.isSaveInProgress
            val confirmActionColor = if (confirmActionsEnabled) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.surface
            }
            val confirmActionContentColor = if (confirmActionsEnabled) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.surface
            }

            // VALIDATION - CONFIRM AND SAVE BUTTON
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(start = 10.dp),
                containerColor = confirmActionColor,
                elevation = if (confirmActionsEnabled) {
                    FloatingActionButtonDefaults.elevation(8.dp)
                } else {
                    FloatingActionButtonDefaults.elevation(2.dp)
                },
                onClick = {
                    if (!confirmActionsEnabled) return@ExtendedFloatingActionButton
                    // Fix #3: blur before save; fix #1/#2: confirmAndSave in ViewModel.
                    focusManager.clearFocus()
                    viewModel.confirmAndSave(homeViewModel.getColonyLocation())
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = "Confirm & Save",
                        modifier = Modifier.size(36.dp),
                        tint = confirmActionContentColor,
                    )
                },
                text = {
                    Text(
                        text = "Confirm & Save",
                        color = confirmActionContentColor,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            )

            // VALIDATION - EDIT BUTTON ON VALIDATION ERROR
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(start = 10.dp),
                containerColor = confirmActionColor,
                elevation = if (confirmActionsEnabled) {
                    FloatingActionButtonDefaults.elevation(8.dp)
                } else {
                    FloatingActionButtonDefaults.elevation(2.dp)
                },
                onClick = {
                    if (!confirmActionsEnabled) return@ExtendedFloatingActionButton
                    viewModel.editAfterAttemptedSave()
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = "Edit",
                        modifier = Modifier.size(36.dp),
                        tint = confirmActionContentColor,
                    )
                },
                text = {
                    Text(
                        text = "Edit",
                        color = confirmActionContentColor,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            )
        }
    }
}
