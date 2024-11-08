package weddellseal.markrecap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


class SupportingDataRepository(
    private val observersDao: ObserversDao,
    private val sealColoniesDao: SealColoniesDao,
    private val fileUploadDao: FileUploadDao
) {

    // Return flow of file uploads from the DAO
    val fileUploads: Flow<List<FileUploadEntity>> = fileUploadDao.getAllFileUploads()

    // used to refresh the database with a current list of observers
    suspend fun insertObserversData(fileUploadId: Long, csvData: List<Observers>): Int {
        return withContext(Dispatchers.IO) {
            observersDao.insertObserversRecords(fileUploadId, csvData)
        }
    }

    // used to refresh the database with a current list of locations
    suspend fun insertColoniesData(fileUploadId: Long, csvData: List<SealColony>): Int {
        return withContext(Dispatchers.IO) {
            sealColoniesDao.insertColonyRecords(
                fileUploadId,
                csvData
            )
        }
    }

    fun getObserverInitials(): List<String> {
        return observersDao.getObserverInitials()
    }

    // used to return a list of location names
    fun getLocations(): List<String> {
        return sealColoniesDao.getSealColonyNames()
    }

    // used to search for a seal colony by passing in the device latitude and longitude
    suspend fun findColony(searchLatitude: Double, searchLongitude: Double): SealColony? {
        return sealColoniesDao.findColonyByLatLong(searchLatitude,searchLongitude)
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

    suspend fun deleteSealColoniesByFileUpload(fileUploadId: Long) {
        withContext(Dispatchers.IO) {
            sealColoniesDao.deleteById(fileUploadId)
        }
    }

    suspend fun clearColonyData() {
        withContext(Dispatchers.IO) {
            sealColoniesDao.clearColoniesTable()
        }
    }

    // Insert file upload entry and get the fileUploadId
    suspend fun insertFileUpload(fileUpload: FileUploadEntity): Long {
        return fileUploadDao.insertFileUpload(fileUpload)
    }

    suspend fun updateFileUploadStatus(fileUploadId: Long, status: String) {
        fileUploadDao.updateFileUploadStatus(fileUploadId, status)
    }

}