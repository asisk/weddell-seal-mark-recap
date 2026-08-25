package weddellseal.markrecap.ui.home

import android.content.res.Configuration
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens
import weddellseal.markrecap.ui.CenteredAppBar
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.permissions.locationPermissionsGranted
import weddellseal.markrecap.ui.utils.scaffoldContentInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var locationGranted by remember { mutableStateOf(context.locationPermissionsGranted()) }

    // Re-check location permission on resume (e.g. after returning from the system prompt).
    // Do not cancel location jobs here — that previously stopped updates for other screens.
    DisposableEffect(lifecycleOwner, viewModel) {
        fun syncLocation() {
            val granted = context.locationPermissionsGranted()
            locationGranted = granted
            if (granted) {
                viewModel.onPermissionsResult(true)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            syncLocation()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                    painter = painterResource(R.drawable.thirtytwoyearold),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.6f // Adjust this value for desired transparency
                        }
                )

                val isLandscape =
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .scaffoldContentInsets(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 20.dp))

                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DeviceGPSRow(
                                viewModel = viewModel,
                                locationGranted = locationGranted,
                                onEnableLocation = {
                                    navController.navigate(Screens.LocationPermissions.route)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DeviceIDRow(Modifier.weight(1f))
                        }
                    } else {
                        DeviceGPSRow(
                            viewModel = viewModel,
                            locationGranted = locationGranted,
                            onEnableLocation = {
                                navController.navigate(Screens.LocationPermissions.route)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp, end = 30.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DeviceIDRow(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp, end = 30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 10.dp))

                    if (isLandscape) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HomeScreenCard(viewModel, isLandscape = true)
                        }
                    } else {
                        HomeScreenCard(viewModel)
                    }
                }
            }
        }
    }
}
