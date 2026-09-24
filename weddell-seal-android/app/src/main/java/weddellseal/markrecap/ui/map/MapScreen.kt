package weddellseal.markrecap.ui.map

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.R
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.ui.CenteredAppBar
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.utils.scaffoldContentInsets

/** Compact square size for map overlay controls (zoom / location / north). */
private val MapControlSize = 40.dp
private val MapControlShape = RoundedCornerShape(8.dp)

/** Matches MapLibre colony fill/outline colors in [MapLibreMapView]. */
private object ColonyLegendColors {
    val Inside = Color(0xE32196F3)
    val Outside = Color(0xE39E9E9E)
    val Local = Color(0xE39C27B0)
    val Active = Color(0xFFFF9800)
}

@Composable
fun MapScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    sealColonyRepository: SealColonyRepository,
    warmMap: WarmMapViewModel,
    mapContent: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // MapLibre does not survive Activity recreate on rotation well; lock portrait
    // while this screen is shown and restore the previous mode on leave.
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = previous
        }
    }

    val location by homeViewModel.currentLocation.collectAsState()
    val autoDetectedColony by homeViewModel.autoDetectedColony.collectAsState()
    val uiState by homeViewModel.uiState.collectAsState()
    val metadata by homeViewModel.metadata.collectAsState()
    val colonies by sealColonyRepository.colonies.collectAsState(initial = emptyList())

    val preparing = warmMap.preparing
    val styleFile = warmMap.styleFile
    val packError = warmMap.packError

    // First cold open: frame GPS region once. Warm revisits keep the parked camera.
    LaunchedEffect(styleFile) {
        val file = styleFile ?: return@LaunchedEffect
        if (warmMap.isStyleLoaded(file)) return@LaunchedEffect
        val live = location
        when {
            live?.isLiveFix == true &&
                MapTileEnvelope.contains(
                    live.coordinates.latitude,
                    live.coordinates.longitude,
                ) -> {
                warmMap.followLive = true
            }
            live != null &&
                BozemanMapEnvelope.contains(
                    live.coordinates.latitude,
                    live.coordinates.longitude,
                ) -> {
                warmMap.requestMyLocation()
            }
            else -> {
                warmMap.followLive = false
            }
        }
    }

    val status = mapStatusLines(
        location = location,
        isRefreshingGps = uiState.isRefreshingGps,
        autoDetectedColony = autoDetectedColony,
        overrideColony = uiState.overrideColony,
        selectedColony = metadata.selectedColony,
    )
    val drawable = remember(colonies) { colonies.drawableColonies() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = { NavMenu(navController) },
    ) {
        Scaffold(
            topBar = {
                CenteredAppBar(
                    onNavigationIconClick = {
                        scope.launch { drawerState.open() }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .scaffoldContentInsets(innerPadding),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = status.primary,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        status.secondary?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (drawable.isEmpty()) {
                            Text(
                                text = MapScreenUi.EMPTY_COLONIES,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { warmMap.requestMcMurdo() }) {
                            Text(MapScreenUi.CENTER_MCMURDO)
                        }
                        Button(onClick = { warmMap.requestBozeman() }) {
                            Text(MapScreenUi.CENTER_BOZEMAN)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when {
                        preparing && styleFile == null -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = MapScreenUi.PREPARING_MAP,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }

                        styleFile == null -> {
                            Text(
                                text = packError ?: MapScreenUi.PACK_MISSING,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        else -> {
                            mapContent()
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                NorthIndicator()
                                MapZoomButton(
                                    label = MapScreenUi.ZOOM_IN,
                                    onClick = { warmMap.requestZoomIn() },
                                )
                                MapZoomButton(
                                    label = MapScreenUi.ZOOM_OUT,
                                    onClick = { warmMap.requestZoomOut() },
                                )
                                IconButton(
                                    onClick = {
                                        warmMap.requestMyLocation()
                                        if (location?.isLiveFix != true) {
                                            homeViewModel.refreshGps()
                                        }
                                    },
                                    modifier = Modifier.size(MapControlSize),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_location_on),
                                        contentDescription = MapScreenUi.MY_LOCATION,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            ColonyMapLegend(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { uriHandler.openUri(MapScreenUi.ATTRIBUTION_URL) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = MapScreenUi.ATTRIBUTION,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun NorthIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .size(MapControlSize)
            .clip(MapControlShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MapControlShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_north_arrow),
            contentDescription = MapScreenUi.NORTH,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MapZoomButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(MapControlSize),
        shape = MapControlShape,
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ColonyMapLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = MapScreenUi.LEGEND_TITLE,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LegendRow(color = ColonyLegendColors.Inside, label = MapScreenUi.LEGEND_INSIDE)
        LegendRow(color = ColonyLegendColors.Outside, label = MapScreenUi.LEGEND_OUTSIDE)
        LegendRow(color = ColonyLegendColors.Local, label = MapScreenUi.LEGEND_LOCAL)
        LegendRow(
            color = Color.Transparent,
            label = MapScreenUi.LEGEND_ACTIVE,
            borderColor = ColonyLegendColors.Active,
        )
    }
}

@Composable
private fun LegendRow(
    color: Color,
    label: String,
    borderColor: Color = Color(0xFF424242),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .border(1.5.dp, borderColor, RoundedCornerShape(2.dp))
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
