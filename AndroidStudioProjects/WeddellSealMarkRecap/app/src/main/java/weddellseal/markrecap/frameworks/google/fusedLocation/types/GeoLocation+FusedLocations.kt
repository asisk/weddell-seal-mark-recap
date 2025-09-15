package weddellseal.markrecap.frameworks.google.fusedLocation.types

import android.location.Location
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.ui.utils.getCoordinatesLastUpdatedDate

fun GeoLocation.Companion.fromFusedLocation(location: Location): GeoLocation {
    return GeoLocation(
        coordinates = Coordinates(
            latitude = location.latitude,
            longitude = location.longitude
        ),
        altitude = location.altitude,
        bearing = location.bearing.toDouble(),
        updatedDate = getCoordinatesLastUpdatedDate()
    )
}

