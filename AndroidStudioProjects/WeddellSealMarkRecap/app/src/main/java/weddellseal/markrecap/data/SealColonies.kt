package weddellseal.markrecap.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sealColonies",
    foreignKeys = [
        ForeignKey(
            entity = FileUploadEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("fileUploadId"),
            onDelete = ForeignKey.CASCADE // Optional: delete related rows if the file record is deleted
        )
    ],
    indices = [
        Index(value = ["fileUploadId"]), // Index for faster lookups on fileUploadId
        Index(value = ["location"], unique = true),
        Index(value = ["sLimit", "nLimit", "wLimit", "eLimit"]) // Compound index for lat/long limit-based queries
    ]
)
data class SealColony(
    @PrimaryKey(autoGenerate = true) val colonyId: Int = 0,
    @ColumnInfo(name = "inOut") val inOut: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "nLimit") val nLimit: Double,
    @ColumnInfo(name = "sLimit") val sLimit: Double,
    @ColumnInfo(name = "wLimit") val wLimit: Double,
    @ColumnInfo(name = "eLimit") val eLimit: Double,
    @ColumnInfo(name = "adjLat") val adjLat: Double,
    @ColumnInfo(name = "adjLong") val adjLong: Double,
    @ColumnInfo(name = "fileUploadId") val fileUploadId: Long // Foreign key reference
)
