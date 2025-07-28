package weddellseal.markrecap.frameworks.room.observations

/*
 * Data access object. A mapping of SQL queries to functions.
 * When you use a DAO, you call the methods, and Room takes care of the rest.
 */

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservationDao {

    @Query("SELECT * FROM observationLogs WHERE deletedAt IS NULL ORDER BY id DESC")
    fun getCurrentObservationsOrdered(): Flow<List<ObservationRecord>>

    @Query("SELECT * FROM observationLogs ORDER BY id DESC")
    fun getAllObservationsForSeasonOrdered(): Flow<List<ObservationRecord>>

    @Query("SELECT COUNT(*) FROM observationLogs WHERE deletedAt IS NULL")
    suspend fun getCount(): Int

    @Upsert
    suspend fun upsert(log: ObservationRecord)

    // Delete a single record
    @Query("DELETE FROM observationLogs WHERE id = :id")
    suspend fun delete(id: Int)

    // Soft delete current records that haven't been deleted yet (where deletedAt is NULL)
    @Query("UPDATE observationLogs SET deletedAt = :deletedAt WHERE deletedAt IS NULL")
    suspend fun softDeleteObservations(deletedAt: Long = System.currentTimeMillis())

    // Delete all records
    @Query("DELETE FROM observationLogs")
    suspend fun deleteAll()
}
