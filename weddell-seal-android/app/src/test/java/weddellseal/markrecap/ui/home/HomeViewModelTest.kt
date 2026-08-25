package weddellseal.markrecap.ui.home

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
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
import org.junit.Assert.assertTrue
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
        assertEquals(-77.00050, loc!!.coordinates.latitude, 0.000_001)
        assertEquals(166.00025, loc.coordinates.longitude, 0.000_001)
    }

    /**
     * Regression: home screen shows fixed label "-77." + typed digits, but
     * [HomeViewModel.getColonyLocation] composed latitude as
     * `latitudeDegrees + decimals/100000` (-77 + 0.x), which moves toward zero
     * and stores/exports ~-76.x instead of -77.x.
     */
    @Test
    fun getColonyLocation_otherColony_negativeLatitudeMatchesHomeScreenDisplay() = runBlocking {
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
        // Home UI: "-77." + "12345" and "166." + "67890"
        vm.updateOtherColonyLatitude("12345")
        vm.updateOtherColonyLongitude("67890")

        val loc = vm.getColonyLocation()!!
        assertEquals(-77.12345, loc.coordinates.latitude, 0.000_001)
        assertEquals(166.67890, loc.coordinates.longitude, 0.000_001)
        // Stakeholder report: backend showed -76 instead of the home-screen -77
        assertTrue(
            "Latitude must stay in the -77 range shown on the home screen, was ${loc.coordinates.latitude}",
            loc.coordinates.latitude <= -77.0,
        )
    }

    @Test
    fun onCleared_stopsLocationUpdates() {
        val locationSource = FakeLocationSource()
        val store = ViewModelStore()
        ViewModelProvider(store, homeViewModelFactory(locationSource))[HomeViewModel::class.java]
        assertEquals(0, locationSource.stopCount)

        store.clear()
        assertEquals(1, locationSource.stopCount)
    }

    @Test
    fun onPermissionsResult_startsAndStopsLocationUpdates() {
        val locationSource = FakeLocationSource()
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            mockSealColonyRepository(),
            mockObserversRepository(),
        )

        vm.onPermissionsResult(granted = true)
        assertEquals(1, locationSource.startCount)
        assertEquals(0, locationSource.stopCount)

        vm.onPermissionsResult(granted = false)
        assertEquals(1, locationSource.startCount)
        assertEquals(1, locationSource.stopCount)
    }

    private fun mockSealColonyRepository(): SealColonyRepository {
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(emptyList())
        return sealRepo
    }

    private fun mockObserversRepository(): ObserversRepository {
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())
        return observersRepo
    }

    private fun homeViewModelFactory(locationSource: FakeLocationSource): ViewModelProvider.Factory {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockSealColonyRepository()
        val observersRepo = mockObserversRepository()
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(app, locationSource, sealRepo, observersRepo) as T
            }
        }
    }
}
