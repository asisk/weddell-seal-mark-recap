package weddellseal.markrecap.data

// Import necessary dependencies
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var fileUploadDao: FileUploadDao
    private lateinit var sealColonyDao: SealColoniesDao
    private lateinit var observersDao: ObserversDao
    private lateinit var wedCheckDao: WedCheckDao

    @Before
    fun setUp() {
        // Create an in-memory version of the database
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        fileUploadDao = db.fileUploadDao()
        observersDao = db.observersDao()
        sealColonyDao = db.sealColoniesDao()
        wedCheckDao = db.wedCheckDao()
    }


    @Test
    fun testInsertValidForeignKey() = runBlocking {
        // Insert a FileUploadEntity first
        val fileUploadId = fileUploadDao.insertFileUpload(
            FileUploadEntity(id = 0, filename = "testfile.csv")
        )

        // Insert a SealColony record with the valid fileUploadId
        val sealColony = SealColony(
            inOut = "in",
            location = "Test Location",
            nLimit = 45.0,
            sLimit = 40.0,
            wLimit = 30.0,
            eLimit = 35.0,
            adjLat = 42.0,
            adjLong = 32.0,
            fileUploadId = fileUploadId // Valid foreign key
        )

       val insertedIds =  sealColonyDao.insertColonyRecords(fileUploadId, listOf(sealColony))

        // Assert that the insert was successful
        assertTrue(insertedIds > 0)
    }


    @Test(expected = SQLiteConstraintException::class)
    fun testInsertInvalidForeignKey(): Unit = runBlocking {
        // Insert a FileUploadEntity first
        val fileUploadId: Long = -1

        // Attempt to insert a SealColony record with an invalid fileUploadId (e.g., 9999, which does not exist)
        val sealColony = SealColony(
            inOut = "in",
            location = "Invalid Location",
            nLimit = 45.0,
            sLimit = 40.0,
            wLimit = 30.0,
            eLimit = 35.0,
            adjLat = 42.0,
            adjLong = 32.0,
            fileUploadId = 9999 // Invalid foreign key
        )

        sealColonyDao.insertColonyRecords(fileUploadId, listOf(sealColony))

        // The test will pass if the SQLiteConstraintException is thrown
    }


    @Test
    fun testCascadeDelete() = runBlocking {
        // Insert a FileUploadEntity
        val fileUploadId = fileUploadDao.insertFileUpload(
            FileUploadEntity(id = 0, filename = "testfile.csv")
        )

        // Insert a SealColony record with the valid fileUploadId
        val sealColony = SealColony(
            inOut = "in",
            location = "Test Location",
            nLimit = 45.0,
            sLimit = 40.0,
            wLimit = 30.0,
            eLimit = 35.0,
            adjLat = 42.0,
            adjLong = 32.0,
            fileUploadId = fileUploadId
        )
        sealColonyDao.insertColonyRecords(fileUploadId, listOf(sealColony))

        // Assert that the SealColony has also been deleted
        val sealColonyCount = sealColonyDao.getCount()
        assertTrue(sealColonyCount == 0)
    }

    @Test
    fun testQueryRelatedData() = runBlocking {
        // Insert a FileUploadEntity
        val fileUploadId = fileUploadDao.insertFileUpload(
            FileUploadEntity(id = 0, filename = "testfile.csv")
        )

        // Insert a SealColony record with the valid fileUploadId
        val sealColony = SealColony(
            inOut = "in",
            location = "Test Location",
            nLimit = 45.0,
            sLimit = 40.0,
            wLimit = 30.0,
            eLimit = 35.0,
            adjLat = 42.0,
            adjLong = 32.0,
            fileUploadId = fileUploadId
        )
        sealColonyDao.insertColonyRecords(fileUploadId, listOf(sealColony))

        // Query SealColony records related to the FileUploadEntity
        val relatedSealColonies = sealColonyDao.getRecordsByFileUploadId(fileUploadId)

        // Assert that we have the related SealColony
        assertTrue(relatedSealColonies.isNotEmpty())
    }

    @After
    fun tearDown() {
        db.close()
    }
}