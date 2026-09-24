package weddellseal.markrecap

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.map.MapScreen
import weddellseal.markrecap.ui.map.MapScreenUi
import weddellseal.markrecap.ui.map.WarmMapViewModel
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme

/**
 * Light Map smoke: hosts [MapScreen] and asserts pack-missing copy when tiles are absent.
 * Does not assert MapLibre native rendering.
 */
@RunWith(AndroidJUnit4::class)
class MapScreenInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app: ObservationLogApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as ObservationLogApplication

    @Test
    fun mapScreenShowsPackMissingWithoutCrashing() {
        val sealRepo = mockk<SealColonyRepository>()
        every { sealRepo.colonies } returns flowOf(emptyList())
        val observersRepo = ObserversRepository(app.getObserversDao())
        val homeViewModel = HomeViewModel(
            app,
            FakeLocationSource(),
            sealRepo,
            observersRepo,
        )

        composeRule.setContent {
            WeddellSealMarkRecapTheme {
                MapScreen(
                    navController = rememberNavController(),
                    homeViewModel = homeViewModel,
                    sealColonyRepository = sealRepo,
                    warmMap = WarmMapViewModel(),
                    mapContent = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(MapScreenUi.PACK_MISSING, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(MapScreenUi.EMPTY_COLONIES, substring = true).assertIsDisplayed()
    }
}
