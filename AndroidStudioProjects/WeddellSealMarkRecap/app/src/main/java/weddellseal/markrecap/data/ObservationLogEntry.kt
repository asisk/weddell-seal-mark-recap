package weddellseal.markrecap.data

/*
 * Used in to display the Recent Observations in list form
 */

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observationLogs")
data class ObservationLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "device_id") val deviceID: String,
    @ColumnInfo(name = "season") val season: String,
    @ColumnInfo(name = "speno") val speno: String,
    @ColumnInfo(name = "date") val date: String,  // date format: yyyy-MM-dd
    @ColumnInfo(name = "time") val time: String,  // time format: hh:mm:ss
    @ColumnInfo(name = "census_id") val censusID: String,
    @ColumnInfo(name = "latitude") val latitude: String,  // example -77.73004, could also be 4 decimal precision
    @ColumnInfo(name = "longitude") val longitude: String,  // example 166.7941, could also be 2 decimal precision
    @ColumnInfo(name = "age_class") val ageClass: String,
    @ColumnInfo(name = "sex") val sex: String,
    @ColumnInfo(name = "num_relatives") val numRelatives: String,
    @ColumnInfo(name = "old_tag_id_one") val oldTagIDOne: String,
    @ColumnInfo(name = "old_tag_id_two") val oldTagIDTwo: String,
    @ColumnInfo(name = "tag_id_one") val tagIDOne: String,
    @ColumnInfo(name = "tag_one_indicator") val tagOneIndicator: String,
    @ColumnInfo(name = "tag_id_two") val tagIDTwo: String,
    @ColumnInfo(name = "tag_two_indicator") val tagTwoIndicator: String,
    @ColumnInfo(name = "rel_tag_id_one") val relativeTagIDOne: String,
    @ColumnInfo(name = "rel_tag_id_two") val relativeTagIDTwo: String,
    @ColumnInfo(name = "seal_condition") val sealCondition: String,
    @ColumnInfo(name = "observer_initials") val observerInitials: String,
    @ColumnInfo(name = "flagged_entry") val flaggedEntry: String,
    @ColumnInfo(name = "tag_event") val tagEvent: String,
    @ColumnInfo(name = "retag_reason") val reasonForRetag: String,
    @ColumnInfo(name = "weight") val weight: String,
    @ColumnInfo(name = "tissue_sampled") val tissueSampled: String,
    @ColumnInfo(name = "comments") val comments: String,
    @ColumnInfo(name = "colony") val colony: String,
)