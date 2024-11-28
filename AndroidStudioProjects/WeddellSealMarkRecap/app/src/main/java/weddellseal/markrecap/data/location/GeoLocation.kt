package weddellseal.markrecap.data.location

data class GeoLocation(
    val coordinates: Coordinates,
    val altitude: Double? = null,
    val bearing: Double? = null,
) {
    companion object {
    }
}

val GeoLocation.Companion.bozeman: GeoLocation
    get() = GeoLocation(
        coordinates = Coordinates(
            latitude = 45.6797,
            longitude = -111.0447
        ),
        bearing = 0.0,
    )

