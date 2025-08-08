package weddellseal.markrecap.ui.tagretag

/*
 * Main screen for entering seal data
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.UiEvent
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRetagScreen(
    navController: NavHostController,
    viewModel: TagRetagModel,
    homeViewModel: HomeViewModel,
    recentObsViewModel: RecentObservationsViewModel
) {
    val uiEventFlow = viewModel.uiEvent

    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }

    val primarySeal by viewModel.primarySeal.collectAsState()
    val pupOneSeal by viewModel.pupOne.collectAsState()
    val pupTwoSeal by viewModel.pupTwo.collectAsState()

    // TODO, remove once location testing is complete
//    LaunchedEffect(location) {
//        Log.d("UI", "Observed location: $location")
//    }

    // SAVE SUCCESS
    LaunchedEffect(Unit) {
        uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSavedToast -> {
                    snackBarHostState.showSnackbar(
                        event.message,
                        duration = SnackbarDuration.Long
                    )
                }

                else -> Unit // ignore all other events
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavMenu(navController)
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) },
            topBar = {
                TagRetagAppBar(
                    onNavigationIconClick = {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    },
                    homeViewModel
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                TagRetagHeader(viewModel, homeViewModel)

                // TODO, move this to the a Header component specific to Census
                // CENSUS METADATA
                if (homeUiState.selectedCensusNumber.isNotEmpty() && homeUiState.isCensusMode && !uiState.isEditMode) {
                    // Prepopulate Options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box() {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ExtendedFloatingActionButton(
                                    icon = {
                                        Icon(
                                            Icons.Filled.BabyChangingStation,
                                            "Mom & Pup",
                                            Modifier.size(36.dp)
                                        )
                                    },
                                    text = { Text("Mom & Pup") },
                                    onClick = {
                                        // update viewModel with prefilled fields
                                        if (primarySeal.ageClass == SealAgeClass.UNKNOWN) {
                                            viewModel.prefillMomAndPup()
                                        }
                                    },
                                    modifier = Modifier
                                        .alpha(if (uiState.isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                        .clickable(enabled = !uiState.isPrefilled) { } // Disable clicks if already selected
                                )
                                ExtendedFloatingActionButton(
                                    icon = {
                                        Icon(
                                            Icons.Filled.Female,
                                            "Single Female",
                                            Modifier.size(36.dp)
                                        )
                                    },
                                    text = { Text("Single Female") },
                                    onClick = {
                                        // update viewModel with prefilled fields
                                        if (primarySeal.ageClass == SealAgeClass.UNKNOWN) {
                                            viewModel.prefillSingleFemale()
                                        }
                                    },
                                    modifier = Modifier
                                        .alpha(if (uiState.isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                        .clickable(enabled = !uiState.isPrefilled) { } // Disable clicks if already selected
                                )
                                ExtendedFloatingActionButton(
                                    icon = {
                                        Icon(
                                            Icons.Filled.Male,
                                            "Single Male",
                                            Modifier.size(36.dp)
                                        )
                                    },
                                    text = { Text("Single Male") },
                                    onClick = {
                                        // update viewModel with prefilled fields
                                        if (primarySeal.ageClass == SealAgeClass.UNKNOWN) {
                                            viewModel.prefillSingleMale()
                                        }
                                    },
                                    modifier = Modifier
                                        .alpha(if (uiState.isPrefilled) 0.5f else 1f) // Change opacity when inactive
                                        .clickable(enabled = !uiState.isPrefilled) { } // Disable clicks if already selected
                                )
                            }
                        }
                    }
                }

                // SEAL CARDS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TabbedCards(viewModel, primarySeal, pupOneSeal, pupTwoSeal)
                }

                TagRetagFooter(viewModel, homeViewModel, recentObsViewModel, navController)

            }
        }
    }
}

