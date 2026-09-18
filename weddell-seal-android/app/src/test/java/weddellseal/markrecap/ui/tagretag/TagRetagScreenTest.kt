package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import weddellseal.markrecap.Screens
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.ui.recentobservations.DisplayObservation
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

/**
 * After save, [TagRetagViewModel.resetModelState] clears the form but the page used to stay
 * scrolled to Save. The next seal should start at the top of the entry.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h700dp")
@OptIn(ExperimentalCoroutinesApi::class)
class TagRetagScreenTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var tagRetagViewModel: TagRetagViewModel
    private lateinit var recentObsViewModel: RecentObservationsViewModel
    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(emptyList())
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())
        homeViewModel = HomeViewModel(app, FakeLocationSource(), sealRepo, observersRepo)

        val observationRepo = mockk<ObservationRepository>(relaxed = true)
        every { observationRepo.currentObservationsDescByID } returns flowOf(emptyList())
        every { observationRepo.currentObservationsByID } returns flowOf(emptyList())
        every { observationRepo.allObservationsDescByID } returns flowOf(emptyList())

        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)
        tagRetagViewModel = TagRetagViewModel(
            app,
            observationRepo,
            wedCheckRepo,
            MutableStateFlow(TestFixtures.sampleMetadata()),
            MutableStateFlow(HomeViewModel.UiState(overrideColony = false)),
        )
        recentObsViewModel = RecentObservationsViewModel(app, observationRepo)
        navController = mockk(relaxed = true)
        val destination = mockk<NavDestination>()
        every { destination.route } returns Screens.TagRetag.route
        every { navController.currentDestination } returns destination
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resetAfterSave_scrollsPageBackToTop() {
        composeRule.setContent {
            MaterialTheme {
                TagRetagScreen(
                    navController = navController,
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                    recentObsViewModel = recentObsViewModel,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Save Seal", useUnmergedTree = true)
            .performScrollTo()
        composeRule.waitForIdle()

        assertTrue(
            "Save is below the fold, so scrolling to it must move the page",
            verticalScrollValue() > 0f,
        )

        composeRule.runOnIdle {
            tagRetagViewModel.resetModelState()
        }
        composeRule.waitForIdle()

        assertEquals(
            "After save/reset the next seal entry should start at the top of the page",
            0f,
            verticalScrollValue(),
            0.5f,
        )
    }

    @Test
    fun editDialog_staysSingleWhenScrollingAndDoesNotNavigateIfAlreadyOnTagRetag() {
        composeRule.setContent {
            MaterialTheme {
                TagRetagScreen(
                    navController = navController,
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                    recentObsViewModel = recentObsViewModel,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            tagRetagViewModel.onEditAttempt(
                DisplayObservation.Standalone(
                    TestFixtures.minimalObservationRecord().copy(id = 42, tagIDOne = "456B"),
                ),
            )
        }
        composeRule.waitForIdle()

        assertEquals(
            1,
            composeRule.onAllNodesWithText("Yes, edit entry", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )

        composeRule.onNodeWithContentDescription("Save Seal", useUnmergedTree = true)
            .performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Age").performScrollTo()
        composeRule.waitForIdle()

        assertEquals(
            "Scrolling the enter page must not open a second edit-confirm dialog",
            1,
            composeRule.onAllNodesWithText("Yes, edit entry", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )

        composeRule.onNodeWithText("Yes, edit entry", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertTrue(tagRetagViewModel.uiState.value.isEditMode)
        verify(exactly = 0) { navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>()) }
    }

    private fun verticalScrollValue(): Float {
        val node = composeRule.onNodeWithTag(TAG_RETAG_SCROLL_TEST_TAG).fetchSemanticsNode()
        return node.config[SemanticsProperties.VerticalScrollAxisRange].value()
    }
}
