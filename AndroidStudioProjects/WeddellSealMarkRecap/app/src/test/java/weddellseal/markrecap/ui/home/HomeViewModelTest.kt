package weddellseal.markrecap.ui.home

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun coloniesList_ordersOtherFirst() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(listOf("Alpha", "Other", "Beta"))
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(listOf("JD"))

        val vm = HomeViewModel(app, FakeLocationSource(), sealRepo, observersRepo)

        val names = vm.coloniesList.first { it.size == 3 }
        assertEquals(listOf("Other", "Alpha", "Beta"), names)
    }

    @Test
    fun getColonyLocation_usesManualCoordinatesForOther() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(emptyList())
        coEvery { sealRepo.findColonyByName("Other") } returns TestFixtures.sampleColony(location = "Other")
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())

        val vm = HomeViewModel(app, FakeLocationSource(), sealRepo, observersRepo)
        vm.setOverrideColonyCheckbox(true)
        vm.updateSelectedColony("Other")
        vm.metadata.first { it.selectedColony?.location == "Other" }
        vm.updateOtherColonyLatitude("50")
        vm.updateOtherColonyLongitude("25")

        val loc = vm.getColonyLocation()
        // Decimal field is up to 5 digits after "-77." / "166." (see CoordinatesTextField)
        assertEquals(-77.0 + 50 / 100_000.0, loc!!.coordinates.latitude, 0.000_001)
        assertEquals(166.0 + 25 / 100_000.0, loc.coordinates.longitude, 0.000_001)
    }
}
