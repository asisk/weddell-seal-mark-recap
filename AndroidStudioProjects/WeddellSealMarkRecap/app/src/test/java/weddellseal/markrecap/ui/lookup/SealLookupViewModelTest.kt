package weddellseal.markrecap.ui.lookup

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class SealLookupViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun findSealbyTagID_setsLookupWhenRecordFound() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val record = WedCheckRecord(
            speno = 99,
            season = 2024,
            ageClass = "A",
            sex = "F",
            tagIdOne = "100A",
            tagIdTwo = "NA",
            comments = "",
            ageYears = 3,
            tissueSampled = "NA",
            pupinMassStudy = "NA",
            numPreviousPups = "NA",
            pupinTTStudy = "NA",
            momMassMeasurements = "NA",
            condition = "3",
            lastPhysio = "NA",
            population = "NA",
            fileUploadId = 1L,
            latitude = -77.0,
            longitude = 166.0
        )
        val repo = mockk<WedCheckRepository>()
        every { repo.findSealbyTagID("100A") } returns record

        val vm = SealLookupViewModel(app, repo)
        vm.findSealbyTagID("100A")

        vm.uiState.first { it.sealFound }
        assertEquals(99, vm.lookupSeal.value.speNo)
        assertEquals(SealAgeClass.ADULT, vm.lookupSeal.value.ageClass)
    }

    @Test
    fun findSealbyTagID_emptyQuery_doesNotSearch() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = mockk<WedCheckRepository>(relaxed = true)
        val vm = SealLookupViewModel(app, repo)
        vm.findSealbyTagID("")
        assertTrue(!vm.uiState.value.isSearching && !vm.uiState.value.sealFound && !vm.uiState.value.sealNotFound)
    }
}
