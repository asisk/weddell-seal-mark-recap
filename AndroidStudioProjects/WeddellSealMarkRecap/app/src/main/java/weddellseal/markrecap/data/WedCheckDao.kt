package weddellseal.markrecap.data

/*
 * Data access object for historic data records for seals.
 */

import androidx.room.Dao
import androidx.room.Query


@Dao
interface WedCheckDao {

    @Query("SELECT * FROM wedCheck WHERE speno = :lookupSpeno")
    fun lookupSealBySpeno(lookupSpeno: Int): WedCheckRecord

}
