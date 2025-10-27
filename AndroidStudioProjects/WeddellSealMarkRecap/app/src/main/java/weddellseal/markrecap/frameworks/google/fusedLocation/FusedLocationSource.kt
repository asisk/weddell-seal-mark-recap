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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.frameworks.google.fusedLocation.types.fromFusedLocation
import weddellseal.markrecap.ui.permissions.locationPermissionsGranted
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FusedLocationSource"

class FusedLocationSource(
    private val context: Context
) : LocationSource, LocationListener {

    private val fusedProviderClient: FusedLocationProviderClient
    private val locationFlow = MutableSharedFlow<GeoLocation>()
    private var isUpdating = false

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
                }.build(),
                null
            ).addOnSuccessListener { location ->
                continuation.resume(Result.success(GeoLocation.Companion.fromFusedLocation(location)))
            }.addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
        }
    }

    @OptIn(FlowPreview::class)
    override suspend fun locationUpdates(): Flow<GeoLocation> {
        return locationFlow
            .distinctUntilChanged { old, new ->
                // Only consider locations "different" if they're more than 0.5 meters apart
                val distance = old.coordinates.distanceTo(new.coordinates)
                distance < 0.5
            }
            .sample(5000L) // Sample at most once per 5 seconds
    }

    @SuppressLint("MissingPermission")
    override suspend fun startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates called, isUpdating: $isUpdating")
        if (isUpdating) {
            Log.w(TAG, "Location updates already running, skipping start")
            return
        }
        if (!context.locationPermissionsGranted()) {
            Log.e(TAG, "startUpdates(): Location permissions not granted")
            return
        }
        
        Log.i(TAG, "Starting location updates with permissions granted")
        fusedProviderClient.requestLocationUpdates(
            LocationRequest.Builder(5000L).apply {
                setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateDistanceMeters(0.5f) // Update for movement > 0.5 meters
                setMaxUpdateDelayMillis(5000L) // Maximum 5 second delay
            }.build(),
            Executors.newSingleThreadExecutor(),
            this,
        )
        isUpdating = true
        Log.i(TAG, "Location updates started successfully, isUpdating: $isUpdating")
    }

    override suspend fun stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates called, isUpdating: $isUpdating")
        if (!isUpdating) {
            Log.w(TAG, "Location updates not running, skipping stop")
            return
        }
        fusedProviderClient.removeLocationUpdates(this)
        isUpdating = false
        Log.i(TAG, "Location updates stopped successfully, isUpdating: $isUpdating")
    }

    override fun onLocationChanged(update: Location) {
        Log.d(TAG, "onLocationChanged received: lat=${update.latitude}, lng=${update.longitude}, accuracy=${update.accuracy}m")
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val geoLocation = GeoLocation.Companion.fromFusedLocation(update)
                locationFlow.emit(geoLocation)
                Log.d(TAG, "Successfully emitted location update")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing location update", e)
            }
        }
    }
}