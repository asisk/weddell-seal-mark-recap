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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Cancel
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.ui.home.HomeViewModel

@Composable
fun TagRetagHeader(
    viewModel: TagRetagModel,
    homeViewModel: HomeViewModel,
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val location by homeViewModel.currentLocation.collectAsState()

    val primarySeal by viewModel.primarySeal.collectAsState()
    val pupOneSeal by viewModel.pupOne.collectAsState()
    val pupTwoSeal by viewModel.pupTwo.collectAsState()

    // CENSUS PREPOPULATE OPTIONS
    if (homeUiState.isCensusMode && !primarySeal.isEntryStarted) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

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
                        text = "Mom & Pup",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                onClick = {
                    // update viewModel with prefilled fields
                    if (primarySeal.ageClass == SealAgeClass.UNKNOWN) {
                        viewModel.prefillMomAndPup()
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
            )

            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        Icons.Filled.Female,
                        "Single Female",
                        Modifier.size(36.dp)
                    )
                },
                text = {
                    Text(
                        text = "Single Female",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                onClick = {
                    // update viewModel with prefilled fields
                    if (primarySeal.ageClass == SealAgeClass.UNKNOWN) {
                        viewModel.prefillSingleFemale()
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
            )

            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        Icons.Filled.Male,
                        "Single Male",
                        Modifier.size(36.dp)
                    )
                },
                text = {
                    Text(
                        text = "Single Male",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                onClick = {
                    // update viewModel with prefilled fields
                    if (primarySeal.ageClass == SealAgeClass.UNKNOWN) {
                        viewModel.prefillSingleMale()
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
            )
        }
    }

    if (uiState.isEditMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // CENSUS NUMBER
            if (uiState.observationCensusNumber != "") {
                Text(
                    text = "#${uiState.observationCensusNumber}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(36.dp))

            // OBSERVERS & COLONY - FROM OBSERVATION RECORD
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.originalMetadata.getObserversString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    text = uiState.originalMetadata.selectedColony,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(36.dp))

            // GPS LOCATION - FROM OBSERVATION RECORD
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

            Spacer(modifier = Modifier.width(36.dp))

            // CANCEL EDIT BUTTON
            ExtendedFloatingActionButton(
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                onClick = {
                    viewModel.exitEditMode()
                },
                icon = {
                    Icon(
                        Icons.Outlined.Cancel,
                        "Exit Edit Mode",
                        Modifier.size(36.dp),
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

        // WARNING BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFE0B2))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
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

            // CONFIRM AND SAVE BUTTON
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(start = 10.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                onClick = {
                    // flag seals for review
                    if (!primarySeal.isValid) {
                        viewModel.flagSealForReview(primarySeal.sealType)
                    }
                    if (!pupOneSeal.isValid) {
                        viewModel.flagSealForReview(pupOneSeal.sealType)
                    }
                    if (!pupTwoSeal.isValid) {
                        viewModel.flagSealForReview(pupTwoSeal.sealType)
                    }

                    viewModel.writeObservationRecord(location)
                },
                icon = { Icon(Icons.Filled.Save, "Confirm & Save", Modifier.size(36.dp)) },
                text = {
                    Text(
                        text = "Confirm & Save",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            )

            // EDIT BUTTON ON VALIDATION ERROR
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(start = 10.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                onClick = {
                    // enable edit
                    viewModel.editAfterAttemptedSave()
                },
                icon = {
                    Icon(
                        Icons.Filled.Save, "Edit", Modifier.size(36.dp)
                    )
                },
                text = {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            )
        }
    }
}
