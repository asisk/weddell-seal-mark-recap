package weddellseal.markrecap.locationFramework

import android.location.Location

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null
)

fun Location.toCoordinates(): Coordinates {
    return Coordinates(
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = if (this.hasAltitude()) this.altitude else null
    )
}

