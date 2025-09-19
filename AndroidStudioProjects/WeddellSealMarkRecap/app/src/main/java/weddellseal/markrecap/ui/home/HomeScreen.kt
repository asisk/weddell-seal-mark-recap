package weddellseal.markrecap.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.R
import weddellseal.markrecap.ui.CenteredAppBar
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.permissions.RequestPermissions
import weddellseal.markrecap.ui.permissions.missingPermissions
import weddellseal.markrecap.ui.utils.cancelAllAndClear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Used to request permissions for Location
    RequestPermissionsEffect(viewModel)

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
                    painter = painterResource(R.drawable.thirtytwoyearold),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    DeviceGPSRow(viewModel)
                    Spacer(modifier = Modifier.height(10.dp))

                    DeviceIDRow()
                    Spacer(modifier = Modifier.height(10.dp))

                    HomeScreenCard(viewModel)
                }
            }
        }
    }
}

@Composable
private fun RequestPermissionsEffect(
    vm: HomeViewModel,
) {
    val missing = LocalContext.current.missingPermissions()
    if (missing.isEmpty()) {
        vm.onPermissionsResult(true)
        return
    }
    RequestPermissions(missing, vm::onPermissionsResult)
}