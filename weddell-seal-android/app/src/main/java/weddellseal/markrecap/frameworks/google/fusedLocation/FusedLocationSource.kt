package weddellseal.markrecap.frameworks.google.fusedLocation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.LocationUpdatePhase
import weddellseal.markrecap.domain.location.areLocationsEquivalentForUi
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.location.locationRequestSettings
import weddellseal.markrecap.domain.location.sampleAfterFirstLiveFix
import weddellseal.markrecap.frameworks.google.fusedLocation.types.fromFusedLocation
import weddellseal.markrecap.logDebug
import weddellseal.markrecap.ui.permissions.locationPermissionsGranted
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FusedLocationSource"

class FusedLocationSource(
    private val context: Context
) : LocationSource, LocationListener {

    private val fusedProviderClient: FusedLocationProviderClient
    private val locationFlow = MutableSharedFlow<GeoLocation>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var isUpdating = false
    private var updatePhase = LocationUpdatePhase.ACQUIRE
    private var locationExecutor: ExecutorService? = null

    init {
        // Check if Google Play Services are available
        if (!context.isGooglePlayAvailable()) {
            error("Google Play Services are not available. Location Accuracy is likely to be affected.")
        }
        fusedProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun requestSingleUpdate(): Result<GeoLocation> {
        if (!context.locationPermissionsGranted()) {
            return Result.failure(IllegalStateException("Location permissions not granted"))
        }

        return suspendCoroutine { continuation ->
            fusedProviderClient.getCurrentLocation(
                CurrentLocationRequest.Builder().apply {
                    setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                    setMaxUpdateAgeMillis(0L)
                }.build(),
                null
            ).addOnSuccessListener { location ->
                if (location == null) {
                    continuation.resume(Result.failure(IllegalStateException("No current location")))
                } else {
                    continuation.resume(
                        Result.success(GeoLocation.fromFusedLocation(location, isLiveFix = true))
                    )
                }
            }.addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnownLocation(): GeoLocation? {
        if (!context.locationPermissionsGranted()) return null
        return suspendCoroutine { continuation ->
            fusedProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(
                        location?.let { GeoLocation.fromFusedLocation(it, isLiveFix = false) }
                    )
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    override suspend fun locationUpdates(): Flow<GeoLocation> {
        return locationFlow
            .distinctUntilChanged { old, new ->
                val isSame = areLocationsEquivalentForUi(old, new)
                if (isSame) {
                    logDebug(TAG) { "distinctUntilChanged: treating locations as the same" }
                }
                isSame
            }
            .sampleAfterFirstLiveFix()
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    override fun startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates called, isUpdating: $isUpdating")
        if (isUpdating) {
            Log.w(TAG, "Location updates already running, skipping start")
            return
        }
        if (!context.locationPermissionsGranted()) {
            Log.e(TAG, "startUpdates(): Location permissions not granted")
            return
        }

        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, TAG).apply { isDaemon = true }
        }
        try {
            updatePhase = LocationUpdatePhase.ACQUIRE
            fusedProviderClient.requestLocationUpdates(
                buildLocationRequest(LocationUpdatePhase.ACQUIRE),
                executor,
                this,
            )
        } catch (e: RuntimeException) {
            executor.shutdown()
            throw e
        }
        locationExecutor = executor
        isUpdating = true
        requestLastKnownAndCurrent(executor)
        Log.i(TAG, "Location updates started successfully, isUpdating: $isUpdating")
    }

    @SuppressLint("MissingPermission")
    private fun requestLastKnownAndCurrent(executor: ExecutorService) {
        fusedProviderClient.lastLocation
            .addOnSuccessListener(executor) { location ->
                if (!isUpdating || location == null) return@addOnSuccessListener
                locationFlow.tryEmit(GeoLocation.fromFusedLocation(location, isLiveFix = false))
            }
        fusedProviderClient.getCurrentLocation(
            CurrentLocationRequest.Builder().apply {
                setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMaxUpdateAgeMillis(0L)
            }.build(),
            null,
        ).addOnSuccessListener(executor) { location ->
            if (!isUpdating || location == null) return@addOnSuccessListener
            emitLiveFix(GeoLocation.fromFusedLocation(location, isLiveFix = true))
        }
    }

    @Synchronized
    override fun stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates called, isUpdating: $isUpdating")
        if (!isUpdating) {
            Log.w(TAG, "Location updates not running, skipping stop")
            return
        }
        fusedProviderClient.removeLocationUpdates(this)
        locationExecutor?.shutdown()
        locationExecutor = null
        isUpdating = false
        updatePhase = LocationUpdatePhase.ACQUIRE
        Log.i(TAG, "Location updates stopped successfully, isUpdating: $isUpdating")
    }

    override fun onLocationChanged(update: Location) {
        logDebug(TAG) { "onLocationChanged: accuracy=${update.accuracy}m" }
        try {
            emitLiveFix(GeoLocation.fromFusedLocation(update, isLiveFix = true))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing location update", e)
        }
    }

    @Synchronized
    private fun emitLiveFix(location: GeoLocation) {
        if (!isUpdating) return
        locationFlow.tryEmit(location)
        switchToTrackPhaseIfNeeded()
    }

    /**
     * After the first live fix, re-request updates at the calmer track cadence so a full
     * field day does not keep the GPS chip at 1 Hz.
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    private fun switchToTrackPhaseIfNeeded() {
        if (!isUpdating || updatePhase == LocationUpdatePhase.TRACK) return
        val executor = locationExecutor ?: return
        updatePhase = LocationUpdatePhase.TRACK
        fusedProviderClient.requestLocationUpdates(
            buildLocationRequest(LocationUpdatePhase.TRACK),
            executor,
            this,
        )
        Log.i(TAG, "Switched location updates to TRACK phase")
    }

    companion object {
        internal fun buildLocationRequest(phase: LocationUpdatePhase): LocationRequest {
            val settings = locationRequestSettings(phase)
            return LocationRequest.Builder(settings.intervalMs).apply {
                setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateDistanceMeters(settings.minUpdateDistanceMeters)
                setMinUpdateIntervalMillis(settings.minUpdateIntervalMs)
                settings.maxUpdateDelayMs?.let { setMaxUpdateDelayMillis(it) }
            }.build()
        }
    }
}
