package weddellseal.markrecap.ui.map

import android.content.Context
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import weddellseal.markrecap.frameworks.map.OfflineMapAssets
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import java.io.File

/**
 * Keeps a single [MapView] alive for the Activity so leaving/returning to Map via
 * navigation does not destroy GL state, reload the style, or reset the camera.
 *
 * The map is hosted above [androidx.navigation.NavHost] and only hidden (not disposed)
 * when another screen is shown.
 */
class WarmMapViewModel : ViewModel() {
    private var hostIdentity: Int = 0
    private var mapView: MapView? = null
    private var mapLibreMap: MapLibreMap? = null
    private var loadedStylePath: String? = null
    private var mapConfigured: Boolean = false
    private var cameraMoveListener: MapLibreMap.OnCameraMoveStartedListener? = null
    private var cameraIdleBearingListener: MapLibreMap.OnCameraMoveListener? = null

    /** Once true, the map layer stays in composition for the Activity lifetime. */
    var keepAlive by mutableStateOf(false)
        private set

    var styleFile by mutableStateOf<File?>(null)
        private set
    var packError by mutableStateOf<String?>(null)
        private set
    var preparing by mutableStateOf(false)
        private set

    var followLive by mutableStateOf(true)
    var myLocationRequest by mutableIntStateOf(0)
        private set
    var centerMcMurdoRequest by mutableIntStateOf(0)
        private set
    var centerBozemanRequest by mutableIntStateOf(0)
        private set
    var zoomInRequest by mutableIntStateOf(0)
        private set
    var zoomOutRequest by mutableIntStateOf(0)
        private set
    var resetNorthRequest by mutableIntStateOf(0)
        private set
    var flyToColonyRequest by mutableIntStateOf(0)
        private set

    /** Target for [flyToColonyRequest]; consumed by the map view. */
    var pendingColonyCamera by mutableStateOf<ColonyCameraTarget?>(null)
        private set

    /** Last colony the user flew to (dropdown label). */
    var lastFlownColonyName by mutableStateOf<String?>(null)
        private set

    /** Current map bearing in degrees (0 = north-up). For the compass control. */
    var mapBearing by mutableStateOf(0.0)
        private set

    /** Updated each composition so the retained listener stays current. */
    var onCameraMovedByUser: () -> Unit = {}

    fun ensurePackPrepared(context: Context) {
        if (styleFile != null && packError == null) {
            keepAlive = true
            return
        }
        if (preparing) return
        preparing = true
        packError = null
        viewModelScope.launch {
            val prepared = withContext(Dispatchers.IO) {
                runCatching { OfflineMapAssets.ensureCopied(context.applicationContext) }
                    .onFailure { packError = it.message }
                    .getOrNull()
            }
            styleFile = prepared?.styleFile
            if (prepared == null && packError == null) {
                packError = MapScreenUi.PACK_MISSING
            }
            preparing = false
            if (styleFile != null) {
                keepAlive = true
            }
        }
    }

    fun requestMyLocation() {
        followLive = true
        myLocationRequest++
    }

    fun requestMcMurdo() {
        followLive = false
        centerMcMurdoRequest++
    }

    fun requestBozeman() {
        followLive = false
        centerBozemanRequest++
    }

    fun requestZoomIn() {
        followLive = false
        zoomInRequest++
    }

    fun requestZoomOut() {
        followLive = false
        zoomOutRequest++
    }

    fun requestResetNorth() {
        resetNorthRequest++
    }

    fun requestFlyToColony(colony: SealColony) {
        val target = colonyCameraTarget(colony) ?: return
        followLive = false
        lastFlownColonyName = colony.location
        pendingColonyCamera = target
        flyToColonyRequest++
    }

