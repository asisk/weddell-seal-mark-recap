package weddellseal.markrecap

/*
 * ObservationLogApplication sets up the repository
 * and permissions when the app is first opened
 * and creates the csv writer
 */

import android.app.Application

class ObservationLogApplication : Application() {
    lateinit var observationSaver: ObservationSaverRepository
    lateinit var permissions: PermissionManager
    override fun onCreate() {
        super.onCreate()

        observationSaver = ObservationSaverRepository(this, this.contentResolver)
        permissions = PermissionManager(this)
    }
}