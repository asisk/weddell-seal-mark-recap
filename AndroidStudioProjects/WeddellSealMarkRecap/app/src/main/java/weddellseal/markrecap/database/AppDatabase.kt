package weddellseal.markrecap.database

/*
 * Simplifies database work and serves as an access point to the
 * underlying SQLite database (hides SQLiteOpenHelper).
 * The Room database uses the ObservationDAO to issue queries
 * to the SQLite database.
 */


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ObservationLogEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun observationDao(): ObservationDao

    //companion is visible to other classes
    companion object {
//       const val DB_NAME = "DailyWeddellSealObservations"
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "observations_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
