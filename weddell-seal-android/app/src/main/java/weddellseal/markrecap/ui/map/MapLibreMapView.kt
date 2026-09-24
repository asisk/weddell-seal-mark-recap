package weddellseal.markrecap.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.frameworks.map.OfflineMapAssets
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import java.io.File

private const val SOURCE_COLONIES = "colonies-source"
private const val SOURCE_LABELS = "colony-labels-source"
private const val SOURCE_DEVICE = "device-location-source"
private const val LAYER_FILL = "colonies-fill"
private const val LAYER_LINE = "colonies-line"
private const val LAYER_LABELS = "colony-labels"
private const val LAYER_DEVICE = "device-location"
/** Style anchors for stack: basemap → colonies → lat/long → COMNAP → glyphs. */
private const val LAYER_GRATICULE = "graticule"
private const val LAYER_COMNAP_FACILITIES = "comnap-facilities"
private const val LAYER_COMNAP_LABELS = "comnap-labels"
private const val ICON_RESEARCH_STATION = "research-station"

@Composable
fun MapLibreMapView(
    warmMap: WarmMapViewModel,
    styleFile: File?,
    colonies: List<SealColony>,
    activeColonyName: String?,
    currentLocation: GeoLocation?,
    followLiveLocation: Boolean,
    myLocationRequest: Int,
    centerMcMurdoRequest: Int,
    centerBozemanRequest: Int,
    zoomInRequest: Int,
    zoomOutRequest: Int,
    mapVisible: Boolean,
    onCameraMovedByUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? ComponentActivity
        ?: error("MapLibreMapView requires a ComponentActivity")
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(warmMap) { warmMap.obtainMapView(activity) }
    var mapRef by remember { mutableStateOf(warmMap.mapLibreMap()) }
    var styleReady by remember {
        mutableStateOf(styleFile?.let { warmMap.isStyleLoaded(it) } == true)
    }
    var lastHandledMyLocationRequest by remember { mutableIntStateOf(0) }
    var myLocationFlightActive by remember { mutableStateOf(false) }

    SideEffect {
        warmMap.onCameraMovedByUser = onCameraMovedByUser
    }

    // Activity lifecycle only — do not tear down when Map nav destination leaves.
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (mapVisible) mapView.onStart()
                Lifecycle.Event.ON_RESUME -> if (mapVisible) mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> {
                    // Avoid onStop while keeping the map warm across screens; Activity
                    // finish still destroys via WarmMapViewModel.onCleared().
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mapVisible) {
        warmMap.setMapVisible(mapVisible)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { size ->
                    if (size.width <= 0 || size.height <= 0) return@onSizeChanged
                    if (mapView.width == size.width && mapView.height == size.height) {
                        return@onSizeChanged
                    }
                    mapView.post {
                        mapView.measure(
                            View.MeasureSpec.makeMeasureSpec(size.width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(size.height, View.MeasureSpec.EXACTLY),
                        )
                        mapView.layout(0, 0, size.width, size.height)
                    }
                },
            update = { /* MapView is stateful; updates via LaunchedEffects below */ },
        )
    }

    LaunchedEffect(styleFile) {
        val file = styleFile ?: return@LaunchedEffect
        if (warmMap.isStyleLoaded(file)) {
            val map = warmMap.mapLibreMap()
            mapRef = map
            if (map != null) warmMap.ensureCameraMoveListener(map)
            styleReady = true
            return@LaunchedEffect
        }
        mapView.getMapAsync { map ->
            warmMap.setMapLibreMap(map)
            mapRef = map
            if (!warmMap.isMapConfigured()) {
                map.setMinZoomPreference(MapTileEnvelope.MIN_ZOOM)
                map.setMaxZoomPreference(
                    maxOf(MapTileEnvelope.MAX_ZOOM, BozemanMapEnvelope.MAX_ZOOM),
                )
                map.uiSettings.isRotateGesturesEnabled = false
                map.uiSettings.isTiltGesturesEnabled = false
                map.setLatLngBoundsForCameraTarget(packCameraBounds())
                warmMap.markMapConfigured()
            }
            warmMap.ensureCameraMoveListener(map)

            map.setStyle(Style.Builder().fromUri(OfflineMapAssets.styleUri(file))) { style ->
                addResearchStationIcon(style, file.parentFile)
                ensureOverlayLayers(style)
                warmMap.markStyleLoaded(file)
                styleReady = true
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(packCameraBounds(), 48),
                    1,
                )
            }
        }
    }

    LaunchedEffect(styleReady, colonies, activeColonyName) {
        if (!styleReady) return@LaunchedEffect
        val style = mapRef?.style ?: return@LaunchedEffect
        updateColonySources(style, colonies, activeColonyName)
    }

    LaunchedEffect(styleReady, currentLocation, followLiveLocation) {
        if (!styleReady) return@LaunchedEffect
        val map = mapRef ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        updateDeviceSource(style, currentLocation)
        // Soft-follow: recenter only (keep zoom). Skip while My Location is flying —
        // a concurrent animateCamera would cancel the zoom-to-16 jump.
        if (myLocationFlightActive) return@LaunchedEffect
        if (followLiveLocation && currentLocation?.isLiveFix == true) {
            val coords = currentLocation.coordinates
            val region = regionFor(coords.latitude, coords.longitude)
            if (region == CameraRegion.Unlocked) return@LaunchedEffect
            when (region) {
                CameraRegion.Antarctic -> applyAntarcticCameraLock(map)
                CameraRegion.Bozeman -> applyBozemanCameraLock(map)
                CameraRegion.Unlocked -> Unit
            }
            map.animateCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(coords.latitude, coords.longitude),
                ),
            )
        }
    }

    // Wait for a location fix if needed (request alone may fire before GPS refresh).
    LaunchedEffect(myLocationRequest, styleReady, currentLocation) {
        if (myLocationRequest == 0 || !styleReady) return@LaunchedEffect
        if (myLocationRequest == lastHandledMyLocationRequest) return@LaunchedEffect
        val map = mapRef ?: return@LaunchedEffect
        val location = currentLocation ?: return@LaunchedEffect
        lastHandledMyLocationRequest = myLocationRequest
        myLocationFlightActive = true
        flyToDeviceLocation(
            map = map,
            latitude = location.coordinates.latitude,
            longitude = location.coordinates.longitude,
            zoom = MapScreenUi.MY_LOCATION_ZOOM,
            animate = true,
            onComplete = { myLocationFlightActive = false },
        )
    }

    LaunchedEffect(centerMcMurdoRequest, styleReady) {
        if (centerMcMurdoRequest == 0 || !styleReady) return@LaunchedEffect
        val map = mapRef ?: return@LaunchedEffect
        flyToRegion(
            map = map,
            latitude = MapScreenUi.MCMURDO_LATITUDE,
            longitude = MapScreenUi.MCMURDO_LONGITUDE,
            zoom = MapScreenUi.MCMURDO_ZOOM,
            region = CameraRegion.Antarctic,
        )
    }

    LaunchedEffect(centerBozemanRequest, styleReady) {
        if (centerBozemanRequest == 0 || !styleReady) return@LaunchedEffect
        val map = mapRef ?: return@LaunchedEffect
        flyToRegion(
            map = map,
            latitude = BozemanMapEnvelope.CENTER_LATITUDE,
            longitude = BozemanMapEnvelope.CENTER_LONGITUDE,
            zoom = BozemanMapEnvelope.CENTER_ZOOM,
            region = CameraRegion.Bozeman,
        )
    }

    LaunchedEffect(zoomInRequest, styleReady) {
        if (zoomInRequest == 0 || !styleReady) return@LaunchedEffect
        mapRef?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    LaunchedEffect(zoomOutRequest, styleReady) {
        if (zoomOutRequest == 0 || !styleReady) return@LaunchedEffect
        mapRef?.animateCamera(CameraUpdateFactory.zoomOut())
    }
}

