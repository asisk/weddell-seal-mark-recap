package weddellseal.markrecap.ui.recentobservations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.ui.tagretag.TagRetagModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationViewer(
    navController: NavHostController,
    tagRetagModel: TagRetagModel
) {

    Scaffold(
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
                            "Tag/Retag Viewer",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 36.sp // Adjust this value as needed
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Back",
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
            ObservationTabbedCards(tagRetagModel)
//                        ExtendedFloatingActionButton(
//                            modifier = Modifier
//                                .padding(bottom = 20.dp, start = 20.dp)
//                                .fillMaxWidth(),
//                            containerColor = Color.LightGray,
//                            onClick = {
////                                    obsViewModel.populateSeal(wedCheckViewModel.wedCheckSeal)
//                                navController.navigate(Screens.AddObservationLog.route)
//                            },
//                            icon = { Icon(Icons.Filled.PostAdd, "Edit seal") },
//                            text = {
//                                Text(
//                                    text = "Tag/Retag",
//                                    fontSize = 18.sp, // Set your desired text size here
//                                    fontWeight = FontWeight.Bold, // Optional: set the font weight
//                                    color = Color.Black // Optional: set the text color
//                                )
//                            }
//                        )
        }
    }
}
