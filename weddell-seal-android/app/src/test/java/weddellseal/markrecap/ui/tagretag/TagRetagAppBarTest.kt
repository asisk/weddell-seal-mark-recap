package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.home.ColonyGpsUi
import weddellseal.markrecap.ui.home.HomeViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class TagRetagAppBarTest {

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
        every { sealRepo.coloniesList } returns flowOf(emptyList())
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())
        homeViewModel = HomeViewModel(app, FakeLocationSource(), sealRepo, observersRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun header_followsLiveAutoDetectedColonyAfterMove() {
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Hutton Cliffs")
        )
        composeRule.setContent {
            MaterialTheme {
                TagRetagAppBar(
                    onNavigationIconClick = {},
                    homeViewModel = homeViewModel,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hutton Cliffs").assertIsDisplayed()

        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Turtle Rock")
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Turtle Rock").assertIsDisplayed()
        composeRule.onNodeWithText("Hutton Cliffs").assertDoesNotExist()
    }

    @Test
    fun header_showsWaitingCopyWhenGpsHasNoColony() {
        composeRule.setContent {
            MaterialTheme {
                TagRetagAppBar(
                    onNavigationIconClick = {},
                    homeViewModel = homeViewModel,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(ColonyGpsUi.WAITING_FOR_GPS_SHORT).assertIsDisplayed()
        composeRule.onNodeWithText("Colony missing").assertDoesNotExist()
    }

    @Test
    fun header_showsOverrideSelection() {
        homeViewModel.setOverrideColonyCheckbox(true)
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Hutton Cliffs")
        )
        composeRule.setContent {
            MaterialTheme {
                TagRetagAppBar(
                    onNavigationIconClick = {},
                    homeViewModel = homeViewModel,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Colony missing").assertIsDisplayed()
        composeRule.onNodeWithText("Hutton Cliffs").assertDoesNotExist()
    }
}
