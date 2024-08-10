package weddellseal.markrecap.data

/*
 * Used in to display a database row of historic seal observation
 */

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
    @ColumnInfo(name = "pupinMassStudy") val pupinMassStudy: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "numPreviousPups") val numPreviousPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "pupinTTStudy") val pupinTTStudy: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "momMassMeasurements") val momMassMeasurements: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "condition") val condition: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "lastPhysio") val lastPhysio: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "colony") val colony: String, // NA possible value, otherwise its a number
)

// Extension function to map WedCheckRecord to Seal
fun WedCheckRecord.toSeal(): WedCheckSeal {
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

    var numTags = 0
    var tagOneAlpha = ""
    var tagOneNumber = 0
    var tagTwoAlpha = ""
    var tagTwoNumber = 0

    // Process tags and update variables

    val processedTagOne = processTags(tagIdOne)
    tagOneAlpha = processedTagOne.tagAlpha
    tagOneNumber = processedTagOne.tagNumber
    if (processedTagOne.tagValid) {
        numTags++
    }

    val processedTagTwo = processTags(tagIdTwo)
    tagTwoAlpha = processedTagTwo.tagAlpha
    tagTwoNumber = processedTagTwo.tagNumber
    if (processedTagTwo.tagValid) {
        numTags++
    }

    return WedCheckSeal(
        age = ageString,
        ageYears = ageNumeric,
        comment = comments,
        condition = condition,
        found = true,
        lastSeenSeason = season,
        massPups = pupinMassStudy,
        name = name,
        numTags = numTags.toString(),
        momMassMeasurements = momMassMeasurements,
        numPreviousPups = numPreviousPups,
        sex = sealSex,
        speNo = speno,
        pupinTTStudy = pupinTTStudy,
        tagEventType = "",
        tagIdOne = tagIdOne,
        tagOneAlpha = tagOneAlpha,
        tagOneNumber = tagOneNumber,
        tagIdTwo = tagIdTwo,
        tagTwoAlpha = tagTwoAlpha,
        tagTwoNumber = tagTwoNumber,
        tissueSampled = tissue,
        lastPhysio = lastPhysio,
        colony = colony
    )
}

data class TagProcessingResult(
    val tagValid: Boolean,
    val tagAlpha: String,
    val tagNumber: Int
)

fun processTags(tag: String?): TagProcessingResult {
    var tagValid = false
    var finalTagAlpha = ""
    var finalTagNumber = 0

    fun validateTag(tag: String?) {

        //verify that the tag is the valid format
        if (tag.isNullOrBlank() || tag == "NA" || tag == "NoTag") return
        if (tag.dropLast(1).toIntOrNull() == null) return
        if (!tag.last().isLetter()) return

        tagValid = true

        if (tag == "") {
            finalTagAlpha = tag.last().toString()
            finalTagNumber = tag.dropLast(1).toIntOrNull() ?: 0
        }
    }

    validateTag(tag)

    return TagProcessingResult(tagValid, finalTagAlpha, finalTagNumber)
}