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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.ui.components.SealSearchField
import weddellseal.markrecap.ui.components.WedCheckCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealLookupScreen(
    navController: NavHostController,
    wedCheckViewModel: WedCheckViewModel
) {
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
                        navController.navigateUp()
                        wedCheckViewModel.resetState()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screens.HomeScreen.route)
                        wedCheckViewModel.resetState()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(36.dp)
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
                Row(
                    modifier = Modifier
                        .padding(6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var sealTagID by remember { mutableStateOf("") }

                    if (wedCheckViewModel.uiState.isError) {
                        SealNotFoundToast()
                    }

                    SealSearchField(wedCheckViewModel) { newText ->
                        sealTagID = newText
                    }

                    IconButton(
                        onClick = {
                            // reset the current seal & start a new search
                            wedCheckViewModel.resetState()
                            wedCheckViewModel.findSeal(sealTagID)
                        },
                        modifier = Modifier.padding(bottom = 15.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(45.dp)
                        )
                    }
                }
            }
            val scrollState = rememberScrollState()
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
                    .verticalScroll(state = scrollState, enabled = true)
            ) {
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 90.dp, end = 90.dp, top = 10.dp, bottom = 10.dp) // Apply padding to left and right
                ) {
                    WedCheckCard(wedCheckViewModel.wedCheckSeal)
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