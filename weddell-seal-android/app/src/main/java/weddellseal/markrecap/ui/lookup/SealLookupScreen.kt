package weddellseal.markrecap.ui.lookup

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.ui.AppBar
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel
import weddellseal.markrecap.ui.utils.scaffoldContentInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealLookupScreen(
    navController: NavHostController,
    viewModel: SealLookupViewModel,
    homeViewModel: HomeViewModel,
    tagRetagViewModel: TagRetagViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.sealNotFound) {
        if (uiState.sealNotFound) {
            scope.launch { snackBarHostState.showSnackbar("Seal not found!") }
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
                AppBar(
                    onNavigationIconClick = {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    },
                    "Seal Lookup"
                )
            },

            ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .scaffoldContentInsets(innerPadding)
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    // SEAL LOOKUP
                    Column(
                        modifier = Modifier
                            .padding(30.dp)
                            .verticalScroll(state = scrollState, enabled = true)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(6.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            SealSearchField(viewModel)

                            if (uiState.sealFound) {
                                ExtendedFloatingActionButton(
                                    modifier = Modifier
                                        .padding(bottom = 20.dp, start = 20.dp)
                                        .fillMaxWidth(),
                                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    onClick = {
                                        if (!tagRetagViewModel.primarySeal.value.isEntryStarted) {
                                            tagRetagViewModel.populateSealFromLookup(viewModel.lookupSeal.value)
                                            navController.navigate(Screens.TagRetag.route)
                                        } else {
                                            // Show a Toast message if the seal is already started
                                            Toast.makeText(
                                                context,
                                                "Looks like you're already editing another seal! Finish or delete to edit this record.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_post_add),
                                            contentDescription = "Edit seal",
                                            modifier = Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                        )
                                    },
                                    text = {
                                        Text(
                                            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                                            text = "Tag/Retag",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                )
                            }
                        }
                    }
                    // Remaining height after the search row, so results stay on-screen
                    // (including on phone-sized CI emulators with the keyboard IME inset).
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LookupCard(viewModel.lookupSeal.collectAsState().value, homeViewModel)
                    }
                }
            }
        }
    }
}