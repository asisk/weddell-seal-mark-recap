package weddellseal.markrecap.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sealColonies")
data class SealColony(
    @PrimaryKey(autoGenerate = true) val colonyId: Int = 0,
    @ColumnInfo(name = "inOut") val inOut: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "nLimit") val nLimit: Double,
    @ColumnInfo(name = "sLimit") val sLimit: Double,
    @ColumnInfo(name = "wLimit") val wLimit: Double,
    @ColumnInfo(name = "eLimit") val eLimit: Double,
    @ColumnInfo(name = "adjLat") val adjLat: Double,
    @ColumnInfo(name = "adjLong") val adjLong: Double
)
