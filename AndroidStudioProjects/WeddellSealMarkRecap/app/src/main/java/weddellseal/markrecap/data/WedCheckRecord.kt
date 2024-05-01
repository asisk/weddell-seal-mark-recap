package weddellseal.markrecap.data

/*
 * Used in to display the Recent Observations in list form
 */

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import weddellseal.markrecap.models.AddObservationLogViewModel

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
    @ColumnInfo(name = "tissue") val tissueSampled: String,
    @ColumnInfo(name = "previousPups") val previousPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "massPups") val massPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "swimPups") val swimPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "photoYears") val photoYears: String, // NA possible value, otherwise its a number
)

data class TagProcessingResult(
    val numTags: Int,
    val tagId: String,
    val tagAlpha: String,
    val tagNumber: Int
)

// Extension function to map WedCheckRecord to Seal
fun WedCheckRecord.toSeal(): AddObservationLogViewModel.Seal {
    var name =""
    var ageString = ""
    if (ageClass == "A") {
        name = "adult"
        ageString = "Adult"
    } else if (ageClass == "P") {
        name = "pupOne"
        ageString = "Pup"
    }

    var numTags = 0
    var tagId = ""
    var tagAlpha = ""
    var tagNumber = 0

    // Process tags and update variables
    val processedTags = processTags(tagIdOne, tagIdTwo)
    numTags = processedTags.numTags
    tagId = processedTags.tagId
    tagAlpha = processedTags.tagAlpha
    tagNumber = processedTags.tagNumber

    return AddObservationLogViewModel.Seal(
        age = ageString,
        ageYears = ageYears,
        comment = comments,
        condition = "",
        isStarted = false,
        isWedCheckRecord = true,
        lastSeenSeason = season,
        massPups = massPups,
        name = name,
        notebookDataString = "",
        numRelatives = previousPups.toIntOrNull() ?: 0,
        numTags = numTags,
        photoYears = photoYears,
        previousPups = previousPups,
        pupPeed = false, // this field won't exist on historic records before 2024
        sex = sex,
        speNo = speno,
        swimPups = swimPups,
        tagAlpha = tagAlpha,
        tagEventType = "",
        tagId = tagId,
        tagNumber = tagNumber,
        tissueTaken = tissueSampled.toBoolean(),
    )

    fun String.toBoolean(): Boolean {
        return equals("true", ignoreCase = true)
    }

}

fun processTags(tag1: String?, tag2: String?): TagProcessingResult {
    var numTags = 0
    var tagId = ""
    var tagAlpha = ""
    var tagNumber = 0

    val validTag = when {
        !tag1.isNullOrBlank() && tag1 != "NA" -> tag1
        !tag2.isNullOrBlank() && tag2 != "NA" -> tag2
        else -> null
    }

    validTag?.let {
        numTags++
        tagId = it
        tagAlpha = it.last().toString()
        // Extract everything except the last character
        tagNumber = it.substring(0, it.length - 1).toIntOrNull() ?: 0

    }

    return TagProcessingResult(numTags, tagId, tagAlpha, tagNumber)
}