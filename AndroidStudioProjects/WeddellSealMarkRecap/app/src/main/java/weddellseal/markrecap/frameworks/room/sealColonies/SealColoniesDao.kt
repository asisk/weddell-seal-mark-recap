package weddellseal.markrecap.frameworks.room.sealColonies

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SealColoniesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) //the suspend keyword means that coroutines are supported
    suspend fun insertSealColonyRecord(sealColonies: SealColony): Long // Return the row ID

    @Transaction
    suspend fun insertColonyRecords(fileUploadId: Long, sealColonies: List<SealColony>): Int {
        var insertedCount = 0
        sealColonies.forEach { record ->
            val result = insertSealColonyRecord(record.copy(fileUploadId = fileUploadId))
            if (result > 0) {
                insertedCount++ // Increment the count for successful insertions
            }
        }
        return insertedCount
    }

    @Query("SELECT COUNT(*) FROM sealColonies")
    suspend fun getCount(): Int

    @Query("SELECT location FROM sealColonies")
    fun getSealColonyNames(): List<String>

    @Query("SELECT * FROM sealColonies WHERE fileUploadId = :fileUploadId")
    suspend fun getRecordsByFileUploadId(fileUploadId: Long): List<SealColony>

    // The query checks whether the input latitude and longitude
    // fall within the boundaries (sLimit, nLimit, wLimit, and eLimit).
    // :adjLat is the input latitude.
    // :adjLong is the input longitude.
    @Query("SELECT * FROM sealColonies WHERE :deviceLatitude BETWEEN sLimit AND nLimit AND :deviceLongitude BETWEEN wLimit AND eLimit")
    suspend fun findColonyByLatLong(deviceLatitude: Double, deviceLongitude: Double): SealColony?

    @Query("DELETE FROM sealColonies WHERE colonyId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sealColonies")
    suspend fun clearColoniesTable()
}
