package weddellseal.markrecap.data

/*
 * Used in to display the Recent Observations in list form
 */

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observationLogs")
data class ObservationLogEntry(
    //TODO, change this to match schema desired
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "date") val date: String, // date format: yyyy-MM-dd
    @ColumnInfo(name = "currentLocation") val currentLocation: String,
    @ColumnInfo(name = "lastKnownLocation") val lastKnownLocation: String
)