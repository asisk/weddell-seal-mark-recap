package weddellseal.markrecap.ui.tagretag

/*
 * Main screen for entering seal data
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.UiEvent
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

/** Semantics test tag for the Tag/Retag scroll container; used to assert scroll-to-top after save. */
const val TAG_RETAG_SCROLL_TEST_TAG = "tag_retag_scroll"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRetagScreen(
    navController: NavHostController,
    viewModel: TagRetagViewModel,
    homeViewModel: HomeViewModel,
    recentObsViewModel: RecentObservationsViewModel
) {
    val uiEventFlow = viewModel.uiEvent

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }

    val primarySeal by viewModel.primarySeal.collectAsState()
    val pupOneSeal by viewModel.pupOne.collectAsState()
    val pupTwoSeal by viewModel.pupTwo.collectAsState()
    val fieldResetCounter by remember {
        viewModel.uiState.map { it.fieldResetCounter }
    }.collectAsState(initial = viewModel.uiState.value.fieldResetCounter)
    val scrollState = rememberScrollState()
    var isInitialScrollEffect by remember { mutableStateOf(true) }

    // After save (and any other resetModelState), return to the top so the next
    // seal starts at Age / Tag Event instead of remaining scrolled to Save.
    // Skip the first run so a configuration change does not jump to the top.
    LaunchedEffect(fieldResetCounter) {
        if (isInitialScrollEffect) {
            isInitialScrollEffect = false
            return@LaunchedEffect
        }
        scrollState.scrollTo(0)
    }

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
            ) {

                // This row stays fixed, not scrollable
                TagRetagHeader(viewModel, homeViewModel)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .testTag(TAG_RETAG_SCROLL_TEST_TAG)
                ) {
                    // SEAL CARDS
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TabbedCards(viewModel, homeViewModel,primarySeal, pupOneSeal, pupTwoSeal)
                    }

                    TagRetagFooter(viewModel, homeViewModel, recentObsViewModel, navController)
                }
            }
        }
    }
}

