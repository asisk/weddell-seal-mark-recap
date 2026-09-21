package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColonyRow(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val metadata by viewModel.metadata.collectAsState()

    val autoDetectedColony by viewModel.autoDetectedColony.collectAsState()
    val coloniesList by viewModel.coloniesList.collectAsState()
    var showOverrideConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "Colony",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.width(28.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.overrideColony) {
                    ColonyDropDown(
                        label = "Selected Colony",
                        options = coloniesList,
                        selectedOption = metadata.selectedColony?.location ?: "",
                        onValueChange = { valueSelected ->
                            viewModel.updateSelectedColony(valueSelected)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (metadata.selectedColony != null && metadata.selectedColony?.location != "Other") {
                        Text(
                            text = metadata.selectedColony?.let {
                                "${metadata.selectedColony?.adjLat}" + "    " + "${metadata.selectedColony?.adjLong}"
                            } ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.setOverrideColonyCheckbox(false)
                            viewModel.clearColony()
                        }
                    ) {
                        Text(
                            text = ColonyGpsUi.USE_GPS_BUTTON,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    ColonyAutoDetectColumn(
                        autoDetectedColony = autoDetectedColony,
                        onOverrideClick = { showOverrideConfirm = true },
                    )
                }
            }
        }
    }

    if (showOverrideConfirm) {
        ColonyOverrideConfirmDialog(
            onDismiss = { showOverrideConfirm = false },
            onConfirm = {
                showOverrideConfirm = false
                viewModel.setOverrideColonyCheckbox(true)
            },
        )
    }
}

@Composable
internal fun ColonyAutoDetectColumn(
    autoDetectedColony: SealColony?,
    onOverrideClick: () -> Unit,
) {
    val waiting = autoDetectedColony == null
    val notDetected = autoDetectedColony?.location == ColonyPopulation.NOT_DETECTED
    Text(
        text = when {
            waiting -> ColonyGpsUi.WAITING_FOR_GPS
            else -> autoDetectedColony!!.location
        },
        style = if (waiting) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.titleLarge
        }.copy(textAlign = TextAlign.Center),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
    if (!waiting && !notDetected) {
        Text(
            text = "${autoDetectedColony?.adjLat}    ${autoDetectedColony?.adjLong}",
            style = MaterialTheme.typography.titleMedium
        )
    }
    if (waiting || notDetected) {
        Text(
            text = ColonyGpsUi.OVERRIDE_HINT,
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
    TextButton(onClick = onOverrideClick) {
        Text(
            text = ColonyGpsUi.OVERRIDE_BUTTON,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun ColonyOverrideConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = ColonyGpsUi.OVERRIDE_BUTTON,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = ColonyGpsUi.OVERRIDE_CONFIRM,
                style = MaterialTheme.typography.titleMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = ColonyGpsUi.OVERRIDE_CONFIRM_ACTION,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    )
}
