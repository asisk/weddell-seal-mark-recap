package weddellseal.markrecap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WedCheckRepository(private val wedCheckDao: WedCheckDao, private val fileUploadDao: FileUploadDao) {
    fun findSealbyTagID(sealTagID: String): WedCheckRecord {
        return wedCheckDao.lookupSealByTagID(sealTagID)
    }

    fun findSealbySpeNo(speNo: Int): WedCheckRecord {
        return wedCheckDao.lookupSealBySpeNo(speNo)
    }

    fun getSealSpeNo(sealTagID: String): Int {
        return wedCheckDao.lookupSpeNoByTagID(sealTagID) ?: 0
    }

    suspend fun insertCsvData(fileUploadId: Long, csvData: List<WedCheckRecord>): Result<Int> {
        return withContext(Dispatchers.IO) {
            wedCheckDao.insertWedCheckRecords(fileUploadId, csvData)
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