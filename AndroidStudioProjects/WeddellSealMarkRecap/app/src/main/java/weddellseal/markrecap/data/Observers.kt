package weddellseal.markrecap.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "observers",
    foreignKeys = [
        ForeignKey(
            entity = FileUploadEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("fileUploadId"),
            onDelete = ForeignKey.CASCADE // Optional: delete related rows if the file record is deleted
        )
    ],
    indices = [
        Index(value = ["initials"], unique = true),
        Index(value = ["fileUploadId"]), // Index for faster lookups on fileUploadId
    ]
)
data class Observers(
    @PrimaryKey(autoGenerate = true) val observerId: Int = 0,
    @ColumnInfo(name = "initials") val initials: String,
    @ColumnInfo(name = "fileUploadId") val fileUploadId: Long // Foreign key reference
)