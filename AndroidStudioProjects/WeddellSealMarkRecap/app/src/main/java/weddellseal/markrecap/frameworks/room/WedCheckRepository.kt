package weddellseal.markrecap.frameworks.room

import weddellseal.markrecap.ui.file.FileStatus
import weddellseal.markrecap.frameworks.room.files.FileUploadDao
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckDao
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord

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
           return try {
               val count = wedCheckDao.insertWedCheckRecords(fileUploadId, csvData)
               Result.success(count)
           } catch (e: Exception) {
               Result.failure(e)
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