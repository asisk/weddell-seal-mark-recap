package weddellseal.markrecap.frameworks.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observers.Observers
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryRoundTripTest {

    private lateinit var db: AppDatabase
    private lateinit var observationRepository: ObservationRepository
    private lateinit var wedCheckRepository: WedCheckRepository
    private lateinit var sealColonyRepository: SealColonyRepository
    private lateinit var observersRepository: ObserversRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        observationRepository = ObservationRepository(db.observationDao())
        wedCheckRepository = WedCheckRepository(db.wedCheckDao(), db.fileUploadDao())
        sealColonyRepository = SealColonyRepository(db.sealColoniesDao())
        observersRepository = ObserversRepository(db.observersDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observationWriteThenCount() = runBlocking {
        val record = TestFixtures.minimalObservationRecord()
        observationRepository.writeObservation(record)
        assertEquals(1, db.observationDao().getCount())
    }

    @Test
    fun observationFlowEmitsWrittenRow() = runBlocking {
        observationRepository.writeObservation(TestFixtures.minimalObservationRecord())
        val list = observationRepository.currentObservationsDescByID.first()
        assertEquals(1, list.size)
        assertEquals("123A", list[0].tagIDOne)
    }

    @Test
    fun sealColonyRepositoryInsertAndFindByName() = runBlocking {
        val fileId = db.fileUploadDao().insertFileUpload(
            FileUploadEntity(
                fileType = FileType.OBSERVERS,
                fileAction = FileAction.UPLOAD.name,
                filename = "c.csv",
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )
        val colony = SealColony(
            inOut = "in",
            location = "UniqueColonyRepoTest",
            nLimit = 1.0,
            sLimit = -1.0,
            wLimit = 1.0,
            eLimit = 2.0,
            adjLat = 0.5,
            adjLong = 1.5,
            fileUploadId = fileId
        )
        val inserted = sealColonyRepository.insertColoniesData(fileId, listOf(colony))
        assertTrue(inserted > 0)
        val found = sealColonyRepository.findColonyByName("UniqueColonyRepoTest")
        assertEquals("UniqueColonyRepoTest", found?.location)
    }

    @Test
    fun observersRepositoryInsertAndFlow() = runBlocking {
        val fileId = db.fileUploadDao().insertFileUpload(
            FileUploadEntity(
                fileType = FileType.OBSERVERS,
                fileAction = FileAction.UPLOAD.name,
                filename = "o.csv",
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )
        observersRepository.insertObserversData(
            fileId,
            listOf(Observers(initials = "ZZ", fileUploadId = fileId))
        )
        val initials = observersRepository.observersList.first()
        assertTrue(initials.contains("ZZ"))
    }

    @Test
    fun wedCheckRepositoryInsertCsvData() = runBlocking {
        val fileId = db.fileUploadDao().insertFileUpload(
            FileUploadEntity(
                fileType = FileType.WEDCHECK,
                fileAction = FileAction.UPLOAD.name,
                filename = "WedCheck.csv",
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )
        val row = WedCheckRecord(
            speno = 4242,
            season = 2024,
            ageClass = "A",
            sex = "F",
            tagIdOne = "100A",
            tagIdTwo = "NA",
            comments = "",
            ageYears = 5,
            tissueSampled = "NA",
            pupinMassStudy = "NA",
            numPreviousPups = "NA",
            pupinTTStudy = "NA",
            momMassMeasurements = "NA",
            condition = "3",
            lastPhysio = "NA",
            population = "NA",
            fileUploadId = fileId,
            latitude = -77.0,
            longitude = 166.0
        )
        val result = wedCheckRepository.insertCsvData(fileId, listOf(row))
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertEquals(1, db.wedCheckDao().getCount())
    }
}
