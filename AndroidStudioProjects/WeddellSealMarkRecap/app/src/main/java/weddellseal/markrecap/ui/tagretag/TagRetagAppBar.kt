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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRetagAppBar(
    onNavigationIconClick: () -> Unit,
    viewModel: TagRetagViewModel,
    homeViewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // CENSUS NUMBER
                if (homeUiState.isCensusMode) {
                    Text(
                        text = "Census #${homeUiState.selectedCensusNumber}",
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