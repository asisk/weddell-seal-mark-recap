package weddellseal.markrecap.ui.lookup

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.SealLookupViewModel
import weddellseal.markrecap.models.TagRetagModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealLookupScreen(
    navController: NavHostController,
    viewModel: SealLookupViewModel,
    obsViewModel: TagRetagModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.sealNotFound) {
        if (uiState.sealNotFound) {
            scope.launch { snackbarHostState.showSnackbar("Seal not found!") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Seal Lookup",
                            style = MaterialTheme.typography.titleLarge,
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
                .fillMaxSize()
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
                    .fillMaxWidth()
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
                        var sealTagID by remember { mutableStateOf("") }
                        var focusManager = LocalFocusManager.current

                        SealSearchField(sealTagID, viewModel) { newText ->
                            sealTagID = newText
                        }

                        if (uiState.sealFound) {
                            ExtendedFloatingActionButton(
                                modifier = Modifier
                                    .padding(bottom = 20.dp, start = 20.dp)
                                    .fillMaxWidth(),
                                containerColor = Color.LightGray,
                                onClick = {
                                    obsViewModel.populateSeal(viewModel.lookupSeal.value)
                                    viewModel.setTagRetagLookup(true)
                                    navController.navigate(Screens.AddObservationLog.route)
                                },
                                icon = { Icon(Icons.Filled.PostAdd, "Edit seal") },
                                text = {
                                    Text(
                                        text = "Tag/Retag",
                                        fontSize = 18.sp, // Set your desired text size here
                                        fontWeight = FontWeight.Bold, // Optional: set the font weight
                                        color = Color.Black // Optional: set the text color
                                    )
                                }
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.findSealbyTagID(sealTagID)
                                },
                                modifier = Modifier.padding(bottom = 15.dp, end = 20.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(45.dp)
                                )
                            }
                        }
                    }
                }
                LookupCard(viewModel.lookupSeal.collectAsState().value)
            }
        }
    }
}