package weddellseal.markrecap.data

class WedCheckRepository (private val wedCheckDao: WedCheckDao) {
    fun findSeal(speno: Int) : WedCheckRecord {
        val sealRecord = wedCheckDao.lookupSealBySpeno(speno)
        return sealRecord
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var INSTANCE: WedCheckRepository? = null

        fun getInstance(wedCheckDao: WedCheckDao) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WedCheckRepository(wedCheckDao).also { INSTANCE = it }
            }
    }

}