package weddellseal.markrecap.ui.tagretag


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRetagAppBar(
    onNavigationIconClick: () -> Unit,
    homeViewModel: HomeViewModel
) {
    val metadata by homeViewModel.metadata.collectAsState()
    val uiState by homeViewModel.uiState.collectAsState()

    val currentLocation = homeViewModel.currentLocation
    var colony by remember { mutableStateOf<SealColony?>(null) }

    LaunchedEffect(currentLocation) {
        if (currentLocation.value != null) {
            colony = homeViewModel.findColony(currentLocation.value!!.coordinates)
        }
    }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // CENSUS NUMBER
                if (metadata.isCensusMode) {
                    Text(
                        text = "Census #${metadata.censusNumber}",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Start
                    )
                } else {
                    Text(
                        text = "Tag / Retag",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Start
                    )
                }

                val errorColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)

                // SELECTED OBSERVERS & COLONY
                val observersText =
                    if (metadata.selectedObservers.isEmpty()) "Observers missing"
                    else metadata.getObserversString()
                Text(
                    text = observersText,
                    color = if (metadata.selectedObservers.isEmpty()) errorColor else MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleLarge
                )


                val colonyLocationName =
                    if (uiState.overrideColony) {
                        metadata.selectedColony?.location ?: "Colony missing"
                    } else {
                        colony?.location ?: "Colony missing"
                    }

                Text(
                    text = colonyLocationName,
                    color = if (colonyLocationName == "Colony missing") MaterialTheme.colorScheme.error.copy(
                        alpha = 0.9f
                    ) else MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Toggle drawer",
                    Modifier.size(36.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        )
    )
}