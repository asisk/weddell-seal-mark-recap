package weddellseal.markrecap.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Context.havePermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

fun Context.missingPermissions(): List<String> {
    // Location
    val requiredPerms = mutableSetOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // double check each one and return only the ones we do not have
    return requiredPerms.filterNot { havePermission(it) }
}

fun Context.locationPermissions(): List<String> {
    return listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}

/**
 * True when the app may use precise GPS.
 *
 * Fine location is what colony detect and observation stamps need. Do not require the
 * coarse bit from the permission-result map: on some Android 12+ devices, granting
 * Precise + "Only this time" returns an inconsistent FINE/COARSE pair and would leave
 * the disclosure screen stuck if we required both.
 */
fun Context.locationPermissionsGranted(): Boolean {
    return havePermission(Manifest.permission.ACCESS_FINE_LOCATION)
}

/** Coarse-only (Approximate) — not enough for this app's GPS features. */
fun Context.hasApproximateLocationOnly(): Boolean {
    return havePermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
        !havePermission(Manifest.permission.ACCESS_FINE_LOCATION)
}
