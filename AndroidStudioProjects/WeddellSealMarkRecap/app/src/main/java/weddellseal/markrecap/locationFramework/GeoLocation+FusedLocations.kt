package weddellseal.markrecap.locationFramework

import android.location.Location
import weddellseal.markrecap.data.location.GeoLocation
import weddellseal.markrecap.data.location.Coordinates

fun GeoLocation.Companion.fromFusedLocation(location: Location): GeoLocation {
    return GeoLocation(
        coordinates = Coordinates(
            latitude = location.latitude,
            longitude = location.longitude
        ),
        altitude = location.altitude,
        bearing = location.bearing.toDouble(),
    )
}

