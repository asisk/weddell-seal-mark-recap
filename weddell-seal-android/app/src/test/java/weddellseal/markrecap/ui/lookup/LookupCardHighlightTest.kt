package weddellseal.markrecap.ui.lookup

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.FieldHighlight
import weddellseal.markrecap.ui.home.HomeViewModel

/**
 * Parker 2025 season recap: lookup notes, tissue Need, and Dead must use the same
 * high-contrast highlight as selected tag/retag buttons (dark background, white text).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class LookupCardHighlightTest {

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
    fun tissueNeed_isHighlighted() {
        setLookupCardContent(WedCheckSeal(tissueSampled = "Need"))

        composeRule.onNodeWithText("Need").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(FieldHighlight.TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun tissueNotNeed_isNotHighlighted() {
        setLookupCardContent(WedCheckSeal(tissueSampled = "NA"))

        composeRule.onNodeWithText("Tissue Taken").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag(FieldHighlight.TEST_TAG).assertCountEquals(0)
    }

    @Test
    fun deadCondition_isHighlighted() {
        setLookupCardContent(WedCheckSeal(condition = SealCondition.DEAD))

        composeRule.onNodeWithText("Condition").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(SealCondition.DEAD.code).assertIsDisplayed()
        composeRule.onNodeWithTag(FieldHighlight.TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun liveCondition_isNotHighlighted() {
        setLookupCardContent(WedCheckSeal(condition = SealCondition.GOOD))

        composeRule.onNodeWithText("Condition").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag(FieldHighlight.TEST_TAG).assertCountEquals(0)
    }

    @Test
    fun lookupNotes_areHighlighted() {
        setLookupCardContent(WedCheckSeal(comment = "scar on left flipper"))

        composeRule.onNodeWithText("scar on left flipper").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(FieldHighlight.TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun blankLookupNotes_areNotHighlighted() {
        setLookupCardContent(WedCheckSeal(comment = "   "))

        composeRule.onNodeWithText("Comments").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag(FieldHighlight.TEST_TAG).assertCountEquals(0)
    }

    @Test
    fun needDeadAndNotes_eachGetAHighlight() {
        setLookupCardContent(
            WedCheckSeal(
                tissueSampled = "Need",
                condition = SealCondition.DEAD,
                comment = "old tags",
            ),
        )

        composeRule.onAllNodesWithTag(FieldHighlight.TEST_TAG).assertCountEquals(3)
    }

    private fun setLookupCardContent(seal: WedCheckSeal) {
        composeRule.setContent {
            MaterialTheme {
                LookupCard(seal = seal, homeViewModel = homeViewModel)
            }
        }
        composeRule.waitForIdle()
    }
}
