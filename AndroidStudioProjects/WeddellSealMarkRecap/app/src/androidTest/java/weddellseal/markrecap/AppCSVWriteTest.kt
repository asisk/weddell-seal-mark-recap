package weddellseal.markrecap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import weddellseal.markrecap.database.AppDatabase
import weddellseal.markrecap.database.ObservationDao
import weddellseal.markrecap.database.ObservationLogEntry
import java.io.File
import java.util.Date
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppCSVWriteTest {
    lateinit var database: AppDatabase
    lateinit var observationDao: ObservationDao
    lateinit var appContext: Context
    lateinit var observationSaver: ObservationSaverRepository
    lateinit var file: File

    @Before
    fun setUpDatabase(){
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        observationSaver = ObservationSaverRepository(appContext, appContext.contentResolver)
        file = observationSaver.generateObservationLogFile()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        observationDao = database.observationDao()
    }
    @After
    fun tearDown(){
        database.close()
        File(file.name).delete()
    }

    @Test
    fun writeCSV_returnsTrue() = runBlocking{
        val obs = ObservationLogEntry(
            1, Date().toString(),
            "45.8759667, long: -111.27695", "last known location empty"
        );

        val logs : List<ObservationLogEntry> = listOf(obs)

        val latch = CountDownLatch(1)
        val job = async(Dispatchers.IO) {
            assertTrue(observationSaver.writeObservationsToCSV(file, logs))
            latch.countDown()
        }
        latch.await()
        job.cancelAndJoin()
    }
}