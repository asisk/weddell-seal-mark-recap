package weddellseal.markrecap.locationFramework

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

private const val TAG = "Context+GooglePlay"

fun Context.isGooglePlayAvailable(): Boolean {
    val resultCode =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

    // Use a `when` statement to determine Google Play services availability
    return when (resultCode) {
        ConnectionResult.SUCCESS -> {
            Log.i(TAG, "Google Play services are available.")
            true
        }

        ConnectionResult.SERVICE_MISSING -> {
            Log.e(TAG, "Google Play services are missing.")
            false
        }

        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
            Log.w(TAG, "Google Play services need to be updated.")
            //TODO, handling this case more explicitly, possibly prompting the user to update.
            true
        }

        ConnectionResult.SERVICE_DISABLED -> {
            Log.e(TAG, "Google Play services are disabled.")
            false
        }

        ConnectionResult.SERVICE_INVALID -> {
            Log.e(TAG, "Google Play services are invalid.")
            false
        }

        else -> {
            Log.e(TAG, "Google Play services are in an unknown state: $resultCode")
            false
        }
    }
}