package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.FieldHighlight
import weddellseal.markrecap.ui.home.HomeViewModel

/**
 * After save, [TagRetagViewModel.resetModelState] drops pup tabs in the same frame the
 * user may still have a pup tab selected. Indexing the new one-item tab list at the old
 * pup index crashed with IndexOutOfBoundsException.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class TabbedCardsTest {

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

        val observationRepo = mockk<ObservationRepository>(relaxed = true)
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)
        tagRetagViewModel = TagRetagViewModel(
            app,
            observationRepo,
            wedCheckRepo,
            MutableStateFlow(TestFixtures.sampleMetadata()),
            MutableStateFlow(HomeViewModel.UiState(overrideColony = false)),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savingWhilePupTabSelected_doesNotCrashAndReturnsToPrimaryTab() {
        var primarySeal by mutableStateOf(primaryWithOnePup())
        var pupOneSeal by mutableStateOf(completePupOne())
        var pupTwoSeal by mutableStateOf(emptyPupTwo())

        composeRule.setContent {
            MaterialTheme {
                TabbedCards(
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                    primarySeal = primarySeal,
                    pupOneSeal = pupOneSeal,
                    pupTwoSeal = pupTwoSeal,
                )
            }
        }

        composeRule.onNodeWithText(SealType.PUPONE.label).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Same seal reset as resetModelState() after a successful save.
        composeRule.runOnIdle {
            primarySeal = Seal(sealType = SealType.PRIMARY)
            pupOneSeal = Seal(sealType = SealType.PUPONE, ageClass = SealAgeClass.PUP)
            pupTwoSeal = Seal(sealType = SealType.PUPTWO, ageClass = SealAgeClass.PUP)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(SealType.PRIMARY.label).assertIsDisplayed()
        composeRule.onNodeWithText(SealType.PUPONE.label).assertDoesNotExist()
    }

    @Test
    fun savingWhilePupTwoTabSelected_doesNotCrashAndReturnsToPrimaryTab() {
        var primarySeal by mutableStateOf(primaryWithTwoPups())
        var pupOneSeal by mutableStateOf(completePupOne())
        var pupTwoSeal by mutableStateOf(completePupTwo())

        composeRule.setContent {
            MaterialTheme {
                TabbedCards(
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                    primarySeal = primarySeal,
                    pupOneSeal = pupOneSeal,
                    pupTwoSeal = pupTwoSeal,
                )
            }
        }

        composeRule.onNodeWithText(SealType.PUPTWO.label).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            primarySeal = Seal(sealType = SealType.PRIMARY)
            pupOneSeal = Seal(sealType = SealType.PUPONE, ageClass = SealAgeClass.PUP)
            pupTwoSeal = Seal(sealType = SealType.PUPTWO, ageClass = SealAgeClass.PUP)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(SealType.PRIMARY.label).assertIsDisplayed()
        composeRule.onNodeWithText(SealType.PUPTWO.label).assertDoesNotExist()
    }

    @Test
    fun wedCheckComment_isHighlighted() {
        composeRule.setContent {
            MaterialTheme {
                TabbedCards(
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                    primarySeal = primaryWithWedCheckComment("scar on left flipper"),
                    pupOneSeal = emptyPupTwo().copy(sealType = SealType.PUPONE),
                    pupTwoSeal = emptyPupTwo(),
                )
            }
        }

        composeRule.onNodeWithText("scar on left flipper").assertIsDisplayed()
        composeRule.onNodeWithTag(FieldHighlight.TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun blankWedCheckComment_isNotHighlighted() {
        composeRule.setContent {
            MaterialTheme {
                TabbedCards(
                    viewModel = tagRetagViewModel,
                    homeViewModel = homeViewModel,
                    primarySeal = primaryWithWedCheckComment("   "),
                    pupOneSeal = emptyPupTwo().copy(sealType = SealType.PUPONE),
                    pupTwoSeal = emptyPupTwo(),
                )
            }
        }

        composeRule.onAllNodesWithTag(FieldHighlight.TEST_TAG).assertCountEquals(0)
    }

    private fun primaryWithWedCheckComment(comment: String) =
        TestFixtures.completePrimaryMarkedSeal().copy(
            notebookDataString = "Adult 123A",
            wedCheckMatch = WedCheckSeal(speNo = 42, comment = comment),
        )

    private fun primaryWithOnePup() = TestFixtures.completePrimaryMarkedSeal().copy(
        numRelatives = SealRelatives.ONE,
        notebookDataString = "Adult 123A",
    )

    private fun primaryWithTwoPups() = TestFixtures.completePrimaryMarkedSeal().copy(
        numRelatives = SealRelatives.TWO,
        notebookDataString = "Adult 123A",
    )

    private fun completePupOne() = Seal(
        sealType = SealType.PUPONE,
        ageClass = SealAgeClass.PUP,
        sex = SealSex.FEMALE,
        tagEventType = TagEventType.NEW,
        tagNumber = "999",
        tagAlpha = "P",
        numTags = "1",
        condition = SealCondition.GOOD,
        notebookDataString = "Pup 999P",
    )

    private fun completePupTwo() = Seal(
        sealType = SealType.PUPTWO,
        ageClass = SealAgeClass.PUP,
        sex = SealSex.MALE,
        tagEventType = TagEventType.NEW,
        tagNumber = "888",
        tagAlpha = "Q",
        numTags = "1",
        condition = SealCondition.GOOD,
        notebookDataString = "Pup 888Q",
    )

    private fun emptyPupTwo() = Seal(
        sealType = SealType.PUPTWO,
        ageClass = SealAgeClass.PUP,
    )
}
