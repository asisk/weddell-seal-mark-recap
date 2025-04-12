package weddellseal.markrecap.frameworks.room.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import weddellseal.markrecap.ui.file.FileAction
import weddellseal.markrecap.ui.file.FileStatus
import weddellseal.markrecap.ui.file.FileType

@Entity(
    tableName = "fileUploads",
    indices = [Index(value = ["filename"])]
)
data class FileUploadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // safer as Long

    @ColumnInfo(name = "fileType")
    val fileType: FileType, // needs @TypeConverter

    @ColumnInfo(name = "fileAction")
    val fileAction: String = FileAction.UPLOAD.name,

    @ColumnInfo(name = "filename")
    val filename: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "status")
    val status: FileStatus = FileStatus.IDLE, // needs @TypeConverter

    @ColumnInfo(name = "statusMessage")
    val statusMessage: String? = null,

    @ColumnInfo(name = "recordCount")
    val recordCount: Int = 0,
)