private fun addResearchStationIcon(style: Style, packRoot: File?) {
    if (packRoot == null) return
    val iconsDir = File(packRoot, "icons")
    val hiRes = File(iconsDir, "research-station@2x.png")
    val loRes = File(iconsDir, "research-station.png")
    val file = when {
        hiRes.isFile -> hiRes
        loRes.isFile -> loRes
        else -> return
    }
    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
    // @2x asset → pixel ratio 2 so MapLibre scales it correctly.
    if (file.name.contains("@2x")) {
        bitmap.density = 320
    } else {
        bitmap.density = 160
    }
    style.addImage(ICON_RESEARCH_STATION, bitmap)
}

private fun ensureOverlayLayers(style: Style) {
    if (style.getSource(SOURCE_COLONIES) == null) {
        style.addSource(GeoJsonSource(SOURCE_COLONIES, emptyFeatureCollection()))
    }
    if (style.getSource(SOURCE_LABELS) == null) {
        style.addSource(GeoJsonSource(SOURCE_LABELS, emptyFeatureCollection()))
    }
    if (style.getSource(SOURCE_DEVICE) == null) {
        style.addSource(GeoJsonSource(SOURCE_DEVICE, emptyFeatureCollection()))
    }

    // Desired stack (top → bottom): glyphs → COMNAP → lat/long → colony boxes → basemap.
    // Colony fill/line sit just under the graticule (lat/long).
    val belowGraticule = LAYER_GRATICULE.takeIf { style.getLayer(it) != null }
        ?: LAYER_COMNAP_FACILITIES.takeIf { style.getLayer(it) != null }

    if (style.getLayer(LAYER_FILL) == null) {
        addOverlayLayer(
            style,
            FillLayer(LAYER_FILL, SOURCE_COLONIES).withProperties(
                PropertyFactory.fillColor(
                    Expression.match(
                        Expression.get("category"),
                        Expression.literal("Inside"), Expression.color(Color.argb(90, 33, 150, 243)),
                        Expression.literal("Outside"), Expression.color(Color.argb(50, 158, 158, 158)),
                        Expression.literal("Local"), Expression.color(Color.argb(70, 156, 39, 176)),
                        Expression.color(Color.argb(40, 96, 125, 139)),
                    ),
                ),
                PropertyFactory.fillOpacity(
                    Expression.match(
                        Expression.get("active"),
                        Expression.literal("true"), Expression.literal(0.55f),
                        Expression.literal(0.28f),
                    ),
                ),
            ),
            belowLayerId = belowGraticule,
        )
    }
    if (style.getLayer(LAYER_LINE) == null) {
        addOverlayLayer(
            style,
            LineLayer(LAYER_LINE, SOURCE_COLONIES).withProperties(
                PropertyFactory.lineColor(
                    Expression.match(
                        Expression.get("active"),
                        Expression.literal("true"), Expression.color(Color.rgb(255, 152, 0)),
                        Expression.match(
                            Expression.get("category"),
                            Expression.literal("Inside"), Expression.color(Color.rgb(25, 118, 210)),
                            Expression.literal("Outside"), Expression.color(Color.rgb(117, 117, 117)),
                            Expression.literal("Local"), Expression.color(Color.rgb(123, 31, 162)),
                            Expression.color(Color.rgb(69, 90, 100)),
                        ),
                    ),
                ),
                PropertyFactory.lineWidth(
                    Expression.match(
                        Expression.get("active"),
                        Expression.literal("true"), Expression.literal(3.5f),
                        Expression.match(
                            Expression.get("category"),
                            Expression.literal("Inside"), Expression.literal(2.0f),
                            Expression.literal("Outside"), Expression.literal(1.2f),
                            Expression.literal(1.5f),
                        ),
                    ),
                ),
            ),
            belowLayerId = belowGraticule,
        )
    }
    // GPS sits with COMNAP (above lat/long, under text glyphs).
    if (style.getLayer(LAYER_DEVICE) == null) {
        val belowGlyphs = LAYER_COMNAP_LABELS.takeIf { style.getLayer(it) != null }
        addOverlayLayer(
            style,
            org.maplibre.android.style.layers.CircleLayer(LAYER_DEVICE, SOURCE_DEVICE).withProperties(
                PropertyFactory.circleRadius(
                    Expression.match(
                        Expression.get("live"),
                        Expression.literal("true"), Expression.literal(9f),
                        Expression.literal(7f),
                    ),
                ),
                PropertyFactory.circleColor(
                    Expression.match(
                        Expression.get("live"),
                        Expression.literal("true"), Expression.color(Color.rgb(33, 150, 243)),
                        Expression.color(Color.rgb(100, 181, 246)),
                    ),
                ),
                PropertyFactory.circleStrokeWidth(2.5f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
            ),
            belowLayerId = belowGlyphs,
            aboveLayerId = if (belowGlyphs == null) {
                LAYER_COMNAP_FACILITIES.takeIf { style.getLayer(it) != null }
            } else {
                null
            },
        )
    }
    // Colony name labels join the glyph stack at the very top.
    if (style.getLayer(LAYER_LABELS) == null) {
        style.addLayer(
            SymbolLayer(LAYER_LABELS, SOURCE_LABELS).withProperties(
                PropertyFactory.textField(Expression.get("name")),
                PropertyFactory.textFont(arrayOf("Open Sans Regular")),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(Color.BLACK),
                PropertyFactory.textHaloColor(Color.WHITE),
                PropertyFactory.textHaloWidth(1.2f),
                PropertyFactory.textAllowOverlap(false),
            ),
        )
    }
}

private fun addOverlayLayer(
    style: Style,
    layer: org.maplibre.android.style.layers.Layer,
    belowLayerId: String? = null,
    aboveLayerId: String? = null,
) {
    when {
        belowLayerId != null -> style.addLayerBelow(layer, belowLayerId)
        aboveLayerId != null -> style.addLayerAbove(layer, aboveLayerId)
        else -> style.addLayer(layer)
    }
}

private fun updateColonySources(
    style: Style,
    colonies: List<SealColony>,
    activeColonyName: String?,
) {
    (style.getSource(SOURCE_COLONIES) as? GeoJsonSource)
        ?.setGeoJson(coloniesToGeoJson(colonies, activeColonyName))
    (style.getSource(SOURCE_LABELS) as? GeoJsonSource)
        ?.setGeoJson(colonyLabelsToGeoJson(colonies))
}

private fun updateDeviceSource(style: Style, location: GeoLocation?) {
    val source = style.getSource(SOURCE_DEVICE) as? GeoJsonSource ?: return
    if (location == null) {
        source.setGeoJson(emptyFeatureCollection())
        return
    }
    val live = location.isLiveFix
    val lon = location.coordinates.longitude
    val lat = location.coordinates.latitude
    source.setGeoJson(
        """
        {"type":"FeatureCollection","features":[{
          "type":"Feature",
          "properties":{"live":"${if (live) "true" else "false"}"},
          "geometry":{"type":"Point","coordinates":[$lon,$lat]}
        }]}
        """.trimIndent(),
    )
}

private fun emptyFeatureCollection(): String =
    """{"type":"FeatureCollection","features":[]}"""

private fun packCameraBounds(): LatLngBounds =
    LatLngBounds.Builder()
        .include(LatLng(MapTileEnvelope.SOUTH, MapTileEnvelope.WEST))
        .include(LatLng(MapTileEnvelope.NORTH, MapTileEnvelope.EAST))
        .build()

private fun bozemanCameraBounds(): LatLngBounds =
    LatLngBounds.Builder()
        .include(LatLng(BozemanMapEnvelope.SOUTH, BozemanMapEnvelope.WEST))
        .include(LatLng(BozemanMapEnvelope.NORTH, BozemanMapEnvelope.EAST))
        .build()

private enum class CameraRegion { Antarctic, Bozeman, Unlocked }

private fun regionFor(latitude: Double, longitude: Double): CameraRegion = when {
    MapTileEnvelope.contains(latitude, longitude) -> CameraRegion.Antarctic
    BozemanMapEnvelope.contains(latitude, longitude) -> CameraRegion.Bozeman
    else -> CameraRegion.Unlocked
}

/**
 * Cross-region flies must clear the previous [setLatLngBoundsForCameraTarget] first.
 * Applying Bozeman bounds while the camera is still in Antarctica (or the reverse)
 * leaves the target outside the active lock, so MapLibre no-ops the animation.
 */
private fun flyToDeviceLocation(
    map: MapLibreMap,
    latitude: Double,
    longitude: Double,
    zoom: Double,
    animate: Boolean,
    onComplete: (() -> Unit)? = null,
) {
    flyToRegion(
        map = map,
        latitude = latitude,
        longitude = longitude,
        zoom = zoom,
        region = regionFor(latitude, longitude),
        animate = animate,
        onComplete = onComplete,
    )
}

private fun flyToRegion(
    map: MapLibreMap,
    latitude: Double,
    longitude: Double,
    zoom: Double,
    region: CameraRegion,
    animate: Boolean = true,
    onComplete: (() -> Unit)? = null,
) {
    val target = LatLng(latitude, longitude)
    val clampedZoom = when (region) {
        CameraRegion.Antarctic ->
            zoom.coerceIn(MapTileEnvelope.MIN_ZOOM, MapTileEnvelope.MAX_ZOOM)
        CameraRegion.Bozeman ->
            zoom.coerceIn(BozemanMapEnvelope.MIN_ZOOM, BozemanMapEnvelope.MAX_ZOOM)
        CameraRegion.Unlocked -> zoom
    }
    val update = CameraUpdateFactory.newLatLngZoom(target, clampedZoom)
    val camera = map.cameraPosition.target
    val crossingRegions = camera == null ||
        regionFor(camera.latitude, camera.longitude) != region

    val applyLock = {
        when (region) {
            CameraRegion.Antarctic -> applyAntarcticCameraLock(map)
            CameraRegion.Bozeman -> applyBozemanCameraLock(map)
            CameraRegion.Unlocked -> {
                map.setMinZoomPreference(0.0)
                map.setMaxZoomPreference(
                    maxOf(MapTileEnvelope.MAX_ZOOM, BozemanMapEnvelope.MAX_ZOOM),
                )
                map.setLatLngBoundsForCameraTarget(null)
            }
        }
    }

    // Previous regional lock rejects targets outside its envelope (e.g. McMurdo → Baxter).
    if (crossingRegions) {
        map.setLatLngBoundsForCameraTarget(null)
    }

    if (!animate) {
        map.moveCamera(update)
        applyLock()
        onComplete?.invoke()
        return
    }

    val callback = object : MapLibreMap.CancelableCallback {
        override fun onFinish() {
            applyLock()
            onComplete?.invoke()
        }

        override fun onCancel() {
            applyLock()
            onComplete?.invoke()
        }
    }

    if (crossingRegions) {
        map.animateCamera(update, callback)
    } else {
        applyLock()
        map.animateCamera(update, callback)
    }
}

private fun applyAntarcticCameraLock(map: MapLibreMap) {
    map.setMinZoomPreference(MapTileEnvelope.MIN_ZOOM)
    map.setMaxZoomPreference(MapTileEnvelope.MAX_ZOOM)
    map.setLatLngBoundsForCameraTarget(packCameraBounds())
}

private fun applyBozemanCameraLock(map: MapLibreMap) {
    map.setMinZoomPreference(BozemanMapEnvelope.MIN_ZOOM)
    map.setMaxZoomPreference(BozemanMapEnvelope.MAX_ZOOM)
    map.setLatLngBoundsForCameraTarget(bozemanCameraBounds())
}

internal tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
