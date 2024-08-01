package weddellseal.markrecap.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observers")
data class Observers(
    @PrimaryKey(autoGenerate = true) val observerId: Int = 0,
    @ColumnInfo(name = "initials") val initials: String
)