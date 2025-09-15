package weddellseal.markrecap.domain.tagretag.data

enum class SealType(val type: String, val label: String) {
    PRIMARY("primary", "Seal"),
    PUPONE("pupOne", "Pup One"),
    PUPTWO("pupTwo", "Pup Two"),
    UNKNOWN("UNKNOWN", "Unknown");
}