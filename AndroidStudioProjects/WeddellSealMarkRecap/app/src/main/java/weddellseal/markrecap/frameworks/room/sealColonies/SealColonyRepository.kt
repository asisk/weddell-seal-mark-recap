package weddellseal.markrecap.frameworks.room.sealColonies

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SealColonyRepository(
    private val sealColoniesDao: SealColoniesDao,
) {
    val coloniesList: Flow<List<String>> = sealColoniesDao.getSealColonyNames()

    // used to refresh the database with a current list of locations
    suspend fun insertColoniesData(fileUploadId: Long, csvData: List<SealColony>): Int {
        return withContext(Dispatchers.IO) {
            sealColoniesDao.insertColonyRecords(
                fileUploadId,
                csvData
            )
        }
    }

    // used to search for a seal colony by passing in the device latitude and longitude
    suspend fun findColony(searchLatitude: Double, searchLongitude: Double): SealColony? {
        return sealColoniesDao.findColonyByLatLong(searchLatitude,searchLongitude)
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
}