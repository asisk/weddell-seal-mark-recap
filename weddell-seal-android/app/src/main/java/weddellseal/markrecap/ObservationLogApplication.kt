package weddellseal.markrecap

/*
 * ObservationLogApplication sets up the repository
 * and permissions when the app is first opened
 * and creates the csv writer
 */

import android.app.Application
import weddellseal.markrecap.frameworks.room.AppDatabase
import weddellseal.markrecap.frameworks.room.files.FilesRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.files.FileUploadDao
import weddellseal.markrecap.frameworks.room.observations.ObservationDao
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observers.ObserversDao
import weddellseal.markrecap.frameworks.room.sealColonies.SealColoniesDao
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckDao

class ObservationLogApplication : Application() {
    private lateinit var db: AppDatabase
    lateinit var observationRepo: ObservationRepository
    private lateinit var wedCheckRepo: WedCheckRepository
    lateinit var supportingDataRepo: FilesRepository
    lateinit var permissions: PermissionManager

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        observationRepo = ObservationRepository(db.observationDao())
        wedCheckRepo = WedCheckRepository(db.wedCheckDao(), db.fileUploadDao())
        supportingDataRepo = FilesRepository(db.fileUploadDao())
        permissions = PermissionManager(this)
    }

    fun getFileUploadDao(): FileUploadDao {
        return db.fileUploadDao()
    }

    fun getWedCheckDao(): WedCheckDao {
        return db.wedCheckDao()
    }

    fun getObservationDao(): ObservationDao {
        return db.observationDao()
    }

    fun getSealColoniesDao(): SealColoniesDao {
        return db.sealColoniesDao()
    }

    fun getObserversDao(): ObserversDao {
        return db.observersDao()
    }
}