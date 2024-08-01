package weddellseal.markrecap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SupportingDataRepository(
    private val observersDao: ObserversDao,
    private val sealColoniesDao: SealColoniesDao
) {

    fun getObserverInitials(): List<String> {
        return observersDao.getObserverInitials()
    }

    // used to return a list of location names
    fun getLocations(): List<String> {
        return sealColoniesDao.getSealColonyNames()
    }

    // used to refresh the database with a current list of observers
    suspend fun insertObserversData(csvData: List<Observers>) {
        withContext(Dispatchers.IO) {
            observersDao.insert(csvData)
        }
    }

    // used to refresh the database with a current list of locations
    suspend fun insertColoniesData(csvData: List<SealColony>) {
        withContext(Dispatchers.IO) {
            sealColoniesDao.insert(csvData)
        }
    }

    //TODO, remove below if unneeded

//    companion object {
//
//        // For Singleton instantiation
//        @Volatile
//        private var INSTANCE: SupportingDataRepository? = null
//
//        fun getInstance(sealColoniesDao: SealColoniesDao) =
//            INSTANCE ?: synchronized(this) {
//                INSTANCE ?: SupportingDataRepository(sealColoniesDao).also { INSTANCE = it }
//            }
//    }

}