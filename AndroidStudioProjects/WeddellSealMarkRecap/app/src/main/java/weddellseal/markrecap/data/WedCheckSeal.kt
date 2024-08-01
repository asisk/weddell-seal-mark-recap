package weddellseal.markrecap.data

data class WedCheckSeal(
    val age: String = "",
    val ageYears: String = "",
    val comment: String = "",
    val condition: String = "",
    var found: Boolean = false,
    val isWedCheckRecord: Boolean = false,
    val lastSeenSeason: Int = 0,
    val massPups: String = "",
    val name: String = "",
    val numRelatives: Int = 0,
    val numTags: String = "",
    val photoYears: String = "",
    val previousPups: String = "",
    val pupPeed: Boolean = false,
    val sex: String = "",
    val speNo: Int = 0,
    val swimPups: String = "",
    val tagAlpha: String = "",
    val tagEventType: String = "",
    val tagId: String = "",
    val tagNumber: Int = 0,
    val tissueSampled: String = ""
)