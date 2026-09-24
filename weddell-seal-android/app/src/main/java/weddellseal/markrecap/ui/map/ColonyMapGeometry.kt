package weddellseal.markrecap.ui.map

import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony

/** Tile / camera envelope matching the locked Antarctic pack (incl Capes). */
object MapTileEnvelope {
    const val SOUTH = -78.08
    const val NORTH = -74.53
    const val WEST = 163.07
    const val EAST = 168.13

    /** Camera floor; vector/hillshade tiles start at 5 (detail best from ~8). */
    const val MIN_ZOOM = 6.0
    const val MAX_ZOOM = 14.0

    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in SOUTH..NORTH && longitude in WEST..EAST

    fun intersectsColony(colony: SealColony): Boolean {
        if (!colony.isDrawableOnMap()) return false
        val latOverlap = colony.sLimit <= NORTH && colony.nLimit >= SOUTH
        val lonOverlap = colony.wLimit <= EAST && colony.eLimit >= WEST
        return latOverlap && lonOverlap
    }
}

/** Gallatin Valley / Bozeman local testing envelope (includes Baxter Meadows). */
object BozemanMapEnvelope {
    const val SOUTH = 45.58
    const val NORTH = 45.88
    const val WEST = -111.28
    const val EAST = -110.90

    const val MIN_ZOOM = 9.0
    const val MAX_ZOOM = 16.0

    /** Rough valley center for “Bozeman” camera. */
    const val CENTER_LATITUDE = 45.70
    const val CENTER_LONGITUDE = -111.05
    const val CENTER_ZOOM = 11.5

    /** Colony CSV Adj_* for Baxter Meadows. */
    const val BAXTER_LATITUDE = 45.70255
    const val BAXTER_LONGITUDE = -111.088
    const val BAXTER_ZOOM = 14.0

    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in SOUTH..NORTH && longitude in WEST..EAST

    fun intersectsColony(colony: SealColony): Boolean {
        if (!colony.isDrawableOnMap()) return false
        val latOverlap = colony.sLimit <= NORTH && colony.nLimit >= SOUTH
        val lonOverlap = colony.wLimit <= EAST && colony.eLimit >= WEST
        return latOverlap && lonOverlap
    }
}

enum class ColonyMapCategory {
    Inside,
    Outside,
    Local,
    Other,
}

fun SealColony.mapCategory(): ColonyMapCategory {
    val raw = inOut.trim().lowercase()
    return when {
        raw == "inside" || raw == "in" -> ColonyMapCategory.Inside
        raw == "outside" || raw == "out" -> ColonyMapCategory.Outside
        raw == "local" -> ColonyMapCategory.Local
        else -> ColonyMapCategory.Other
    }
}

fun SealColony.isDrawableOnMap(): Boolean {
    if (location.equals("Other", ignoreCase = true)) return false
    if (location == ColonyPopulation.NOT_DETECTED) return false
    if (nLimit == 0.0 && sLimit == 0.0 && wLimit == 0.0 && eLimit == 0.0) return false
    // Degenerate (zero-area) boxes.
    if (nLimit == sLimit || wLimit == eLimit) return false
    return nLimit >= sLimit && eLimit >= wLimit
}

fun List<SealColony>.drawableColonies(): List<SealColony> = filter { it.isDrawableOnMap() }

fun List<SealColony>.fitTargetColonies(): List<SealColony> =
    drawableColonies().filter { MapTileEnvelope.intersectsColony(it) }


/**
 * Builds a GeoJSON FeatureCollection of colony rectangles.
 * Properties: name, category, active (boolean as string for MapLibre filters).
 */
fun coloniesToGeoJson(
    colonies: List<SealColony>,
    activeLocationName: String?,
): String {
    val features = colonies.drawableColonies().map { colony ->
        val active = activeLocationName != null &&
            colony.location == activeLocationName &&
            activeLocationName != ColonyPopulation.NOT_DETECTED
        val category = colony.mapCategory().name
        val ring = buildString {
            append("[")
            append("[${colony.wLimit},${colony.sLimit}],")
            append("[${colony.eLimit},${colony.sLimit}],")
            append("[${colony.eLimit},${colony.nLimit}],")
            append("[${colony.wLimit},${colony.nLimit}],")
            append("[${colony.wLimit},${colony.sLimit}]")
            append("]")
        }
        """
        {
          "type": "Feature",
          "properties": {
            "name": ${jsonString(colony.location)},
            "category": ${jsonString(category)},
            "active": "${if (active) "true" else "false"}"
          },
          "geometry": {
            "type": "Polygon",
            "coordinates": [$ring]
          }
        }
        """.trimIndent()
    }
    return """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""
}

/**
 * Point features at adjLat/adjLong for labels.
 */
fun colonyLabelsToGeoJson(colonies: List<SealColony>): String {
    val features = colonies.drawableColonies().map { colony ->
        """
        {
          "type": "Feature",
          "properties": { "name": ${jsonString(colony.location)} },
          "geometry": {
            "type": "Point",
            "coordinates": [${colony.adjLong}, ${colony.adjLat}]
          }
        }
        """.trimIndent()
    }
    return """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""
}

private fun jsonString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
