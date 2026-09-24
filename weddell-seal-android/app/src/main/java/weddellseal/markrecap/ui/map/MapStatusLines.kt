package weddellseal.markrecap.ui.map

import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.location.data.toLocationString
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.ui.home.ColonyGpsUi

object MapScreenUi {
    const val EMPTY_COLONIES =
        "No colony areas loaded. Import Colony_Locations in Admin."
    const val PREPARING_MAP = "Preparing map…"
    const val PACK_MISSING =
        "Basemap pack not installed. Add style.json and region.mbtiles under assets/map."
    const val MY_LOCATION = "My location"
    const val NORTH = "North"
    const val CENTER_MCMURDO = "McMurdo"
    const val CENTER_BOZEMAN = "Bozeman"
    const val MY_LOCATION_ZOOM = 16.0
    const val ZOOM_IN = "+"
    const val ZOOM_OUT = "−"
    const val LEGEND_TITLE = "Colony"
    const val LEGEND_INSIDE = "Inside"
    const val LEGEND_OUTSIDE = "Outside"
    const val LEGEND_LOCAL = "Local"
    const val LEGEND_ACTIVE = "Active (GPS)"

    /** COMNAP McMurdo Station (US). */
    const val MCMURDO_LATITUDE = -77.848209
    const val MCMURDO_LONGITUDE = 166.668422
    const val MCMURDO_ZOOM = 11.0
    const val ATTRIBUTION = "Basemap: Quantarctica + © OSM (Bozeman)"
    const val ATTRIBUTION_URL = "https://www.openstreetmap.org/copyright"
    const val OVERRIDE_PREFIX = "Override: "
}

data class MapStatusLines(
    val primary: String,
    val secondary: String? = null,
)

fun mapStatusLines(
    location: GeoLocation?,
    isRefreshingGps: Boolean,
    autoDetectedColony: SealColony?,
    overrideColony: Boolean,
    selectedColony: SealColony?,
): MapStatusLines {
    val coords = when {
        isRefreshingGps -> ColonyGpsUi.REFRESHING_GPS
        location == null -> ColonyGpsUi.WAITING_FOR_GPS_SHORT
        else -> {
            val display = location.toLocationString()
            if (!location.isLiveFix) {
                "${ColonyGpsUi.LAST_KNOWN_LABEL}: $display"
            } else {
                display
            }
        }
    }

    val gpsColony = when {
        location?.isLiveFix != true -> ColonyGpsUi.WAITING_FOR_GPS_SHORT
        autoDetectedColony == null -> ColonyGpsUi.WAITING_FOR_GPS_SHORT
        autoDetectedColony.location == ColonyPopulation.NOT_DETECTED ->
            ColonyPopulation.NOT_DETECTED
        else -> autoDetectedColony.location
    }

    val primary = "$coords · $gpsColony"

    val secondary = if (overrideColony) {
        val name = selectedColony?.location ?: "—"
        "${MapScreenUi.OVERRIDE_PREFIX}$name (set on Home)"
    } else {
        null
    }

    return MapStatusLines(primary = primary, secondary = secondary)
}

fun activeGpsColonyName(autoDetectedColony: SealColony?): String? {
    val name = autoDetectedColony?.location ?: return null
    if (name == ColonyPopulation.NOT_DETECTED) return null
    return name
}
