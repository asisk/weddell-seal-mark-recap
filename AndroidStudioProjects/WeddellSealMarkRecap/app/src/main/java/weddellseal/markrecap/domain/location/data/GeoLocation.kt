package weddellseal.markrecap.domain.location.data

data class GeoLocation(
    val coordinates: Coordinates,
    val altitude: Double? = null,
    val bearing: Double? = null,
    val updatedDate : String? = null
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

fun GeoLocation.toLocationString(): String {
    return "Lat : ${this.coordinates.latitude}    " + "Long : ${this.coordinates.longitude}\n" + "Updated: ${this.updatedDate}"
}