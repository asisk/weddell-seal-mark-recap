package weddellseal.markrecap.domain.location.data

data class GeoLocation(
    val coordinates: Coordinates,
    val altitude: Double? = null,
    val bearing: Double? = null,
    val updatedDate: String? = null,
    val accuracyMeters: Float? = null,
    /** False for Fused Location last-known; never use those coordinates to detect or save colony. */
    val isLiveFix: Boolean = true,
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

fun GeoLocation.toLocationString(): String = coordinates.toDisplayString()