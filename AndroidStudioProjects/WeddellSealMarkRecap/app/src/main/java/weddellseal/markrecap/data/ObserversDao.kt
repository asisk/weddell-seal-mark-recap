package weddellseal.markrecap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ObserversDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) //the suspend keyword means that coroutines are supported
    suspend fun insertObserversRecord(observer: Observers): Long // Return the row ID

    @Transaction
    suspend fun insertObserversRecords(fileUploadId: Long, observers: List<Observers>): Int {
        var insertedCount = 0
        observers.forEach { record ->
            val result = insertObserversRecord(record.copy(fileUploadId = fileUploadId))
            if (result > 0) {
                insertedCount++ // Increment the count for successful insertions
            }
        }
        return insertedCount
    }

    @Query("SELECT COUNT(*) FROM observers")
    suspend fun getCount(): Int

    @Query("SELECT initials FROM observers")
    fun getObserverInitials(): List<String>

    @Query("SELECT * FROM observers WHERE fileUploadId = :fileUploadId")
    suspend fun getRecordsByFileUploadId(fileUploadId: Long): List<Observers>

    @Query("DELETE FROM observers WHERE observerId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM observers")
    suspend fun clearObserversTable()
}