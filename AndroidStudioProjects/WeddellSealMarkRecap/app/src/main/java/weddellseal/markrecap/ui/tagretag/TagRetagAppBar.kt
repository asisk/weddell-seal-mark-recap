package weddellseal.markrecap.ui.tagretag


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // TAG/RETAG & CENSUS TITLE
                Text(
                    text = if (homeUiState.isCensusMode) "Census #${homeUiState.selectedCensusNumber}" else "Tag/Retag",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 36.sp, // Adjust this value as needed
    textAlign = TextAlign.Start
                    )

                Spacer(modifier = Modifier.width(20.dp))

                val observersText =
                    if (uiState.metadata.selectedObservers.isEmpty()) ""
                    else uiState.metadata.getObserversString()

                Text(
                    text = "Observers:  $observersText",
                    color = if (uiState.metadata.selectedObservers.isEmpty()) MaterialTheme.colorScheme.error.copy(
                        alpha = 0.9f
                    ) else MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.width(20.dp))

                val colonyText = uiState.metadata.selectedColony.ifEmpty { "" }

                Text(
                    text = "Colony:  $colonyText",
                    color = if (uiState.metadata.selectedColony.isEmpty()) MaterialTheme.colorScheme.error.copy(
                        alpha = 0.9f
                    ) else MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleLarge

                    )

                Spacer(modifier = Modifier.width(10.dp))

                // GPS LOCATION
//                Box(
//                    modifier = Modifier.weight(0.6f)
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.End
//                    ) {
//
//                        if (location?.coordinates?.longitude != null) {
//                            Icon(
//                                Icons.Filled.LocationOn,
//                                contentDescription = null,
//                                tint = Color(0xFF1D9C06),
//                                modifier = Modifier
//                                    .padding(start = 10.dp, end = 10.dp)
//                                    .size(40.dp),
//                            )
//                        } else {
//                            Icon(
//                                Icons.Filled.LocationOff,
//                                contentDescription = null,
//                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
//                                modifier = Modifier
//                                    .padding(start = 10.dp, end = 10.dp)
//                                    .size(40.dp),
//                            )
//                        }
//
//                        Text(
//                            text = location?.toLocationString()
//                                ?: "Cannot provide location coordinates!",
//                            style = MaterialTheme.typography.titleMedium,
//                        )
//                    }
//                }
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