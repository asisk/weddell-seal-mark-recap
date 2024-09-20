package weddellseal.markrecap.ui.screens

/*
 * Provides a view of the database records and an option to export to CSV
 * Updated when a new observation is saved
*/

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.ui.components.ObservationItem
import weddellseal.markrecap.ui.utils.notebookEntryValueObservation

@Composable
fun RecentObservationsScreen(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel
) {
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.observationsFlow.collect {
            state.observations = it
        }
    }

    RecentObsScaffold(navController, state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentObsScaffold(
    navController: NavHostController,
    state: RecentObservationsViewModel.UiState
) {
    val context = LocalContext.current
    context.contentResolver

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Saved Observations",
                            fontSize = 36.sp // Adjust this value as needed
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screens.HomeScreen.route)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp)
                    .border(1.dp, Color.Gray)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = true
                ) {
                    items(state.observations) { observation ->
//                        Text(
//                            text =
//                            "ID:  " + observation.id.toString() + "    " +
//                                    "Entered:  " + observation.date + "    " +
//                                    "Notebook Entry:  " + notebookEntryValueObservation(observation),
//                            modifier = Modifier.padding(8.dp)
//                        )
                        ObservationItem(observation = observation)

                        HorizontalDivider()
                    }
                }
            }
        }
    }
}