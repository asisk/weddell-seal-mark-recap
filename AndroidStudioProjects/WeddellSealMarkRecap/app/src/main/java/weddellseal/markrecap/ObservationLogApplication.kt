package weddellseal.markrecap

/*
 * ObservationLogApplication sets up the repository
 * and permissions when the app is first opened
 * and creates the csv writer
 */

import android.app.Application
import weddellseal.markrecap.data.AppDatabase
import weddellseal.markrecap.data.FileUploadDao
import weddellseal.markrecap.data.ObservationDao
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.ObserversDao
import weddellseal.markrecap.data.SealColoniesDao
import weddellseal.markrecap.data.SupportingDataRepository
import weddellseal.markrecap.data.WedCheckDao
import weddellseal.markrecap.data.WedCheckRepository

class ObservationLogApplication : Application() {
    private lateinit var db : AppDatabase
    lateinit var observationRepo: ObservationRepository
    private lateinit var wedCheckRepo: WedCheckRepository
    lateinit var supportingDataRepo: SupportingDataRepository
    lateinit var permissions: PermissionManager

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        observationRepo = ObservationRepository(db.observationDao())
        wedCheckRepo = WedCheckRepository(db.wedCheckDao(), db.fileUploadDao())
        supportingDataRepo = SupportingDataRepository(db.observersDao(), db.sealColoniesDao(), db.fileUploadDao())
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