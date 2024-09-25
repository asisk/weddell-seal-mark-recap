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
    @Query("SELECT * FROM observationLogs WHERE deletedAt IS NULL ORDER BY id DESC")
    fun loadCurrentObservations(): LiveData<List<ObservationLogEntry>>

    //uses LiveData to display database entries in the UI
    @Query("SELECT * FROM observationLogs ORDER BY id DESC")
    fun loadAllObservations(): LiveData<List<ObservationLogEntry>>

    @Query("SELECT * FROM observationLogs WHERE id = :obsId AND deletedAt IS NULL")
    fun loadObsById(obsId: Int): LiveData<ObservationLogEntry>

    @Query("SELECT * FROM observationLogs WHERE deletedAt IS NULL")
    suspend fun getCurrentObservations(): List<ObservationLogEntry>

    @Query("SELECT * FROM observationLogs ORDER BY insertedAt DESC ")
    suspend fun getObservationsForSeasonView(): List<ObservationLogEntry>

    @Query("SELECT COUNT(*) FROM observationLogs WHERE deletedAt IS NULL")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) //the suspend keyword means that coroutines are supported
    suspend fun insert(log: ObservationLogEntry)

    // Soft delete all records that haven't been deleted yet (where deletedAt is NULL)
    @Query("UPDATE observationLogs SET deletedAt = :deletedAt WHERE deletedAt IS NULL")
    suspend fun softDeleteObservations(deletedAt: Long = System.currentTimeMillis())

    // New query & functions to support editing an existing ObservationLogEntry
    @Query(
        """
        UPDATE observationLogs
        SET 
            speno = :speno,
            census_id = :censusID,
            latitude = :latitude,
            longitude = :longitude,
            age_class = :ageClass,
            sex = :sex,
            num_relatives = :numRelatives,
            old_tag_id_one = :oldTagIDOne,
            old_tag_id_two = :oldTagIDTwo,
            tag_id_one = :tagIDOne,
            tag_one_indicator = :tagOneIndicator,
            tag_id_two = :tagIDTwo,
            tag_two_indicator = :tagTwoIndicator,
            rel_tag_id_one = :relativeTagIDOne,
            rel_tag_id_two = :relativeTagIDTwo,
            seal_condition = :sealCondition,
            observer_initials = :observerInitials,
            flagged_entry = :flaggedEntry,
            tag_event = :tagEvent,
            weight = :weight,
            tissue_sampled = :tissueSampled,
            comments = :comments,
            colony = :colony,
            updatedAt = :updatedAt
        WHERE id = :id AND deletedAt IS NULL"""
    )
    suspend fun updateObservation(
        id: String,
        speno: String,
        censusID: String,
        latitude: String,
        longitude: String,
        ageClass: String,
        sex: String,
        numRelatives: String,
        oldTagIDOne: String,
        oldTagIDTwo: String,
        tagIDOne: String,
        tagOneIndicator: String,
        tagIDTwo: String,
        tagTwoIndicator: String,
        relativeTagIDOne: String,
        relativeTagIDTwo: String,
        sealCondition: String,
        observerInitials: String,
        flaggedEntry: String,
        tagEvent: String,
        weight: String,
        tissueSampled: String,
        comments: String,
        colony: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    // Wrapper function to update fields based on the ObservationLogEntry data
    suspend fun updateObservationLogEntry(log: ObservationLogEntry) {
        updateObservation(
            id = log.id.toString(),
            speno = log.speno,
            censusID = log.censusID,
            latitude = log.latitude,
            longitude = log.longitude,
            ageClass = log.ageClass,
            sex = log.sex,
            numRelatives = log.numRelatives,
            oldTagIDOne = log.oldTagIDOne,
            oldTagIDTwo = log.oldTagIDTwo,
            tagIDOne = log.tagIDOne,
            tagOneIndicator = log.tagOneIndicator,
            tagIDTwo = log.tagIDTwo,
            tagTwoIndicator = log.tagTwoIndicator,
            relativeTagIDOne = log.relativeTagIDOne,
            relativeTagIDTwo = log.relativeTagIDTwo,
            sealCondition = log.sealCondition,
            observerInitials = log.observerInitials,
            flaggedEntry = log.flaggedEntry,
            tagEvent = log.tagEvent,
            weight = log.weight,
            tissueSampled = log.tissueSampled,
            comments = log.comments,
            colony = log.colony,
            updatedAt = System.currentTimeMillis()
        )
    }
}
