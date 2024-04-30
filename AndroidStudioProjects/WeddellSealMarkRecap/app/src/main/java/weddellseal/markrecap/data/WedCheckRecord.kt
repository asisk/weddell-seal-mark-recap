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

// Extension function to map WedCheckRecord to Seal
fun WedCheckRecord.toSeal(): AddObservationLogViewModel.Seal {
    var name =""
    if (ageClass == "A") {
        name = "Adult"
    } else if (ageClass == "P") {
        name = "PupOne"
    }

    var numTags = 0
    var tagId = ""
    var tagAlpha = ""

    // Process tags and update variables
    val processedTags = processTags(tagIdOne, tagIdTwo)
    numTags = processedTags.first
    tagId = processedTags.second
    tagAlpha = processedTags.third

    return AddObservationLogViewModel.Seal(
        age = ageYears.toString(),
        comment = comments,
        condition = "",
        isStarted = false,
        name = name,
        notebookDataString = "",
        numRelatives = previousPups.toIntOrNull() ?: 0,
        numTags = numTags,
        pupPeed = false, // this field won't exist on historic records before 2024
        sex = sex,
        tagAlpha = tagAlpha,
        tagEventType = "", // Fill this field with appropriate data
        tagId = tagId,
        tissueTaken = tissueSampled.toBoolean(),
        tagNumber = 0 // Fill this field with appropriate data
    )

    fun String.toBoolean(): Boolean {
        return equals("true", ignoreCase = true)
    }

}

fun processTags(tag1: String?, tag2: String?): Triple<Int, String, String> {
    var numTags = 0
    var tagId = ""
    var tagAlpha = ""

    val validTag = when {
        !tag1.isNullOrBlank() && tag1 != "NA" -> tag1
        !tag2.isNullOrBlank() && tag2 != "NA" -> tag2
        else -> null
    }

    validTag?.let {
        numTags++
        tagId = it
        tagAlpha = it.last().toString()
    }

    return Triple(numTags, tagId, tagAlpha)
}