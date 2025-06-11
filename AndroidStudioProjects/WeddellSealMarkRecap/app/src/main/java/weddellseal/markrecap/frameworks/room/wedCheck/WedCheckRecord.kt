package weddellseal.markrecap.frameworks.room.wedCheck

/*
 * Used in to display a database row of historic seal observation
 */

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity

@Entity(
    tableName = "wedCheck",
    foreignKeys = [ForeignKey(
        entity = FileUploadEntity::class,       // The parent table entity (FileUploadEntity)
        parentColumns = arrayOf("id"),          // The primary key column in the parent table (FileUploadEntity)
        childColumns = arrayOf("fileUploadId"), // The column in the child table (wedCheck) that references the parent
        onDelete = ForeignKey.CASCADE           // Optional: delete related rows if the file record is deleted
    )],
    indices = [
        Index(value = ["fileUploadId"]),            // Index for faster lookups on fileUploadId
        Index(value = ["speno"], unique = true),    // Index for speno, ensure it's unique
        Index(value = ["tagNumberOne"])             // Index for faster lookups on tagNumberOne
    ]
)
// Since speno is the primary key, each record is uniquely identified by speno.
data class WedCheckRecord(
    @PrimaryKey val speno: Int,
    @ColumnInfo(name = "lastSeenSeason") val season: Int,                       // date format: yyyy
    @ColumnInfo(name = "lastObservedAgeClass") val ageClass: String,
    @ColumnInfo(name = "sex") val sex: String,
    @ColumnInfo(name = "tagNumberOne") val tagIdOne: String,
    @ColumnInfo(name = "tagNumberTwo") val tagIdTwo: String,
    @ColumnInfo(name = "comments") val comments: String,
    @ColumnInfo(name = "ageYears") val ageYears: Int,
    @ColumnInfo(name = "tissue") val tissueSampled: String,                     // NA possible value
    @ColumnInfo(name = "pupinMassStudy") val pupinMassStudy: String,            // NA possible value, otherwise its a number
    @ColumnInfo(name = "numPreviousPups") val numPreviousPups: String,          // NA possible value, otherwise its a number
    @ColumnInfo(name = "pupinTTStudy") val pupinTTStudy: String,                // NA possible value, otherwise its a number
    @ColumnInfo(name = "momMassMeasurements") val momMassMeasurements: String,  // NA possible value, otherwise its a number
    @ColumnInfo(name = "condition") val condition: String,                      // NA possible value, otherwise its a number
    @ColumnInfo(name = "lastPhysio") val lastPhysio: String,                    // NA possible value, otherwise its a number
    @ColumnInfo(name = "colony") val colony: String,                            // NA possible value, otherwise its a number
    @ColumnInfo(name = "fileUploadId") val fileUploadId: Long                   // Foreign key reference
)

// Extension function to map WedCheckRecord to Seal
fun WedCheckRecord.toSeal(): WedCheckSeal {
    var ageString = ""
    if (ageClass == "A") {
        ageString = "Adult"
    } else if (ageClass == "P") {
        ageString = "Pup"
    } else if (ageClass == "Y") {
        ageString = "Yearling"
    }

    val sealSex = when (sex) {
        "F" -> "Female"
        "M" -> "Male"
        "U" -> "Unknown"
        else -> "Unknown"
    }

    var ageNumeric = "Unknown"
    if (ageYears > 0) {
        ageNumeric = ageYears.toString()
    }

    // Process tags and update variables
    var numTags = 0
    val processedTagOne = processTags(tagIdOne)
    if (processedTagOne.tagValid) {
        numTags++
    }

    val processedTagTwo = processTags(tagIdTwo)
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
        numTags = numTags.toString(), //not intending to map this over to the observation screen per August 1, 2024 meeting, but added to support validation
        momMassMeasurements = momMassMeasurements,
        numPreviousPups = numPreviousPups,
        sex = sealSex,
        speNo = speno,
        pupinTTStudy = pupinTTStudy,
        tagEventType = "",
        tagIdOne = tagIdOne,
        tagOneAlpha = processedTagOne.tagAlpha,
        tagOneNumber = processedTagOne.tagNumber,
        tagIdTwo = tagIdTwo,
        tagTwoAlpha = processedTagTwo.tagAlpha,
        tagTwoNumber = processedTagTwo.tagNumber,
        tissueSampled = tissueSampled, // updated to map unchanged per August 1, 2024 meeting
        lastPhysio = lastPhysio,
        colony = colony
    )
}

data class TagProcessingResult(
    val tagValid: Boolean,
    val tagAlpha: String,
    val tagNumber: String
)

fun processTags(tag: String?): TagProcessingResult {
    var tagValid = false
    var finalTagAlpha = ""
    var finalTagNumber = ""

    fun validateTag(tag: String?) {

        //verify that the tag is the valid format
        if (tag.isNullOrBlank() || tag == "NA" || tag == "NoTag") return
        if (tag.dropLast(1) == "") return
        if (!tag.last().isLetter()) return

        tagValid = true
        finalTagAlpha = tag.last().toString()
        finalTagNumber = tag.dropLast(1)
    }

    validateTag(tag)

    return TagProcessingResult(tagValid, finalTagAlpha, finalTagNumber)
}