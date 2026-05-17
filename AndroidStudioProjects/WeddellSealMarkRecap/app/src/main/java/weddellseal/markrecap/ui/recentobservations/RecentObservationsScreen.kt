package weddellseal.markrecap.ui.recentobservations

/*
 * Provides a view of the database records and an option to export to CSV
 * Updated when a new observation is saved
*/

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.ui.AppBar
import weddellseal.markrecap.ui.ConfirmEditDialog
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.RecentObservations
import weddellseal.markrecap.ui.UiEvent
import weddellseal.markrecap.ui.UiEvent.ShowEditDialog
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentObservationsScreen(
    navController: NavHostController,
    viewModel: RecentObservationsViewModel,
    tagRetagViewModel: TagRetagViewModel,
) {
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val uiEventFlow = tagRetagViewModel.uiEvent

    val observationToEdit by tagRetagViewModel.selectedRecentObservation.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowEditToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }

                is ShowEditDialog -> {
                    showEditDialog = true
                }

                else -> Unit // Ignore other events
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
            topBar = {
                AppBar(
                    onNavigationIconClick = {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    },
                    "Recent Observations"
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 40.dp, end = 40.dp, bottom = 40.dp)
                        .fillMaxSize()
                        .border(4.dp, Color.LightGray)
                ) {
                    RecentObservations(tagRetagViewModel, viewModel, navController)
                }
                // Show the dialog if showDialog is true
                if (showEditDialog) {
                    ConfirmEditDialog(
                        onDismissRequest = {
                            showEditDialog = false
                        },
                        onConfirmation = {
                            showEditDialog = false

                            observationToEdit?.let { record ->
                                tagRetagViewModel.resetModelState()
                                tagRetagViewModel.loadSealForEdit(record)
                                navController.navigate(Screens.TagRetag.route)
                            }
                        },
                    )
                }
            }
        }
    }
}
