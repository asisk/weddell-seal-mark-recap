package weddellseal.markrecap.ui.tagretag


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.location.data.toLocationString
import weddellseal.markrecap.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRetagAppBar(
    onNavigationIconClick: () -> Unit,
    viewModel: TagRetagModel,
    homeViewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val location by homeViewModel.currentLocation.collectAsState()

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CENSUS NUMBER
                if (homeUiState.isCensusMode) {
                    Text(
                        text = "#${homeUiState.selectedCensusNumber}",
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                val errorColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)

                // SELECTED OBSERVERS & COLONY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val observersText =
                        if (uiState.metadata.selectedObservers.isEmpty()) "Observers missing"
                        else uiState.metadata.getObserversString()
                    Text(
                        text = observersText,
                        color = if (uiState.metadata.selectedObservers.isEmpty()) errorColor else MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleLarge
                    )

                    val colonyText = uiState.metadata.selectedColony.ifEmpty { "Colony missing" }
                    Text(
                        text = colonyText,
                        color = if (uiState.metadata.selectedColony.isEmpty()) MaterialTheme.colorScheme.error.copy(
                            alpha = 0.9f
                        ) else MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                // DEVICE GPS LOCATION ICON
                if (location?.coordinates?.longitude != null && location?.coordinates?.latitude != null) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF1D9C06),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp),
                    )
                } else {
                    Icon(
                        Icons.Filled.LocationOff,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp),
                    )
                }

                // DEVICE GPS LOCATION COORDINATES & TIMESTAMP
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (location?.coordinates?.longitude != null && location?.coordinates?.latitude != null) {
                        location?.toLocationString()?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        location?.updatedDate?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Text(
                            text = "Cannot provide coordinates!",
                            style = MaterialTheme.typography.titleLarge,
                            color = errorColor
                        )
                    }
                }
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