package weddellseal.markrecap.domain.files.data

import androidx.room.TypeConverter
import weddellseal.markrecap.ui.admin.FileStatus

class FileStatusConverter {
    @TypeConverter
    fun fromStatus(value: FileStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): FileStatus = FileStatus.valueOf(value)
}