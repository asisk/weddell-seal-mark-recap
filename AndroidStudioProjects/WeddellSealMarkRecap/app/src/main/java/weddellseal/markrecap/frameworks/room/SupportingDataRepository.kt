package weddellseal.markrecap.frameworks.room

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import weddellseal.markrecap.ui.file.FileAction
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.frameworks.room.files.FileUploadDao
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observers.Observers
import weddellseal.markrecap.frameworks.room.observers.ObserversDao
import weddellseal.markrecap.frameworks.room.sealColonies.SealColoniesDao
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.ui.file.FileStatus


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
    fun getColonyNamesList(): List<String> {
        return sealColoniesDao.getSealColonyNames()
    }

    fun FileUploadEntity.toFileState(): FileState {
        return FileState(
            fileType = fileType.name,
            action = FileAction.valueOf(fileAction),
            status = status,
            errorMessage = statusMessage,
            onUploadClick = {
                //this value is intentionally not populated in the database
            },
            onDownloadClick = {
                //this value is intentionally not populated in the database
            },
            lastFilename = filename
        )
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

    suspend fun updateFileUploadStatus(fileUploadId: Long, status: FileStatus, recordCount: Int, statusMsg: String) {
        fileUploadDao.updateFileUploadStatus(fileUploadId, status, recordCount, statusMsg)
    }
}