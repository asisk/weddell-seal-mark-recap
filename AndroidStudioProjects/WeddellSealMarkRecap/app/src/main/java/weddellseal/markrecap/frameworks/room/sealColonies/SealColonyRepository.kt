package weddellseal.markrecap.frameworks.room.sealColonies

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class SealColonyRepository(
    private val sealColoniesDao: SealColoniesDao,
) {
    private val _colony = MutableStateFlow<SealColony?>(null)
    val colony: StateFlow<SealColony?> = _colony

    fun setColony(colony: SealColony?) {
        _colony.value = colony
    }

    private val _overrideAutoColony = MutableStateFlow(false)
    val overrideAutoColony: StateFlow<Boolean> = _overrideAutoColony

    fun setOverrideAutoColony(value: Boolean) {
        _overrideAutoColony.value = value
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

    // used to return a list of location names
    fun getColonyNamesList(): List<String> {
        return sealColoniesDao.getSealColonyNames()
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