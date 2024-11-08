package weddellseal.markrecap.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


// Define an extension function on Context to fetch current location
@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
suspend fun Context.fetchCurrentLocation(): Coordinates? =
    suspendCancellableCoroutine { continuation ->
        // Initialize FusedLocationProviderClient
        val fusedLocationProviderClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        // Start fetching the location
        fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location: Location? ->
            continuation.resume(location?.toCoordinates())
        }.addOnFailureListener {
            // Resume with null or throw an exception
            continuation.resume(null)
        }
    }


fun Context.isGooglePlayAvailable(): Boolean {
    val resultCode =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

    // Use a `when` statement to determine Google Play services availability
    return when (resultCode) {
        ConnectionResult.SUCCESS -> {
            Log.i("GooglePlayServices", "Google Play services are available.")
            true
        }

        ConnectionResult.SERVICE_MISSING -> {
            Log.e("GooglePlayServices", "Google Play services are missing.")
            false
        }

        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
            Log.w("GooglePlayServices", "Google Play services need to be updated.")
            //TODO, handling this case more explicitly, possibly prompting the user to update.
            true
        }

        ConnectionResult.SERVICE_DISABLED -> {
            Log.e("GooglePlayServices", "Google Play services are disabled.")
            false
        }

        ConnectionResult.SERVICE_INVALID -> {
            Log.e("GooglePlayServices", "Google Play services are invalid.")
            false
        }

        else -> {
            Log.e(
                "GooglePlayServices",
                "Google Play services are in an unknown state: $resultCode"
            )
            false
        }
    }
}