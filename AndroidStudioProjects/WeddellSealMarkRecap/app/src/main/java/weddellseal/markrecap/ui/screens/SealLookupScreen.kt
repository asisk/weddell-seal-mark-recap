package weddellseal.markrecap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.ErrorDialog
import weddellseal.markrecap.ui.components.SealSearchField
import weddellseal.markrecap.ui.components.WedCheckCard

@Composable
fun SealLookupScreen(
    navController: NavHostController,
    wedCheckViewModel: WedCheckViewModel,
    viewModel: AddObservationLogViewModel
) {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                BottomNavigation(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    BottomNavigationItem(
                        label = { Text(text = "Home") },
                        selected = false,
                        onClick = { navController.navigate(Screens.HomeScreen.route) },
                        icon = { Icon(Icons.Filled.Home, null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) { // SEAL LOOKUP
                    Row(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Seal Lookup")
                        var sealTagID by remember { mutableStateOf("") }
                        SealSearchField(wedCheckViewModel) { value ->
                            sealTagID = value
                        }
                        if (wedCheckViewModel.uiState.isSearching) {
                            CircularProgressIndicator() // Display a loading indicator while searching
                        } else {
                            IconButton(
                                onClick = {
                                    // reset the current seal for new search
                                    if (viewModel.wedCheckSeal.isStarted) {
                                        viewModel.resetWedCheckSeal()
                                        wedCheckViewModel.resetSearch()
                                    }
                                    wedCheckViewModel.findSeal(sealTagID, viewModel)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }
                        // Display error dialog if there's an error
                        if (viewModel.uiState.isError) {
                            ErrorDialog(errorMessage = viewModel.uiState.errorMessage) {
                                viewModel.dismissError()
                            }
                        }
                    }
                    if (!wedCheckViewModel.uiState.isSearching) {
                        if (viewModel.wedCheckSeal.isStarted) {
                            WedCheckCard(viewModel, viewModel.wedCheckSeal)
                        }

                        if (wedCheckViewModel.uiState.sealNotFound == true) {
                            SealNotFoundToast()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SealNotFoundToast() {
    val context = LocalContext.current
    Toast.makeText(context, "Seal not found", Toast.LENGTH_LONG).show()
}