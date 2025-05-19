package weddellseal.markrecap.frameworks.room.observers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ObserversRepository(
    private val observersDao: ObserversDao,
) {

    // used to refresh the database with a current list of observers
    suspend fun insertObserversData(fileUploadId: Long, csvData: List<Observers>): Int {
        return withContext(Dispatchers.IO) {
            observersDao.insertObserversRecords(fileUploadId, csvData)
        }
    }

    fun getObserverInitials(): List<String> {
        return observersDao.getObserverInitials()
    }

    suspend fun deleteObserversByFileUpload(fileUploadId: Long) {
        withContext(Dispatchers.IO) {
            observersDao.deleteById(fileUploadId)
        }
    }

    suspend fun clearObserversData() {
        withContext(Dispatchers.IO) {
            observersDao.clearObserversTable()
        }
    }
}