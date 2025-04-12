package weddellseal.markrecap.frameworks.room.files

import androidx.room.TypeConverter
import weddellseal.markrecap.ui.file.FileStatus

class FileStatusConverter {
    @TypeConverter
    fun fromStatus(value: FileStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): FileStatus = FileStatus.valueOf(value)
}
