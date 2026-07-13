package weddellseal.markrecap.frameworks.room.wedCheck

/*
 * Data access object for historic data records for seals.
 */

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
    ): Int {
        var insertedCount = 0
        wedCheckRecords.forEach { record ->
            insertWedCheckRecord(record.copy(fileUploadId = fileUploadId))
            insertedCount++
        }
        return insertedCount
    }

    @Query("SELECT COUNT(*) FROM wedCheck")
    suspend fun getCount(): Int

    // LIMIT 1: Room expects a single row for a non-List return type. Without it, duplicate
    // tag matches (e.g. same ID on tag1 of one seal and tag2 of another) throw and the
    // caller clears the WedCheck match, leaving SPENO blank.
    @Query("SELECT * FROM wedCheck WHERE tagNumberOne = :lookupSealTagID OR tagNumberTwo = :lookupSealTagID LIMIT 1")
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
