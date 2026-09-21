package weddellseal.markrecap.ui.home

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceGPSRowTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun cachedFix_showsLastKnownLabel() {
        val locationSource = FakeLocationSource()
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(emptyList())
        coEvery { sealRepo.findColony(any(), any()) } returns null
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())
        val vm = HomeViewModel(app, locationSource, sealRepo, observersRepo)
        vm.onPermissionsResult(true)
        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.5, 166.5),
                isLiveFix = false,
            )
        )

        composeRule.setContent {
            MaterialTheme {
                DeviceGPSRow(
                    viewModel = vm,
                    locationGranted = true,
                    onEnableLocation = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.LAST_KNOWN_LABEL).assertIsDisplayed()
        composeRule.onNodeWithText("-77.50000    166.50000").assertIsDisplayed()
    }

    @Test
    fun liveFix_doesNotShowLastKnownLabel() {
        val locationSource = FakeLocationSource()
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(emptyList())
        coEvery { sealRepo.findColony(any(), any()) } returns null
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())
        val vm = HomeViewModel(app, locationSource, sealRepo, observersRepo)
        vm.onPermissionsResult(true)
        locationSource.emit(
            GeoLocation(
                coordinates = Coordinates(-77.5, 166.5),
                isLiveFix = true,
            )
        )

        composeRule.setContent {
            MaterialTheme {
                DeviceGPSRow(
                    viewModel = vm,
                    locationGranted = true,
                    onEnableLocation = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.LAST_KNOWN_LABEL).assertDoesNotExist()
        composeRule.onNodeWithText("-77.50000    166.50000").assertIsDisplayed()
    }
}
