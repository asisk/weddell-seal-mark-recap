package weddellseal.markrecap.database

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
    //selects all from the observationLogs table
//    @Query("SELECT * FROM observationLogs ORDER BY date DESC")
//    suspend fun getAll(): Any?

    //this uses LiveData which is used by the Home Screen to display database entries for observations
    @Query("SELECT * FROM observationLogs ORDER BY id DESC")
    fun loadAllObservations(): LiveData<List<ObservationLogEntry>>
    @Query("SELECT * FROM observationLogs")
    fun getObservationsForCSVWrite(): List<ObservationLogEntry?>

    @Query("select * from observationLogs where id = :obsId")
    fun loadObsById(obsId: Int): LiveData<ObservationLogEntry?>?

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
