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

    @Query("SELECT * FROM wedCheck WHERE speno = :lookupSpeno")
    // TODO rename to something that indicates it's coming from the SQlite database
    fun lookupSealBySpeno(lookupSpeno: Int): WedCheckRecord

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<WedCheckRecord>)

}
