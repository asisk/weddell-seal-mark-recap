package weddellseal.markrecap.domain.files.data

import androidx.room.TypeConverter
import weddellseal.markrecap.ui.admin.FileType

class FileTypeConverter {
    @TypeConverter
    fun fromType(value: FileType): String = value.name

    @TypeConverter
    fun toType(value: String): FileType = FileType.valueOf(value)
}