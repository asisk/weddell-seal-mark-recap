package weddellseal.markrecap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ObserversDao {

    @Query("SELECT initials FROM observers")
    fun getObserverInitials(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: List<Observers>)

    @Query("DELETE FROM observers")
    suspend fun clearObserversTable()

}