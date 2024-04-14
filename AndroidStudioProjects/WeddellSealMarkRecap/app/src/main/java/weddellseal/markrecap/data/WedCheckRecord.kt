package weddellseal.markrecap.data

/*
 * Used in to display the Recent Observations in list form
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
    @ColumnInfo(name = "tissue") val tissueSampled: String,
    @ColumnInfo(name = "previousPups") val previousPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "massPups") val massPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "swimPups") val swimPups: String, // NA possible value, otherwise its a number
    @ColumnInfo(name = "photoYears") val photoYears: String, // NA possible value, otherwise its a number
)