package weddellseal.markrecap.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import weddellseal.markrecap.Screens
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.ui.home.HomeViewModel

/**
 * Hosts a single movable [MapLibreMapView] that stays composed when leaving Map
 * (parked invisible behind the nav host) so GL / style / camera are not torn down.
 */
@Composable
fun WarmMapNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    sealColonyRepository: SealColonyRepository,
    warmMap: WarmMapViewModel,
    navGraph: @Composable (mapContent: @Composable () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onMap = backStackEntry?.destination?.route == Screens.Map.route

    LaunchedEffect(onMap) {
        if (onMap) {
            warmMap.ensurePackPrepared(context)
        }
    }

    val movableMap = remember(homeViewModel, sealColonyRepository, warmMap) {
        movableContentOf { visible: Boolean ->
            val location by homeViewModel.currentLocation.collectAsState()
            val autoDetectedColony by homeViewModel.autoDetectedColony.collectAsState()
            val colonies by sealColonyRepository.colonies.collectAsState(initial = emptyList())
            val drawable = colonies.drawableColonies()
            val activeName = activeGpsColonyName(autoDetectedColony)
            val file = warmMap.styleFile
            if (file != null) {
                MapLibreMapView(
                    warmMap = warmMap,
                    styleFile = file,
                    colonies = drawable,
                    activeColonyName = activeName,
                    currentLocation = location,
                    followLiveLocation = warmMap.followLive,
                    myLocationRequest = warmMap.myLocationRequest,
                    centerMcMurdoRequest = warmMap.centerMcMurdoRequest,
                    centerBozemanRequest = warmMap.centerBozemanRequest,
                    zoomInRequest = warmMap.zoomInRequest,
                    zoomOutRequest = warmMap.zoomOutRequest,
                    mapVisible = visible,
                    onCameraMovedByUser = { warmMap.followLive = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (warmMap.keepAlive && !onMap) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0f),
            ) {
                movableMap(false)
            }
        }
        navGraph(
            {
                if (onMap) {
                    movableMap(true)
                }
            },
        )
    }
}
