package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    val autoDetectedColony by viewModel.autoDetectedColony.collectAsState()
    val coloniesList by viewModel.coloniesList.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(.45f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = "Colony",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.width(30.dp))
                Box(
                    modifier = Modifier.weight(1f)  // take the remaining space
                ) {
                    Column(
                        Modifier.padding(horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Override",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Checkbox(
                            checked = uiState.manualColonyCheckbox,
                            onCheckedChange = {
                                viewModel.setManualColonyCheckbox(it)
                                if (!it) {
                                    viewModel.clearColony()
                                }
                            },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            if (uiState.manualColonyCheckbox) {
                ColonyDropDown(
                    label = "Selected Colony",
                    options = coloniesList,
                    selectedOption = uiState.selectedColony,
                    onValueChange = { valueSelected ->
                        viewModel.updateSelectedColony(valueSelected)
                    }
                )
            } else {
                Text(
                    text = autoDetectedColony?.location
                        ?: "...detecting proximity to a known colony...",
                    style = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
                )
            }
        }
    }
}
