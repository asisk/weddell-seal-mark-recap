package weddellseal.markrecap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.room.AppDatabase
import weddellseal.markrecap.frameworks.room.observations.ObservationDao
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import java.io.File

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppCSVWriteTest {
    lateinit var database: AppDatabase
    lateinit var observationDao: ObservationDao
    lateinit var appContext: Context
    lateinit var observationSaver: ObservationRepository
    lateinit var obsFolder: File
    lateinit var file: File

    @Before
    fun setUpDatabase() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        observationDao = database.observationDao()
        observationSaver = ObservationRepository(observationDao)
        obsFolder = File(appContext.filesDir, "observations").also { it.mkdir() }
        file = File(obsFolder, "${System.currentTimeMillis()}.csv")
    }

    @After
    fun tearDown() {
        database.close()
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun writeObservation_incrementsDaoCount() = runBlocking {
        observationSaver.writeObservation(csvWriteTestObservation())
        assertEquals(1, observationDao.getCount())
    }

    @Test
    fun writeDataToStream_writesCsvRows() {
        val records = listOf(csvWriteTestObservation())
        file.outputStream().use { out ->
            observationSaver.writeDataToStream(out, records)
        }
        val text = file.readText()
        assertTrue(text.contains("dev"))
        assertTrue(text.contains("123A"))
    }

    private fun csvWriteTestObservation() = ObservationRecord(
        id = 0,
        deviceID = "dev",
        season = "2025",
        speno = "0",
        date = "2025-01-01",
        time = "12:00:00",
        censusID = "0",
        latitude = "-77.0",
        longitude = "166.0",
        ageClass = SealAgeClass.ADULT.alpha,
        sex = SealSex.FEMALE.alpha,
        numRelatives = "0",
        oldTagIDOne = "",
        oldTagIDTwo = "",
        tagIDOne = "123A",
        tagOneIndicator = "",
        tagIDTwo = "NoTag",
        tagTwoIndicator = "",
        relativeTagIDOne = "",
        relativeTagIDTwo = "",
        sealCondition = SealCondition.GOOD.code,
        observerInitials = "JD",
        flaggedEntry = "",
        tagEvent = TagEventType.MARKED.alpha,
        weight = "",
        tissueSampled = "",
        comments = "",
        retagReason = "",
        colony = "ColonyX",
    )
}
