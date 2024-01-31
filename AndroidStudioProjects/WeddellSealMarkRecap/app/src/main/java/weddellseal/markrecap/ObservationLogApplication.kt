package weddellseal.markrecap

/*
 * ObservationLogApplication sets up the repository
 * and permissions when the app is first opened
 * and creates the csv writer
 */

import android.app.Application
import weddellseal.markrecap.data.AppDatabase
import weddellseal.markrecap.data.ObservationRepository

class ObservationLogApplication : Application() {
    private lateinit var db : AppDatabase
    lateinit var observationRepo: ObservationRepository
    lateinit var permissions: PermissionManager

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        observationRepo = ObservationRepository(db.observationDao())
        permissions = PermissionManager(this)
    }
}