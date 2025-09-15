package weddellseal.markrecap.ui.census

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.ui.CenteredAppBar
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.utils.cancelAllAndClear

@Composable
fun CensusScreen(
    navController: NavHostController,
    viewModel: HomeViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val metadata by viewModel.metadata.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.jobs.cancelAllAndClear()
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
                CenteredAppBar(
                    onNavigationIconClick = {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                )
            },
        ) { innerPadding ->

            Box(modifier = Modifier.fillMaxSize()) {

                // Background Image
                Image(
                    painter = painterResource(R.drawable.erebus_bay_map),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.6f // Adjust this value for desired transparency
                        }
                )

                // Main Content
                Column(
                    modifier = Modifier.padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {

                    Spacer(modifier = Modifier.height(36.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(375.dp)
                            .padding(start = 72.dp, end = 72.dp, top = 24.dp, bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {

                            val options = listOf("1", "2", "3", "4", "5", "6", "7", "8")
                            CensusDropDown(
                                label = "Census Number",
                                options = options,
                                selectedOption = metadata.censusNumber,
                                onValueChange = {
                                    viewModel.updateCensusNumber(it)
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                val elevationDisabled = FloatingActionButtonDefaults.elevation(2.dp)
                                val elevationEnabled = FloatingActionButtonDefaults.elevation(8.dp)
                                val colorDisabled = MaterialTheme.colorScheme.surface
                                val colorEnabled = MaterialTheme.colorScheme.secondary

                                // EXIT CENSUS BUTTON - only enabled if in a census
                                ExtendedFloatingActionButton(
                                    modifier = Modifier.padding(10.dp),
                                    elevation = if (metadata.isCensusMode) elevationEnabled else elevationDisabled,
                                    containerColor = if (metadata.isCensusMode) colorEnabled else colorDisabled,
                                    onClick = {
                                        if (!metadata.isCensusMode) return@ExtendedFloatingActionButton  // guard early exit

                                        viewModel.clearCensus()
                                        navController.navigate(Screens.TagRetag.route)
                                    },
                                    icon = { /* no icon */ },
                                    text = {
                                        Text(
                                            text = "Exit Census",
                                            style = MaterialTheme.typography.headlineMedium,
                                        )
                                    }
                                )

                                // BEGIN CENSUS BUTTON - only enabled if census number is entered
                                ExtendedFloatingActionButton(
                                    modifier = Modifier.padding(10.dp),
                                    elevation = if (metadata.censusNumber.isNotEmpty()) elevationEnabled else elevationDisabled,
                                    containerColor = if (metadata.censusNumber.isNotEmpty()) colorEnabled else colorDisabled,
                                    onClick = {
                                        if (metadata.censusNumber.isEmpty()) return@ExtendedFloatingActionButton  // guard early exit

                                        viewModel.updateIsCensusMode(true)
                                        navController.navigate(Screens.TagRetag.route)
                                    },
                                    icon = { /* no icon */ },
                                    text = {
                                        Text(
                                            text = "Go to Census",
                                            style = MaterialTheme.typography.headlineMedium,
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}