package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColonyRow(
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val metadata by viewModel.metadata.collectAsState()

    val autoDetectedColony by viewModel.autoDetectedColony.collectAsState()
    val coloniesList by viewModel.coloniesList.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Colony",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.width(28.dp))

        // OVERRIDE CHECKBOX
        // Allows a technician to manually select a seal colony
        Column(
            Modifier
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Override",
                style = MaterialTheme.typography.titleMedium
            )
            Checkbox(
                checked = uiState.overrideColony,
                onCheckedChange = {
                    viewModel.setOverrideColonyCheckbox(it)
                    if (!it) {
                        viewModel.clearColony()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        //COLONY DROPDOWN
        Column(
            modifier = Modifier.fillMaxWidth(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.overrideColony) {
                ColonyDropDown(
                    label = "Selected Colony",
                    options = coloniesList,
                    selectedOption = metadata.selectedColony?.location ?: "",
                    onValueChange = { valueSelected ->
                        viewModel.updateSelectedColony(valueSelected)
                    }
                )
                if (metadata.selectedColony != null && metadata.selectedColony?.location != "Other") {
                    Text(
                        text = metadata.selectedColony?.let { "${metadata.selectedColony?.adjLat}" + "    " + "${metadata.selectedColony?.adjLong}" }
                            ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                // AUTO-DETECTED COLONY
                Text(
                    text = autoDetectedColony?.location
                        ?: "...detecting proximity to a known colony...",
                    style = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
                )
                Text(
                    text = autoDetectedColony?.location?.let { "${autoDetectedColony?.adjLat}" + "    " + "${autoDetectedColony?.adjLong}" }
                        ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
