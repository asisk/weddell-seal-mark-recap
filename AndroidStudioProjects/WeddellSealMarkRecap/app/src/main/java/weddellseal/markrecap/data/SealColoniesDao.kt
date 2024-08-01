package weddellseal.markrecap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SealColoniesDao {

    @Query("SELECT location FROM sealColonies")
    fun getSealColonyNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE) //the suspend keyword means that coroutines are supported
    suspend fun insert(data: List<SealColony>)

}
