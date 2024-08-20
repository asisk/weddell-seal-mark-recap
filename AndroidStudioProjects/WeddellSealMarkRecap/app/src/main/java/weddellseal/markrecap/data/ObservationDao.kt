package weddellseal.markrecap.data

/*
 * Data access object. A mapping of SQL queries to functions.
 * When you use a DAO, you call the methods, and Room takes care of the rest.
 */

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface ObservationDao {
    //uses LiveData to display database entries in the UI
    @Query("SELECT * FROM observationLogs ORDER BY id DESC")
    fun loadAllObservations(): LiveData<List<ObservationLogEntry>>
    @Query("SELECT * FROM observationLogs")
    fun getObservationsForCSVWrite(): List<ObservationLogEntry>

    @Query("SELECT * FROM observationLogs WHERE id = :obsId")
    fun loadObsById(obsId: Int): LiveData<ObservationLogEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE) //the suspend keyword means that coroutines are supported
    suspend fun insert(log: ObservationLogEntry)

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertAll(logs: List<ObservationLogEntry?>?)

//  TODO, consider replacing insert with upsert to support a user
//   editing a record that's been submitted to the database
//   @Upsert

//    TODO, consider a deprecation flag
//    @Delete
//    suspend fun delete(log: ObservationLogEntry)
}
