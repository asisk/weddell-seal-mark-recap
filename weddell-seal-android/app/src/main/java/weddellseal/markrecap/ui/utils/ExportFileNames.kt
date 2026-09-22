package weddellseal.markrecap.ui.utils

/**
 * Default CSV name for the system save dialog, e.g. `observations_H_20260819_155900.csv`.
 * The user can still rename in the picker. Empty / unknown device names fall back to `tablet`.
 */
fun observationExportSuggestedName(
    deviceName: String,
    fileDate: String,
    allRecords: Boolean,
): String {
    val tablet = sanitizeDeviceNameForFilename(deviceName)
    val prefix = if (allRecords) "all_observations" else "observations"
    return "${prefix}_${tablet}_${fileDate}.csv"
}

internal fun sanitizeDeviceNameForFilename(deviceName: String): String {
    val normalized = deviceName.trim()
    if (normalized.isEmpty() || normalized.equals("Unknown Device", ignoreCase = true)) {
        return "tablet"
    }

    val replaced = buildString(normalized.length) {
        normalized.forEach { ch ->
            append(
                if (ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-') ch else '_'
            )
        }
    }
    val collapsed = replaced.replace(Regex("_+"), "_")
    val trimmed = collapsed.trim('_', '.', '-')
    return trimmed.takeIf { value -> value.any { it.isLetterOrDigit() } } ?: "tablet"
}
