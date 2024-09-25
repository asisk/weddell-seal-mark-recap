package weddellseal.markrecap.data

/*
 * Data access object for historic data records for seals.
 */

import android.database.sqlite.SQLiteException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction


@Dao
interface WedCheckDao {

    // If a new record is inserted with the same speno, it will replace the existing one.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWedCheckRecord(wedCheckRecord: WedCheckRecord)

    @Transaction
    suspend fun insertWedCheckRecords(
        fileUploadId: Long,
        wedCheckRecords: List<WedCheckRecord>
    ): Result<Int> {
        return try {
            var insertedCount = 0
            wedCheckRecords.forEach { record ->
                insertWedCheckRecord(record.copy(fileUploadId = fileUploadId))
                insertedCount++

            }
            Result.Success(insertedCount)
        } catch (e: SQLiteException) {
            // Handle database exceptions and return a failure result
            Result.Error("Database error: ${e.localizedMessage}")
        }
    }

    @Query("SELECT COUNT(*) FROM wedCheck")
    suspend fun getCount(): Int

    @Query("SELECT * FROM wedCheck WHERE tagNumberOne = :lookupSealTagID OR tagNumberTwo = :lookupSealTagID")
    fun lookupSealByTagID(lookupSealTagID: String): WedCheckRecord

    @Query("SELECT * FROM wedCheck WHERE speno = :speNo")
    fun lookupSealBySpeNo(speNo: Int): WedCheckRecord

    @Query("SELECT speno FROM wedCheck WHERE tagNumberOne = :lookupSealTagID OR tagNumberTwo = :lookupSealTagID")
    fun lookupSpeNoByTagID(lookupSealTagID: String): Int

    @Query("SELECT * FROM wedCheck WHERE fileUploadId = :fileUploadId")
    suspend fun getRecordsByFileUploadId(fileUploadId: Long): List<WedCheckRecord>

//    @Query("DELETE FROM wedCheck WHERE speno = :speno")
//    suspend fun deleteById(speno: Int)

//    @Query("DELETE FROM wedCheck")
//    suspend fun clearWedCheckTable()
}