    fun obtainMapView(activity: ComponentActivity): MapView {
        val identity = System.identityHashCode(activity)
        val existing = mapView
        if (existing != null && hostIdentity == identity) {
            return existing
        }
        // Prefer keeping the existing MapView across Activity recreate when possible.
        if (existing != null) {
            hostIdentity = identity
            return existing
        }
        hostIdentity = identity
        return MapView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            onCreate(null)
            mapView = this
        }
    }

    fun mapLibreMap(): MapLibreMap? = mapLibreMap

    fun setMapLibreMap(map: MapLibreMap) {
        mapLibreMap = map
    }

    fun isStyleLoaded(styleFile: File): Boolean =
        mapLibreMap != null && loadedStylePath == styleFile.absolutePath

    fun markStyleLoaded(styleFile: File) {
        loadedStylePath = styleFile.absolutePath
    }

    fun isMapConfigured(): Boolean = mapConfigured

    fun markMapConfigured() {
        mapConfigured = true
    }

    fun ensureCameraMoveListener(map: MapLibreMap) {
        if (cameraMoveListener == null) {
            val listener = MapLibreMap.OnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    onCameraMovedByUser()
                }
            }
            cameraMoveListener = listener
            map.addOnCameraMoveStartedListener(listener)
        }
        if (cameraIdleBearingListener == null) {
            val bearingListener = MapLibreMap.OnCameraMoveListener {
                mapBearing = map.cameraPosition.bearing
            }
            cameraIdleBearingListener = bearingListener
            map.addOnCameraMoveListener(bearingListener)
            mapBearing = map.cameraPosition.bearing
        }
    }

    fun setMapVisible(visible: Boolean) {
        val view = mapView ?: return
        if (visible) {
            view.onStart()
            view.onResume()
        } else {
            view.onPause()
            // Keep surface; onStop tends to drop GL resources and causes a reload flash.
        }
    }

    private fun destroyMapView() {
        val map = mapLibreMap
        val listener = cameraMoveListener
        if (map != null && listener != null) {
            map.removeOnCameraMoveStartedListener(listener)
        }
        val bearingListener = cameraIdleBearingListener
        if (map != null && bearingListener != null) {
            map.removeOnCameraMoveListener(bearingListener)
        }
        cameraMoveListener = null
        cameraIdleBearingListener = null
        mapLibreMap = null
        loadedStylePath = null
        mapConfigured = false
        mapBearing = 0.0
        mapView?.onDestroy()
        mapView = null
        hostIdentity = 0
    }

    override fun onCleared() {
        destroyMapView()
    }
}

/** Camera destination for flying to a colony box (adj point + region-aware zoom). */
data class ColonyCameraTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)

fun colonyCameraTarget(colony: SealColony): ColonyCameraTarget? {
    if (!colony.isDrawableOnMap()) return null
    val lat = when {
        colony.adjLat != 0.0 -> colony.adjLat
        else -> (colony.nLimit + colony.sLimit) / 2.0
    }
    val lon = when {
        colony.adjLong != 0.0 -> colony.adjLong
        else -> (colony.eLimit + colony.wLimit) / 2.0
    }
    val latSpan = (colony.nLimit - colony.sLimit).coerceAtLeast(0.005)
    val lonSpan = (colony.eLimit - colony.wLimit).coerceAtLeast(0.005)
    val span = maxOf(latSpan, lonSpan)
    val rawZoom = when {
        span > 0.45 -> 10.0
        span > 0.2 -> 11.0
        span > 0.08 -> 12.0
        span > 0.03 -> 13.0
        else -> 14.0
    }
    val zoom = when {
        BozemanMapEnvelope.contains(lat, lon) ->
            rawZoom.coerceIn(BozemanMapEnvelope.MIN_ZOOM, BozemanMapEnvelope.MAX_ZOOM)
        MapTileEnvelope.contains(lat, lon) ->
            rawZoom.coerceIn(MapTileEnvelope.MIN_ZOOM, MapTileEnvelope.MAX_ZOOM)
        else -> MapScreenUi.COLONY_FLY_ZOOM
    }
    return ColonyCameraTarget(latitude = lat, longitude = lon, zoom = zoom)
}
