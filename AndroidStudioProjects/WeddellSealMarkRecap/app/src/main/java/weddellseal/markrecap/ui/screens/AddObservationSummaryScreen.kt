package weddellseal.markrecap.ui.screens

/*
 * Main screen for entering seal data
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.ui.components.SummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObservationSummaryScreen(
    navController: NavHostController,
    viewModel: AddObservationLogViewModel,
) {
    val state = viewModel.uiState
//    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var adultSeal = viewModel.primarySeal
    var pupOne = viewModel.pupOne
    val pupTwo = viewModel.pupTwo

    //send the user back to the observation screen when a log is saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.navigate(Screens.AddObservationLog.route)
        }
    }

    fun canSaveLog(callback: () -> Unit) {
        if (viewModel.isValid()) {
            callback()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Successfully saved!")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("You haven't completed all details")
            }
        }
    }
    // endregion

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Summary",
                            fontSize = 36.sp // Adjust this value as needed
                        )
                    }
                },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() } ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Edit Observation"
                            )
                        }
                    }
                },
                // TODO, address whether the Summary page should allow a user to abandon their observation by navigating home
//                actions = {
//                    IconButton(onClick = { navController.navigate(Screens.HomeScreen.route) }) {
//                        Icon(
//                            imageVector = Icons.Filled.Home,
//                            contentDescription = "Home",
//                            modifier = Modifier.size(48.dp)
//                        )
//                    }
//                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SummaryCard(viewModel, adultSeal, pupOne, pupTwo)
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(16.dp),
                containerColor = Color.LightGray,
                onClick = {
                    canSaveLog {
                        viewModel.createLog(adultSeal, pupOne, pupTwo)
                        if (!viewModel.uiState.isSaving) {
                            viewModel.removeSeal("primary")
                        }
                    }
                },
                icon = { Icon(Icons.Filled.Save, "Save and Start New Observation") },
                text = { Text(text = "Save and Start New Observation") }
            )
        }
    }
}




