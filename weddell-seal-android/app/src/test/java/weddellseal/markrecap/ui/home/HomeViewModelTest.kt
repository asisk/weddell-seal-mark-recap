package weddellseal.markrecap.ui.home

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
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

    @Test
    fun cachedLocation_doesNotDetectColonyOrSave() {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        val cached = GeoLocation(
            coordinates = Coordinates(-77.5, 166.5),
            isLiveFix = false,
        )
        locationSource.emit(cached)

        assertEquals(cached, vm.currentLocation.value)
        assertNull(vm.autoDetectedColony.value)
        assertNull(vm.getColonyLocation())
        coVerify(exactly = 0) { sealRepo.findColony(any(), any()) }
    }

    @Test
    fun liveLocation_detectsColonyAndIsUsedForSave() {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        val colony = TestFixtures.sampleColony(location = "Hutton Cliffs")
        coEvery { sealRepo.findColony(-77.5, 166.5) } returns colony
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        val live = GeoLocation(
            coordinates = Coordinates(-77.5, 166.5),
            accuracyMeters = 12f,
            isLiveFix = true,
        )
        locationSource.emit(live)

        assertEquals("Hutton Cliffs", vm.autoDetectedColony.value?.location)
        assertEquals(live, vm.getColonyLocation())
        assertEquals(
            "Auto-detect must populate selectedColony for Save / CSV colony name",
            "Hutton Cliffs",
            vm.metadata.value.selectedColony?.location,
        )
    }

    @Test
    fun refreshGps_appliesFreshLiveFixAndDetectsColony() = runBlocking {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        val colony = TestFixtures.sampleColony(location = "Hutton Cliffs")
        coEvery { sealRepo.findColony(-77.51, 166.51) } returns colony
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.5, 166.5),
                isLiveFix = false,
            )
        )
        assertNull(vm.autoDetectedColony.value)

        val refreshed = GeoLocation(
            coordinates = Coordinates(-77.51, 166.51),
            accuracyMeters = 10f,
            isLiveFix = true,
        )
        locationSource.nextSingleUpdate = Result.success(refreshed)
        vm.refreshGps()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, locationSource.singleUpdateCount)
        assertEquals(refreshed, vm.currentLocation.value)
        assertEquals("Hutton Cliffs", vm.autoDetectedColony.value?.location)
        assertEquals("Hutton Cliffs", vm.metadata.value.selectedColony?.location)
        assertEquals(refreshed, vm.getColonyLocation())
        assertEquals(false, vm.uiState.value.isRefreshingGps)
    }

    @Test
    fun refreshGps_clearsCoordinatesAndAutoColonyWhileRequestInFlight() = runBlocking {
        val locationSource = FakeLocationSource()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        locationSource.singleUpdateGate = gate
        val sealRepo = mockSealColonyRepository()
        val colony = TestFixtures.sampleColony(location = "Baxter Meadows")
        coEvery { sealRepo.findColony(any(), any()) } returns colony
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        val previous = GeoLocation(
            coordinates = Coordinates(-77.5, 166.5),
            accuracyMeters = 10f,
            isLiveFix = true,
        )
        locationSource.emit(previous)
        assertEquals("Baxter Meadows", vm.autoDetectedColony.value?.location)
        assertEquals("Baxter Meadows", vm.metadata.value.selectedColony?.location)

        val refreshed = previous.copy(accuracyMeters = 8f)
        locationSource.nextSingleUpdate = Result.success(refreshed)
        vm.refreshGps()
        testDispatcher.scheduler.runCurrent()

        assertEquals(true, vm.uiState.value.isRefreshingGps)
        assertNull(vm.currentLocation.value)
        assertNull(vm.autoDetectedColony.value)
        assertNull(vm.metadata.value.selectedColony)

        gate.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(refreshed, vm.currentLocation.value)
        assertEquals("Baxter Meadows", vm.autoDetectedColony.value?.location)
        assertEquals("Baxter Meadows", vm.metadata.value.selectedColony?.location)
        assertEquals(false, vm.uiState.value.isRefreshingGps)
    }

    @Test
    fun refreshGps_keepsOverrideColonyWhileRefreshing() = runBlocking {
        val locationSource = FakeLocationSource()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        locationSource.singleUpdateGate = gate
        val sealRepo = mockSealColonyRepository()
        val handPicked = TestFixtures.sampleColony(location = "Turtle Rock")
        coEvery { sealRepo.findColony(any(), any()) } returns
            TestFixtures.sampleColony(location = "Baxter Meadows")
        coEvery { sealRepo.findColonyByName("Turtle Rock") } returns handPicked
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.5, 166.5),
                accuracyMeters = 10f,
                isLiveFix = true,
            )
        )
        vm.setOverrideColonyCheckbox(true)
        vm.updateSelectedColony("Turtle Rock")
        vm.metadata.first { it.selectedColony?.location == "Turtle Rock" }

        locationSource.nextSingleUpdate = Result.success(
            GeoLocation(
                coordinates = Coordinates(-77.6, 166.6),
                accuracyMeters = 8f,
                isLiveFix = true,
            )
        )
        vm.refreshGps()
        testDispatcher.scheduler.runCurrent()

        assertNull(vm.currentLocation.value)
        assertEquals("Turtle Rock", vm.metadata.value.selectedColony?.location)

        gate.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Turtle Rock", vm.metadata.value.selectedColony?.location)
    }

    @Test
    fun inaccurateLiveMiss_staysWaiting() {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        coEvery { sealRepo.findColony(any(), any()) } returns null
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        val live = GeoLocation(
            coordinates = Coordinates(-77.1, 166.1),
            accuracyMeters = 180f,
            isLiveFix = true,
        )
        locationSource.emit(live)

        assertNull(vm.autoDetectedColony.value)
        assertNull(vm.metadata.value.selectedColony)
        assertEquals(live, vm.getColonyLocation())
    }

    @Test
    fun overrideColony_keepsHandPickedSelectionWhenGpsMoves() = runBlocking {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        val gpsColony = TestFixtures.sampleColony(location = "Hutton Cliffs")
        val handPicked = TestFixtures.sampleColony(location = "Turtle Rock")
            .copy(adjLat = -77.9, adjLong = 166.9)
        coEvery { sealRepo.findColony(any(), any()) } returns gpsColony
        coEvery { sealRepo.findColonyByName("Turtle Rock") } returns handPicked
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.5, 166.5),
                accuracyMeters = 10f,
                isLiveFix = true,
            )
        )
        assertEquals("Hutton Cliffs", vm.metadata.value.selectedColony?.location)

        vm.setOverrideColonyCheckbox(true)
        vm.updateSelectedColony("Turtle Rock")
        vm.metadata.first { it.selectedColony?.location == "Turtle Rock" }

        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.6, 166.6),
                accuracyMeters = 10f,
                isLiveFix = true,
            )
        )

        assertEquals("Turtle Rock", vm.metadata.value.selectedColony?.location)
        assertEquals(-77.9, vm.getColonyLocation()!!.coordinates.latitude, 0.000_001)
    }

    @Test
    fun accurateLiveMiss_setsNotDetected() {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        coEvery { sealRepo.findColony(any(), any()) } returns null
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        val live = GeoLocation(
            coordinates = Coordinates(-77.1, 166.1),
            accuracyMeters = 15f,
            isLiveFix = true,
        )
        locationSource.emit(live)

        assertEquals(ColonyPopulation.NOT_DETECTED, vm.autoDetectedColony.value?.location)
        assertNull(vm.metadata.value.selectedColony)
        assertEquals(live, vm.getColonyLocation())
    }

    @Test
    fun inaccurateLiveHit_stillDetectsColony() {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        val colony = TestFixtures.sampleColony(location = "Turtle Rock")
        coEvery { sealRepo.findColony(any(), any()) } returns colony
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.2, 166.2),
                accuracyMeters = 200f,
                isLiveFix = true,
            )
        )

        assertEquals("Turtle Rock", vm.autoDetectedColony.value?.location)
    }

    @Test
    fun cachedAfterLive_doesNotReplaceLiveForSave() {
        val locationSource = FakeLocationSource()
        val sealRepo = mockSealColonyRepository()
        val colony = TestFixtures.sampleColony(location = "Hutton Cliffs")
        coEvery { sealRepo.findColony(any(), any()) } returns colony
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        val live = GeoLocation(
            coordinates = Coordinates(-77.5, 166.5),
            accuracyMeters = 10f,
            isLiveFix = true,
        )
        locationSource.emit(live)
        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.0, 166.0),
                isLiveFix = false,
            )
        )

        assertEquals(live, vm.currentLocation.value)
        assertEquals(live, vm.getColonyLocation())
        assertEquals("Hutton Cliffs", vm.autoDetectedColony.value?.location)
    }

    @Test
    fun colonyCatalogUpdate_reDetectsAgainstCurrentLiveFix() {
        val locationSource = FakeLocationSource()
        val coloniesFlow = MutableStateFlow<List<String>>(emptyList())
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns coloniesFlow
        coEvery { sealRepo.findColony(any(), any()) } returns null
        val vm = HomeViewModel(
            ApplicationProvider.getApplicationContext(),
            locationSource,
            sealRepo,
            mockObserversRepository(),
        )
        vm.onPermissionsResult(granted = true)

        val live = GeoLocation(
            coordinates = Coordinates(-77.5, 166.5),
            accuracyMeters = 12f,
            isLiveFix = true,
        )
        locationSource.emit(live)
        assertEquals(ColonyPopulation.NOT_DETECTED, vm.autoDetectedColony.value?.location)
        assertNull(vm.metadata.value.selectedColony)

        val baxter = TestFixtures.sampleColony(location = "Baxter Meadows")
        coEvery { sealRepo.findColony(-77.5, 166.5) } returns baxter
        coloniesFlow.value = listOf("Baxter Meadows")

        assertEquals("Baxter Meadows", vm.autoDetectedColony.value?.location)
        assertEquals("Baxter Meadows", vm.metadata.value.selectedColony?.location)
        assertEquals(live, vm.getColonyLocation())
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
