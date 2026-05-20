package weddellseal.markrecap.frameworks.google.fusedLocation.types

import android.location.Location
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Cache the formatter to avoid recreating it on every call
private val COORDINATES_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd    HH:mm:ss    xxx")

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

// Optimized version that caches the formatter
private fun getCoordinatesLastUpdatedDate(): String {
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val nowAtOffset = Instant.now().atOffset(now.offset)
    return nowAtOffset.format(COORDINATES_DATE_FORMATTER) + "  UTC"
}

