package weddellseal.markrecap.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fileUploads",
    indices = [Index(value = ["filename"])] // Optional index on filename if frequent lookups are done by filename
)
data class FileUploadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "filename") val filename: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "status") val status: String = "pending" // New column to track upload status
)