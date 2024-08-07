package weddellseal.markrecap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import weddellseal.markrecap.data.AppDatabase
import weddellseal.markrecap.data.ObservationDao
import weddellseal.markrecap.data.ObservationRepository
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
    fun setUpDatabase(){
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
    fun tearDown(){
        database.close()
        File(file.name).delete()
    }

//    @Test
//    fun writeCSV_returnsTrue() = runBlocking{
//        val obs = ObservationLogEntry(
//            1, Date().toString(),
//            "45.8759667, long: -111.27695", "last known location empty"
//        );
//
//        val logs : List<ObservationLogEntry> = listOf(obs)
//
//        val latch = CountDownLatch(1)
//        val job = async(Dispatchers.IO) {
//            assertTrue(observationSaver.writeObservationsToCSV(file, logs))
//            latch.countDown()
//        }
//        latch.await()
//        job.cancelAndJoin()
//    }
}