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
import weddellseal.markrecap.frameworks.room.AppDatabase
import weddellseal.markrecap.frameworks.room.observations.ObservationDao
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
}