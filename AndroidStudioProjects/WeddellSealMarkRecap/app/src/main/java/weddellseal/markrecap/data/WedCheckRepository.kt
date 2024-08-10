package weddellseal.markrecap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WedCheckRepository(private val wedCheckDao: WedCheckDao) {
    fun findSealbyTagID(sealTagID: String): WedCheckRecord {
        return wedCheckDao.lookupSealByTagID(sealTagID)
    }

    fun findSealbySpeNo(speNo: Int): WedCheckRecord {
        return wedCheckDao.lookupSealBySpeNo(speNo)
    }

    fun getSealSpeNo(sealTagID: String): Int {
        return wedCheckDao.lookupSpeNoByTagID(sealTagID) ?: 0
    }

    suspend fun insertCsvData(csvData: List<WedCheckRecord>) {
        withContext(Dispatchers.IO) {
            wedCheckDao.insertAll(csvData)
        }
    }
}