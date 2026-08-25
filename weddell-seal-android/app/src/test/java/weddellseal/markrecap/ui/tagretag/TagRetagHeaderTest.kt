package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.home.HomeViewModel

/**
 * Parker 2025 season recap: technicians could not switch census prefill buttons after the
 * first tap. A second option must confirm discarding the current entry, then apply.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class TagRetagHeaderTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var tagRetagViewModel: TagRetagViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.coloniesList } returns flowOf(emptyList())
        val observersRepo = mockk<ObserversRepository>()
        every { observersRepo.observersList } returns flowOf(emptyList())
        homeViewModel = HomeViewModel(app, FakeLocationSource(), sealRepo, observersRepo)
        homeViewModel.updateIsCensusMode(true)

        val observationRepo = mockk<ObservationRepository>(relaxed = true)
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)
        tagRetagViewModel = TagRetagViewModel(
            app,
            observationRepo,
            wedCheckRepo,
            homeViewModel.metadata,
            MutableStateFlow(HomeViewModel.UiState(overrideColony = false)),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun censusPrefill_firstTapAppliesImmediately_secondTapConfirmsThenSwitches() {
        composeRule.setContent {
            MaterialTheme {
                TagRetagHeader(
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Mom and pup").performClick()
        composeRule.waitForIdle()

        assertEquals(SealSex.FEMALE, tagRetagViewModel.primarySeal.value.sex)
        assertEquals(SealRelatives.ONE, tagRetagViewModel.primarySeal.value.numRelatives)
        composeRule.onNodeWithText("Are you sure you want to start your entry over?")
            .assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Single Male").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Are you sure you want to start your entry over?")
            .assertIsDisplayed()
        assertEquals(SealSex.FEMALE, tagRetagViewModel.primarySeal.value.sex)

        composeRule.onNodeWithText("Start over", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(SealSex.MALE, tagRetagViewModel.primarySeal.value.sex)
        assertEquals(SealRelatives.ZERO, tagRetagViewModel.primarySeal.value.numRelatives)
        assertEquals(SealRelatives.UNKNOWN, tagRetagViewModel.pupOne.value.numRelatives)
        assertFalse(tagRetagViewModel.primarySeal.value.hasPupOne)
    }

    @Test
    fun censusPrefill_cancelLeavesCurrentEntry() {
        composeRule.setContent {
            MaterialTheme {
                TagRetagHeader(
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Mom and pup").performClick()
        composeRule.onNodeWithContentDescription("Single Female").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Cancel", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(SealSex.FEMALE, tagRetagViewModel.primarySeal.value.sex)
        assertEquals(SealRelatives.ONE, tagRetagViewModel.primarySeal.value.numRelatives)
        composeRule.onNodeWithText("Are you sure you want to start your entry over?")
            .assertDoesNotExist()
    }
}
