package weddellseal.markrecap.data

/*
 * Used in to display the Recent Observations in list form
 */

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import weddellseal.markrecap.models.WedCheckViewModel

@Entity(tableName = "wedCheck")
data class WedCheckRecord(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "speno") val speno: Int,
    @ColumnInfo(name = "lastSeenSeason") val season: Int, // date format: yyyy
    @ColumnInfo(name = "lastObservedAgeClass") val ageClass: String,
    @ColumnInfo(name = "sex") val sex: String,
    @ColumnInfo(name = "tagNumberOne") val tagIdOne: String,
    @ColumnInfo(name = "tagNumberTwo") val tagIdTwo: String,
    @ColumnInfo(name = "comments") val comments: String,
    @ColumnInfo(name = "ageYears") val ageYears: Int,
    @ColumnInfo(name = "tissue") val tissueSampled: String, // NA possible value
    @ColumnInfo(name = "previousPups") val previousPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "massPups") val massPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "swimPups") val swimPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "photoYears") val photoYears: String, // NA possible value, otherwise its a number
)

data class TagProcessingResult(
    val numTags: String,
    val tagId: String,
    val tagAlpha: String,
    val tagNumber: Int
)

// Extension function to map WedCheckRecord to Seal
fun WedCheckRecord.toSeal(): WedCheckViewModel.WedCheckSeal {
    var name = ""
    var ageString = ""
    if (ageClass == "A") {
        name = "adult"
        ageString = "Adult"
    } else if (ageClass == "P") {
        name = "pupOne"
        ageString = "Pup"
    }

    val sealSex = when (sex) {
        "F" -> "Female"
        "M" -> "Male"
        "U" -> "Unknown"
        else -> "Unknown"
    }

    val tissue = when (tissueSampled) {
        "Need" -> "No"
        "Done" -> "Yes"
        else -> "NA"
    }

    var ageNumeric = "Unknown"
    if (ageYears > 0) {
        ageNumeric = ageYears.toString()
    }

    var numTags = ""
    var tagId = ""
    var tagAlpha = ""
    var tagNumber = 0

    // Process tags and update variables
    val processedTags = processTags(tagIdOne, tagIdTwo)
    numTags = processedTags.numTags
    tagId = processedTags.tagId
    tagAlpha = processedTags.tagAlpha
    tagNumber = processedTags.tagNumber

    return WedCheckViewModel.WedCheckSeal(
        age = ageString,
        ageYears = ageNumeric,
        comment = comments,
        condition = "",
        found = true,
        lastSeenSeason = season,
        massPups = massPups,
        name = name,
        numTags = numTags,
        photoYears = photoYears,
        previousPups = previousPups,
        sex = sealSex,
        speNo = speno,
        swimPups = swimPups,
        tagAlpha = tagAlpha,
        tagEventType = "",
        tagId = tagId,
        tagNumber = tagNumber,
        tissueSampled = tissue,
    )

    fun String.toBoolean(): Boolean {
        return equals("true", ignoreCase = true)
    }

}

fun processTags(tag1: String?, tag2: String?): TagProcessingResult {
    var numTags = 0
    var finalTagId = ""
    var finalTagAlpha = ""
    var finalTagNumber = 0

    fun validateTag(tag: String?) {

        //verify that the tag is the valid format
        if (tag.isNullOrBlank() || tag == "NA" || tag == "NoTag") return
        if (tag.dropLast(1).toIntOrNull() == null) return
        if (!tag.last().isLetter()) return

        numTags++

        if (finalTagId == "") {
            finalTagId = tag
            finalTagAlpha = tag.last().toString()
            finalTagNumber = tag.dropLast(1).toIntOrNull() ?: 0
        }
    }

    validateTag(tag1)
    validateTag(tag2)

    return TagProcessingResult(numTags.toString(), finalTagId, finalTagAlpha, finalTagNumber)
}