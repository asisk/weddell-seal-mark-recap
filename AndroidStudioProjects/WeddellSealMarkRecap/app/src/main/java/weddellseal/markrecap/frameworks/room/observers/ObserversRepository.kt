package weddellseal.markrecap.frameworks.room.observers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ObserversRepository(
    private val observersDao: ObserversDao,
) {

    val observersList: Flow<List<String>> = observersDao.getObserverInitials()

    // used to refresh the database with a current list of observers
    suspend fun insertObserversData(fileUploadId: Long, csvData: List<Observers>): Int {
        return withContext(Dispatchers.IO) {
            observersDao.insertObserversRecords(fileUploadId, csvData)
        }
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