package weddellseal.markrecap.ui.home

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
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
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class ColonyRowTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var homeViewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(listOf("Hutton Cliffs", "Other"))
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())
        homeViewModel = HomeViewModel(app, FakeLocationSource(), sealRepo, observersRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun acquiring_showsWaitingCopyAndSecondaryOverride() {
        composeRule.setContent {
            MaterialTheme { ColonyRow(homeViewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.WAITING_FOR_GPS).assertIsDisplayed()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_HINT).assertIsDisplayed()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Override").assertDoesNotExist()
        composeRule.onNodeWithText("Selected Colony").assertDoesNotExist()
    }

    @Test
    fun override_cancelLeavesGpsMode() {
        composeRule.setContent {
            MaterialTheme { ColonyRow(homeViewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_CONFIRM).assertIsDisplayed()

        composeRule.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.WAITING_FOR_GPS).assertIsDisplayed()
        composeRule.onNodeWithText("Selected Colony").assertDoesNotExist()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_CONFIRM).assertDoesNotExist()
    }

    @Test
    fun override_confirmShowsDropdown_useGpsClears() {
        composeRule.setContent {
            MaterialTheme { ColonyRow(homeViewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_BUTTON).performClick()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_CONFIRM_ACTION, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Selected Colony").assertIsDisplayed()
        composeRule.onNodeWithText(ColonyGpsUi.USE_GPS_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_BUTTON).assertDoesNotExist()

        composeRule.onNodeWithText(ColonyGpsUi.USE_GPS_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.WAITING_FOR_GPS).assertIsDisplayed()
        composeRule.onNodeWithText("Selected Colony").assertDoesNotExist()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_CONFIRM).assertDoesNotExist()
    }

    @Test
    fun liveColonyName_isShownWithoutOverrideCheckbox() {
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Hutton Cliffs")
        )
        composeRule.setContent {
            MaterialTheme { ColonyRow(homeViewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Hutton Cliffs").assertIsDisplayed()
        composeRule.onNodeWithText(ColonyGpsUi.WAITING_FOR_GPS).assertDoesNotExist()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Override").assertDoesNotExist()
    }

    @Test
    fun notDetected_keepsHintAndOverride() {
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = ColonyPopulation.NOT_DETECTED)
        )
        composeRule.setContent {
            MaterialTheme { ColonyRow(homeViewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyPopulation.NOT_DETECTED).assertIsDisplayed()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_HINT).assertIsDisplayed()
        composeRule.onNodeWithText(ColonyGpsUi.OVERRIDE_BUTTON).assertIsDisplayed()
    }
}
