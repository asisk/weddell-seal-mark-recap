package weddellseal.markrecap

/*
 * Simplifies database work and serves as an access point to the
 * underlying SQLite database (hides SQLiteOpenHelper).
 * The Room database uses the ObservationDAO to issue queries
 * to the SQLite database.
 */


import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ObservationLogEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "DailyWeddellSealObservations"
    }

    abstract fun observationDao(): ObservationDao
}
