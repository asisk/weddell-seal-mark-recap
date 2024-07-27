package weddellseal.markrecap.data

data class Seal(
    val age: String = "",
    val ageYears: String = "",
    val comment: String = "",
    val condition: String = "",
    var isStarted: Boolean = false,
    val lastSeenSeason: Int = 0,
    val massPups: String = "",
    val name: String = "",
    val notebookDataString: String = "",
    val numRelatives: Int = 0,
    val numTags: Int = 0,
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
    val tissueTaken: Boolean = false,
    val tissueSampled: String = ""
)