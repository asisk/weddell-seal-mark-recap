package weddellseal.markrecap

/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme



@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {

    //LaunchedEffect(Unit) { viewModel.loadLogs() }
    val state = viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {

    }

    mainScaffold(navController, viewModel)
}
//    Scaffold(
//        topBar = { TopAppBar(title = { Text("Observations") }) },
//        floatingActionButtonPosition = FabPosition.End,
//        floatingActionButton = { FloatingActionButton(onClick = {}) { Text("X") } },
//        floatingActionButton = { FloatingActionButton(onClick = {}) { Text("X") } },
//        content = { innerPadding ->
//            Column(
//                Modifier
//                    .fillMaxSize()
//                    .padding(innerPadding))
//            {
//                Card {
//                    Row(Modifier.padding(8.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            text = "text",
//                            modifier = Modifier.weight(1f),
//                            style = MaterialTheme.typography.headlineSmall
//                        )
//                        IconButton(onClick = { navController.navigate(Screens.WriteObservationsToCSV.route)  }) {
//                            Icon(
//                                imageVector = Icons.Filled.Build,
//                                contentDescription = "Build CSV File"
//                            )
//                        }
//                    }
//                }
//            }
//        },
//        bottomBar = { BottomAppBar() { Text("BottomAppBar") } })
//    }
//    Scaffold(
//        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
//        topBar = { TopAppBar( title = { Text("Observations", fontFamily = FontFamily.Serif) }, scrollBehavior = scrollBehavior) },
//        content = {
//            LogCard(
//            modifier = Modifier
//                .fillMaxWidth(),
//            observationLog = log,
//            formattedDate = viewModel.formatDateTime(log.timeInMillis),
//            onDelete = viewModel::delete
//        ) },
//        floatingActionButton = {
//            FloatingActionButton(onClick = { navController.navigate(Screens.AddObservationLog.route) }) {
//                Icon(Icons.Filled.Add, "Add observation")
//            }
//        })
//    ) { innerPadding ->
//        LazyColumn(
//            Modifier
//                .fillMaxSize()
//                .padding(innerPadding),
//            contentPadding = PaddingValues(8.dp)
//        ) {
//            if (!state.loading && state.observationLogs.isEmpty()) {
//                item {
//                    EmptyLogMessage(Modifier.fillParentMaxSize())
//                }
//            }
//            items(state.observationLogs, key = { it.date }) { log ->
//                LogCard(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .animateItemPlacement(),
//                    observationLog = log,
//                    formattedDate = viewModel.formatDateTime(log.timeInMillis),
//                    onDelete = viewModel::delete
//                )
//                Spacer(Modifier.height(16.dp))
//            }
//        }
//    }
//}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScaffold(navController: NavHostController, viewModel: HomeViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = { Text("Observations", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(16.dp),
                onClick = { (viewModel.exportLogs())},
                icon = {Icon(Icons.Filled.Build, "Build CSV File")},
                text = {Text(text = "Build CSV File")}
            )
        },
        floatingActionButtonPosition = FabPosition.Start,
        bottomBar = {
            Text(
                text = "",
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row {
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    onClick = { (navController.navigate(Screens.AddObservationLog.route)) },
                    icon = { Icon(Icons.Filled.PostAdd, "Add Observation") },
                    text = { Text(text = "Add Observation") })
            }
            Row (
                verticalAlignment = Alignment.Bottom
            ){
            }
        }
    }


}


//PhotoGrid(Modifier.padding(16.dp), photos = observationLog.photos)
@Composable
fun EmptyLogMessage(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Hi there \uD83D\uDC4B",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif
        )
       Spacer(Modifier.height(16.dp))
        Text(
            "Create a seal observation log by clicking the ✚ icon below \uD83D\uDC47",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview
@Composable
fun HomeScreen() {
    WeddellSealMarkRecapTheme {
        val navController = rememberNavController()
        val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
        mainScaffold(navController, viewModel)
    }
}