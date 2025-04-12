package weddellseal.markrecap.frameworks.room

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
import androidx.room.TypeConverters
import weddellseal.markrecap.frameworks.room.files.FileStatusConverter
import weddellseal.markrecap.frameworks.room.files.FileTypeConverter
import weddellseal.markrecap.frameworks.room.files.FileUploadDao
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observers.Observers
import weddellseal.markrecap.frameworks.room.observations.ObservationDao
import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.frameworks.room.observers.ObserversDao
import weddellseal.markrecap.frameworks.room.sealColonies.SealColoniesDao
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckDao
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord

@Database(entities = [ObservationLogEntry::class, WedCheckRecord::class, SealColony::class, Observers::class, FileUploadEntity::class], version = 17, exportSchema = false)
@TypeConverters(FileStatusConverter::class, FileTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun observationDao(): ObservationDao
    abstract fun wedCheckDao(): WedCheckDao
    abstract fun fileUploadDao() : FileUploadDao
    abstract fun sealColoniesDao() : SealColoniesDao
    abstract fun observersDao() : ObserversDao

    //companion is visible to other classes
    companion object {
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
                ).fallbackToDestructiveMigration().build()
                //TODO remove the destructive mode once schema stable
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
