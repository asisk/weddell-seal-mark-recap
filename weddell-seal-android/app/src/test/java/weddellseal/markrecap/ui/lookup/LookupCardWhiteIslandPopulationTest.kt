package weddellseal.markrecap.ui.lookup

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.home.HomeViewModel

/**
 * Regression for Parker 2025 season recap: looking up a White Island seal while the device
 * is also at White Island caused the Population row to disappear with no error. Lookup still
 * returned population = "White Island"; [LookupCard] only rendered Population inside the
 * "outside White Island" warning branch and had no else when the auto-detected colony matched.
 * Highlight must compare GPS colony *population* (Erebus Bay vs White Island), not location name
 * and not the manual colony override. Tag/retag is not part of this recap behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class LookupCardWhiteIslandPopulationTest {

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
    fun whiteIslandSeal_lookedUpAtWhiteIsland_showsPopulation() {
        // Parker 2025 season recap: last-seen population must still show when the device is
        // also at White Island (no highlight, no missing row).
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "White Island"),
        )

        setLookupCardContent(
            WedCheckSeal(
                speNo = 42,
                tagIdOne = "100A",
                population = "White Island",
            ),
        )

        // Seal loaded successfully — stakeholder saw no error, just missing Population.
        composeRule.onNodeWithText("SPENO").assertIsDisplayed()
        composeRule.onNodeWithText("42").assertIsDisplayed()

        assertPopulationRowDisplayed()
        composeRule.onNodeWithText("White Island").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun whiteIslandSeal_lookedUpOutsideWhiteIsland_showsPopulationAndWarning() {
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Erebus Bay"),
        )

        setLookupCardContent(
            WedCheckSeal(
                speNo = 42,
                tagIdOne = "100A",
                population = "White Island",
            ),
        )

        assertPopulationRowDisplayed()
        composeRule.onNodeWithText(
            "This seal was last seen at White Island. Current colony detected by device is Erebus Bay.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun whiteIslandSeal_whenColonyUnknown_showsPopulationWithoutWarning() {
        // Parker 2025 season recap: GPS not yet detecting a colony must not hide Population
        // or show a "no population detected" warning.
        setLookupCardContent(
            WedCheckSeal(
                speNo = 42,
                tagIdOne = "100A",
                population = "White Island",
            ),
        )

        assertPopulationRowDisplayed()
        composeRule.onNodeWithText("White Island").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Please take a photo of the tags and seal!").assertDoesNotExist()
    }

    @Test
    fun erebusBaySeal_lookedUpAtWhiteIsland_highlightsPopulation() {
        // Parker 2025 season recap: different population than the current colony should highlight.
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "White Island"),
        )

        setLookupCardContent(
            WedCheckSeal(
                speNo = 7,
                tagIdOne = "200B",
                population = "Erebus Bay",
            ),
        )

        assertPopulationRowDisplayed()
        composeRule.onNodeWithText(
            "This seal was last seen at Erebus Bay. Current colony detected by device is White Island.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Please take a photo of the tags and seal!").assertDoesNotExist()
    }

    @Test
    fun erebusBaySeal_atTurtleRock_doesNotHighlight() {
        // Parker 2025 season recap: same population (Erebus Bay colony vs Erebus Bay seal)
        // should not highlight. Turtle Rock is a colony, not the population name.
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Turtle Rock"),
        )

        setLookupCardContent(
            WedCheckSeal(
                speNo = 7,
                tagIdOne = "200B",
                population = "Erebus Bay",
            ),
        )

        assertPopulationRowDisplayed()
        composeRule.onNodeWithText("Erebus Bay").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "This seal was last seen at Erebus Bay. Current colony detected by device is Turtle Rock.",
        ).assertDoesNotExist()
    }

    private fun assertPopulationRowDisplayed() {
        composeRule.onNodeWithText("Population")
            .performScrollTo()
            .assertIsDisplayed()
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
