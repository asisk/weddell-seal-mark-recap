package weddellseal.markrecap.domain.tagretag.data

/**
 * Maps a GPS colony location to the WedCheck population used for **lookup** highlighting.
 *
 * Parker 2025 season recap (lookup only): always show last-seen population. Highlight when the
 * GPS colony's population differs from last-seen (Erebus Bay vs White Island). Do not highlight
 * while GPS has not detected a colony. Do not use manual colony override — Parker wanted techs
 * to wait for GPS. Tag/retag is not part of this recap ask.
 */
object ColonyPopulation {
    const val WHITE_ISLAND = "White Island"
    const val EREBUS_BAY = "Erebus Bay"

    fun forLocation(location: String?): String? {
        if (location.isNullOrBlank()) return null
        return if (location.equals(WHITE_ISLAND, ignoreCase = true)) WHITE_ISLAND else EREBUS_BAY
    }

    /**
     * Lookup highlight: last-seen population vs GPS colony population.
     * False while GPS has no colony so lookup does not look like "no population detected."
     */
    fun shouldHighlightMismatch(sealPopulation: String?, currentColonyLocation: String?): Boolean {
        if (sealPopulation.isNullOrBlank()) return false
        val currentPopulation = forLocation(currentColonyLocation) ?: return false
        return !currentPopulation.equals(sealPopulation, ignoreCase = true)
    }

    /**
     * Pre-existing tag/retag behavior (not a 2025 recap ask): warn and ask for a photo when
     * entering a White Island seal and GPS is not at White Island, including while GPS is still
     * unknown.
     */
    fun shouldPromptWhiteIslandPhotoOnEnter(
        sealColony: String?,
        gpsColonyLocation: String?,
    ): Boolean {
        if (!sealColony.equals(WHITE_ISLAND, ignoreCase = true)) return false
        return gpsColonyLocation?.equals(WHITE_ISLAND, ignoreCase = true) != true
    }
}

