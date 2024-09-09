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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.SealSearchField
import weddellseal.markrecap.ui.components.WedCheckCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealLookupScreen(
    navController: NavHostController,
    wedCheckViewModel: WedCheckViewModel,
    obsViewModel: AddObservationLogViewModel
) {
    val uiStateWedCheck by wedCheckViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Seal Lookup",
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
            ) { // SEAL LOOKUP
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
                        if (uiStateWedCheck.isError) {
                            SealNotFoundToast()
                        }

                        SealSearchField(sealTagID, wedCheckViewModel) { newText ->
                            sealTagID = newText
                        }

                        if (!wedCheckViewModel.wedCheckSeal.found) {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    // reset the current seal & start a new search
                                    wedCheckViewModel.resetState()

                                    wedCheckViewModel.findSealbyTagID(sealTagID)

                                },
                                modifier = Modifier.padding(bottom = 15.dp, end = 20.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(45.dp)
                                )
                            }
                        } else {
                            ExtendedFloatingActionButton(
                                modifier = Modifier
                                    .padding(bottom = 20.dp, start = 20.dp)
                                    .fillMaxWidth(),
                                containerColor = Color.LightGray,
                                onClick = {
                                    obsViewModel.populateSeal(wedCheckViewModel.wedCheckSeal)
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
                        }
                    }
                }
                WedCheckCard(wedCheckViewModel.wedCheckSeal)
            }
        }
    }
}

@Composable
fun SealNotFoundToast() {
    val context = LocalContext.current
    Toast.makeText(context, "Seal not found", Toast.LENGTH_LONG).show()
}