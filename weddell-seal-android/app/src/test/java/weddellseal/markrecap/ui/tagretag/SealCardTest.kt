package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
import org.junit.Assert.assertTrue
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
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.utils.getCurrentYear

/**
 * Tag/retag keeps the pre-existing White Island photo prompt. Population highlight from the
 * Parker 2025 recap is lookup-only. Also gates the sex-validation banner on isSaveAttempted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h1920dp")
@OptIn(ExperimentalCoroutinesApi::class)
class SealCardTest {

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
    fun whiteIslandSeal_outsideWhiteIsland_showsPhotoPrompt() {
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Erebus Bay"),
        )
        setSealCardContent(sealAtPopulation("White Island"))

        composeRule.onNodeWithText(
            "This seal was last seen at White Island. Current colony detected by device is Erebus Bay.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Please take a photo of the tags and seal!")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun erebusBaySeal_atTurtleRock_doesNotShowPhotoPrompt() {
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "Turtle Rock"),
        )
        setSealCardContent(sealAtPopulation("Erebus Bay"))

        composeRule.onNodeWithText(
            "This seal was last seen at Erebus Bay. Current colony detected by device is Turtle Rock.",
        ).assertDoesNotExist()
        composeRule.onNodeWithText("Please take a photo of the tags and seal!").assertDoesNotExist()
    }

    @Test
    fun erebusBaySeal_atWhiteIsland_doesNotShowWhiteIslandPhotoPrompt() {
        // Parker 2025 recap highlight is lookup-only; tag/retag does not generalize that rule.
        homeViewModel.setAutoDetectedColony(
            TestFixtures.sampleColony(location = "White Island"),
        )
        setSealCardContent(sealAtPopulation("Erebus Bay"))

        composeRule.onNodeWithText(
            "This seal was last seen at Erebus Bay. Current colony detected by device is White Island.",
        ).assertDoesNotExist()
        composeRule.onNodeWithText("Please take a photo of the tags and seal!").assertDoesNotExist()
    }

    @Test
    fun validationBanner_isHiddenWhenSaveHasNotBeenAttempted() {
        // Parker 2025 season recap: banner is gated on isSaveAttempted so a stale Speno
        // mismatch cannot flash before the save-path WedCheck refresh finishes.
        val mismatched = sexMismatchSeal()
        assertTrue(mismatched.validationErrors.any { it.contains("Sex doesn't match") })

        setSealCardContent(mismatched)

        composeRule.onNodeWithText("Sex doesn't match", substring = true).assertDoesNotExist()
    }

    @Test
    fun validationBanner_isShownAfterSaveIsAttemptedWhenSexMismatches() {
        val mismatched = sexMismatchSeal()
        tagRetagViewModel.setIsSaving()
        setSealCardContent(mismatched)

        composeRule.onNodeWithText("Sex doesn't match", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun ageSexRelatives_followFieldResetCounterAfterCensusPrefillSwitch() {
        tagRetagViewModel.prefillMomAndPup()
        composeRule.setContent {
            val seal by tagRetagViewModel.primarySeal.collectAsState()
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SealCard(
                        viewModel = tagRetagViewModel,
                        homeViewModel = homeViewModel,
                        seal = seal,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasText("Female") and hasContentDescription("Selected"))
            .assertIsDisplayed()

        composeRule.runOnIdle {
            tagRetagViewModel.confirmCensusPrefill(CensusPrefill.SINGLE_MALE)
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasText("Female") and hasContentDescription("Selected"))
            .assertDoesNotExist()
        composeRule.onNode(hasText("Male") and hasContentDescription("Selected"))
            .assertIsDisplayed()
    }

    private fun setSealCardContent(seal: Seal) {
        composeRule.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SealCard(
                        viewModel = tagRetagViewModel,
                        homeViewModel = homeViewModel,
                        seal = seal,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun sealAtPopulation(population: String) =
        TestFixtures.completePrimaryMarkedSeal().copy(colony = population)

    private fun sexMismatchSeal() = Seal(
        sealType = SealType.PRIMARY,
        ageClass = SealAgeClass.ADULT,
        sex = SealSex.MALE,
        colony = "Erebus Bay",
        numRelatives = SealRelatives.ZERO,
        tagEventType = TagEventType.MARKED,
        tagNumber = "1234",
        tagAlpha = "A",
        numTags = "1",
        condition = SealCondition.GOOD,
        wedCheckMatch = WedCheckSeal(
            speNo = 10,
            tagIdOne = "1234A",
            sex = SealSex.FEMALE,
            ageClass = SealAgeClass.ADULT,
            numTags = "1",
            condition = SealCondition.GOOD,
            lastSeenSeason = getCurrentYear(),
        ),
    )
}
