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
        if (cameraMoveListener != null) return
        val listener = MapLibreMap.OnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                onCameraMovedByUser()
            }
        }
        cameraMoveListener = listener
        map.addOnCameraMoveStartedListener(listener)
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
        cameraMoveListener = null
        mapLibreMap = null
        loadedStylePath = null
        mapConfigured = false
        mapView?.onDestroy()
        mapView = null
        hostIdentity = 0
    }

    override fun onCleared() {
        destroyMapView()
    }
}
