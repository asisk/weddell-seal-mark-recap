package weddellseal.markrecap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WedCheckRepository(private val wedCheckDao: WedCheckDao) {
    fun findSeal(sealTagID: String): WedCheckRecord {
        return wedCheckDao.lookupSealByTagID(sealTagID)
    }

    suspend fun insertCsvData(csvData: List<WedCheckRecord>) {
        withContext(Dispatchers.IO) {
            wedCheckDao.insertAll(csvData)
        }
    }

    //TODO, consider cleaning this up if unused
    companion object {

        // For Singleton instantiation
        @Volatile
        private var INSTANCE: WedCheckRepository? = null

        fun getInstance(wedCheckDao: WedCheckDao) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WedCheckRepository(wedCheckDao).also { INSTANCE = it }
            }
    }

}