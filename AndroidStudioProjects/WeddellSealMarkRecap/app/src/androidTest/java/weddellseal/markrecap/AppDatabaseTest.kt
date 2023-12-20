package weddellseal.markrecap

import android.util.Log
import androidx.lifecycle.asFlow
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import weddellseal.markrecap.data.AppDatabase
import weddellseal.markrecap.data.ObservationDao
import weddellseal.markrecap.data.ObservationLogEntry
import java.util.Date
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppDatabaseTest {
    lateinit var database: AppDatabase
    lateinit var observationDao: ObservationDao

    @Before
    fun setUpDatabase(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        observationDao = database.observationDao()
    }
    @After
    fun tearDown(){
        database.close()
    }

    @Test
    fun insertObservation_returnsTrue() = runBlocking{
        val obs = ObservationLogEntry(
            1, Date().toString(),
            "45.8759667, long: -111.27695", "last known location empty"
        );

        observationDao.insert(obs)

        val latch = CountDownLatch(1)
        val job = async(Dispatchers.IO) {
            var textObs = observationDao.loadAllObservations()?.asFlow()?.collect {
                Log.i("AppDatabaseTest", "Observation from the database: " + it.toString());
                val hasObs = it?.contains(obs)
                assertTrue(hasObs != null && hasObs == true)

                latch.countDown()
            }
        }
        latch.await()
        job.cancelAndJoin()
    }
}