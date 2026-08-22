package weddellseal.markrecap.frameworks.room

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.frameworks.room.files.FileUploadDao
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.sealColonies.SealColoniesDao
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var fileUploadDao: FileUploadDao
    private lateinit var sealColonyDao: SealColoniesDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        fileUploadDao = db.fileUploadDao()
        sealColonyDao = db.sealColoniesDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertValidForeignKey() = runBlocking {
        val fileUploadId = fileUploadDao.insertFileUpload(
            FileUploadEntity(
                fileType = FileType.OBSERVERS,
                fileAction = FileAction.UPLOAD.name,
                filename = "testfile.csv",
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )

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

        val insertedIds = sealColonyDao.insertColonyRecords(fileUploadId, listOf(sealColony))

        assertTrue(insertedIds > 0)
        assertEquals(1, sealColonyDao.getCount())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun testInsertInvalidForeignKey(): Unit = runBlocking {
        val sealColony = SealColony(
            inOut = "in",
            location = "Invalid Location",
            nLimit = 45.0,
            sLimit = 40.0,
            wLimit = 30.0,
            eLimit = 35.0,
            adjLat = 42.0,
            adjLong = 32.0,
            fileUploadId = 9999
        )

        sealColonyDao.insertColonyRecords(9999, listOf(sealColony))
    }

    @Test
    fun testClearColoniesTableRemovesRows() = runBlocking {
        val fileUploadId = fileUploadDao.insertFileUpload(
            FileUploadEntity(
                fileType = FileType.OBSERVERS,
                fileAction = FileAction.UPLOAD.name,
                filename = "testfile.csv",
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )

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
        assertEquals(1, sealColonyDao.getCount())

        sealColonyDao.clearColoniesTable()
        assertEquals(0, sealColonyDao.getCount())
    }

    @Test
    fun testReplaceColonyRecordsRollsBackOnInsertFailure() = runBlocking {
        val fileUploadId = fileUploadDao.insertFileUpload(
            FileUploadEntity(
                fileType = FileType.OBSERVERS,
                fileAction = FileAction.UPLOAD.name,
                filename = "testfile.csv",
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )
        val existing = SealColony(
            inOut = "in",
            location = "Existing Location",
            nLimit = 45.0,
            sLimit = 40.0,
            wLimit = 30.0,
            eLimit = 35.0,
            adjLat = 42.0,
            adjLong = 32.0,
            fileUploadId = fileUploadId
        )
        sealColonyDao.insertColonyRecords(fileUploadId, listOf(existing))

        val invalidReplacement = existing.copy(
            colonyId = 0,
            location = "Replacement Location",
            fileUploadId = 9999
        )
        try {
            sealColonyDao.replaceColonyRecords(9999, listOf(invalidReplacement))
            fail("Expected foreign key failure to abort the replacement")
        } catch (_: SQLiteConstraintException) {
            // expected: insert fails after the table was cleared inside the transaction
        }

        assertEquals(1, sealColonyDao.getCount())
        assertEquals(
            "Existing Location",
            sealColonyDao.getRecordsByFileUploadId(fileUploadId).single().location
        )
    }

    @Test
    fun testQueryRelatedData() = runBlocking {
        val fileUploadId = fileUploadDao.insertFileUpload(
            FileUploadEntity(
                fileType = FileType.OBSERVERS,
                fileAction = FileAction.UPLOAD.name,
                filename = "testfile.csv",
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )

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

        val relatedSealColonies = sealColonyDao.getRecordsByFileUploadId(fileUploadId)

        assertTrue(relatedSealColonies.isNotEmpty())
    }
}
