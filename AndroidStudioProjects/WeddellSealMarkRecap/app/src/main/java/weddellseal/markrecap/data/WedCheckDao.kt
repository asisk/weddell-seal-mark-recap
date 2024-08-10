package weddellseal.markrecap.data

/*
 * Data access object for historic data records for seals.
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface WedCheckDao {

    @Query("SELECT * FROM wedCheck WHERE tagNumberOne = :lookupSealTagID OR tagNumberTwo = :lookupSealTagID")
    fun lookupSealByTagID(lookupSealTagID: String): WedCheckRecord

    @Query("SELECT * FROM wedCheck WHERE speno = :speNo")
    fun lookupSealBySpeNo(speNo: Int): WedCheckRecord

    @Query("SELECT speno FROM wedCheck WHERE tagNumberOne = :lookupSealTagID OR tagNumberTwo = :lookupSealTagID")
    fun lookupSpeNoByTagID(lookupSealTagID: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<WedCheckRecord>)

}
