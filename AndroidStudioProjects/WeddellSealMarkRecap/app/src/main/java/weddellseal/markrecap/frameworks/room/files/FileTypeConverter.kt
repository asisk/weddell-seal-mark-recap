package weddellseal.markrecap.frameworks.room.files

import androidx.room.TypeConverter
import weddellseal.markrecap.ui.file.FileType

class FileTypeConverter {
    @TypeConverter
    fun fromType(value: FileType): String = value.name

    @TypeConverter
    fun toType(value: String): FileType = FileType.valueOf(value)
}