package weddellseal.markrecap.ui.home

import weddellseal.markrecap.frameworks.room.sealColonies.SealColony

data class ObservationMetadata(
    val selectedColony: SealColony?,
    val selectedObservers: List<String> = listOf(),
    val censusNumber: String = "",
    val isCensusMode: Boolean = false,
    val deviceID: String = "Unknown", // no validation as the user cannot affect change, set default value in case of error getting device
    val currentSeason: String = "2025 Preset", // no validation as the user cannot affect change, set default value in case of error generating season
    val originalDate: String = "",// used for writing edited records and retaining the original date
    val originalTimestamp: String = "", // used for writing edited records and retaining the original timestamp
) {
    // computed property, evaluated only when explicitly accessed
    val isValid: Boolean
        get() = selectedObservers != emptyList<String>()
                && (!isCensusMode || censusNumber != "")

    val isSelectedColonyValid: Boolean
        get() = selectedColony != null

    // computed property, evaluated only when explicitly accessed
    val invalidReason: String
        get() {
            val sb = StringBuilder()
            if (isCensusMode && censusNumber == "") sb.append("\nSelect a census number.")
            if (selectedObservers == emptyList<String>()) sb.append("\nSelect observer(s).")
            return sb.toString()
        }

    val invalidColonyReason : String
        get() {
            if (selectedColony == null) {
                return "Select a colony."
            }
            return ""
        }

    fun getObserversString(): String {
        return selectedObservers.joinToString(", ")
    }
}