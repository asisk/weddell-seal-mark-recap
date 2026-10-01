package weddellseal.markrecap.ui.map

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.R
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.ui.CenteredAppBar
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.utils.scaffoldContentInsets

/** Compact square size for map overlay controls (zoom / location / north). */
private val MapControlSize = 40.dp
private val MapControlShape = RoundedCornerShape(8.dp)

/** Matches MapLibre colony *outline* colors in [MapLibreMapView]. */
private object ColonyLegendColors {
    val Inside = Color(0xFF1976D2) // rgb(25, 118, 210)
    val Outside = Color(0xFF757575) // rgb(117, 117, 117)
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
                            )
                        }
                        status.tertiary?.let {
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColonyFlyDropDown(
                            colonies = drawable,
                            selectedName = warmMap.lastFlownColonyName,
                            onColonySelected = { warmMap.requestFlyToColony(it) },
                            modifier = Modifier.widthIn(min = 140.dp, max = 200.dp),
                        )
                        MapControlButton(
                            onClick = { warmMap.requestMcMurdo() },
                            contentDescription = MapScreenUi.CENTER_MCMURDO,
                            modifier = Modifier.height(MapControlSize),
                        ) {
                            Text(
                                text = MapScreenUi.CENTER_MCMURDO,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                        MapControlButton(
                            onClick = { warmMap.requestBozeman() },
                            contentDescription = MapScreenUi.CENTER_BOZEMAN,
                            modifier = Modifier.height(MapControlSize),
                        ) {
                            Text(
                                text = MapScreenUi.CENTER_BOZEMAN,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
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
                                NorthIndicator(
                                    bearingDegrees = warmMap.mapBearing,
                                    onClick = { warmMap.requestResetNorth() },
                                )
                                MapControlButton(
                                    onClick = { warmMap.requestZoomIn() },
                                    contentDescription = MapScreenUi.ZOOM_IN,
                                ) {
                                    Text(
                                        text = MapScreenUi.ZOOM_IN,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                MapControlButton(
                                    onClick = { warmMap.requestZoomOut() },
                                    contentDescription = MapScreenUi.ZOOM_OUT,
                                ) {
                                    Text(
                                        text = MapScreenUi.ZOOM_OUT,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                MapControlButton(
                                    onClick = {
                                        warmMap.requestMyLocation()
                                        if (location?.isLiveFix != true) {
                                            homeViewModel.refreshGps()
                                        }
                                    },
                                    contentDescription = MapScreenUi.MY_LOCATION,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_location_on),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColonyFlyDropDown(
    colonies: List<SealColony>,
    selectedName: String?,
    onColonySelected: (SealColony) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(colonies) {
        colonies.drawableColonies().sortedBy { it.location.lowercase() }
    }
    val enabled = options.isNotEmpty()
    val label = selectedName?.takeIf { it.isNotBlank() }
        ?: MapScreenUi.FLY_TO_COLONY_PLACEHOLDER

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        TextField(
            readOnly = true,
            enabled = enabled,
            value = label,
            onValueChange = {},
            label = {
                Text(
                    MapScreenUi.FLY_TO_COLONY,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { colony ->
                DropdownMenuItem(
                    text = {
                        Text(
                            colony.location,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onColonySelected(colony)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MapControlButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier.size(MapControlSize),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(MapControlShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MapControlShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun NorthIndicator(
    bearingDegrees: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapControlButton(
        onClick = onClick,
        contentDescription = MapScreenUi.RESET_NORTH,
        modifier = modifier.size(MapControlSize),
    ) {
        // Draw under a canvas rotate (not Modifier.rotate on a bitmap) so edges stay anti-aliased.
        Canvas(modifier = Modifier.size(24.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val tip = size.minDimension * 0.42f
            val waist = size.minDimension * 0.16f
            rotate(degrees = -bearingDegrees.toFloat(), pivot = Offset(cx, cy)) {
                val north = Path().apply {
                    moveTo(cx, cy - tip)
                    lineTo(cx + waist, cy)
                    lineTo(cx, cy - waist * 0.55f)
                    lineTo(cx - waist, cy)
                    close()
                }
                val south = Path().apply {
                    moveTo(cx, cy + tip)
                    lineTo(cx - waist, cy)
                    lineTo(cx, cy + waist * 0.55f)
                    lineTo(cx + waist, cy)
                    close()
                }
                drawPath(south, color = Color(0xFF616161))
                drawPath(north, color = Color(0xFFE53935))
            }
        }
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
        LegendRow(
            fill = Color.Transparent,
            borderColor = ColonyLegendColors.Inside,
            label = MapScreenUi.LEGEND_INSIDE,
        )
        LegendRow(
            fill = Color.Transparent,
            borderColor = ColonyLegendColors.Outside,
            label = MapScreenUi.LEGEND_OUTSIDE,
        )
        LegendRow(
            fill = Color.Transparent,
            borderColor = ColonyLegendColors.Active,
            label = MapScreenUi.LEGEND_ACTIVE,
        )
    }
}

@Composable
private fun LegendRow(
    fill: Color,
    label: String,
    borderColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .border(1.5.dp, borderColor, RoundedCornerShape(2.dp))
                .background(fill, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
