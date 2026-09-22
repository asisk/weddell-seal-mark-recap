package weddellseal.markrecap

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.location.data.toDisplayString
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.testsupport.FakeLocationSource
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import weddellseal.markrecap.ui.home.ColonyGpsUi
import weddellseal.markrecap.ui.home.ColonyRow
import weddellseal.markrecap.ui.home.DeviceGPSRow
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.TagRetagAppBar

/**
 * On-device proof that a cached last-known fix is display-only on Device GPS.
 *
 * The coordinates sit inside a real colony bounding box. Colony auto-detect and
 * [HomeViewModel.getColonyLocation] must still wait for a live fix, so a technician cannot
 * treat last-known as the current colony or the location that is saved.
 */
@RunWith(AndroidJUnit4::class)
class LastKnownLocationInstrumentedTest {

    companion object {
        private const val COLONY_NAME = "LastKnownBoxColony"
        private val CACHED_INSIDE_BOX = GeoLocation(
            coordinates = Coordinates(latitude = -77.5, longitude = 166.5),
            isLiveFix = false,
        )
        private val LIVE_INSIDE_BOX = GeoLocation(
            coordinates = Coordinates(latitude = -77.5, longitude = 166.5),
            accuracyMeters = 12f,
            isLiveFix = true,
        )
        private val COORD_TEXT = Coordinates(
            latitude = -77.5,
            longitude = 166.5,
        ).toDisplayString()
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val app: ObservationLogApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as ObservationLogApplication

    @Before
    fun seedColonyInsideCachedCoordinates() {
        runBlocking {
            app.getSealColoniesDao().clearColoniesTable()
            val fileUploadId = app.getFileUploadDao().insertFileUpload(
                FileUploadEntity(
                    fileType = FileType.COLONIES,
                    fileAction = FileAction.UPLOAD.name,
                    filename = "instrumented-last-known-colony.csv",
                    status = FileStatus.IDLE,
                    statusMessage = null,
                    recordCount = 1,
                ),
            )
            app.getSealColoniesDao().insertColonyRecords(
                fileUploadId,
                listOf(
                    SealColony(
                        inOut = "in",
                        location = COLONY_NAME,
                        nLimit = -77.4,
                        sLimit = -77.8,
                        wLimit = 166.3,
                        eLimit = 166.7,
                        adjLat = -77.5,
                        adjLong = 166.5,
                        fileUploadId = fileUploadId,
                    ),
                ),
            )
        }
    }

    @Test
    fun lastKnownCoordinates_showOnDeviceGpsOnly_untilLiveFix() {
        val locationSource = FakeLocationSource()
        val viewModel = HomeViewModel(
            app,
            locationSource,
            SealColonyRepository(app.getSealColoniesDao()),
            ObserversRepository(app.getObserversDao()),
        )

        composeRule.setContent {
            MaterialTheme {
                Column {
                    DeviceGPSRow(
                        viewModel = viewModel,
                        locationGranted = true,
                        onEnableLocation = {},
                    )
                    ColonyRow(viewModel)
                    TagRetagAppBar(
                        onNavigationIconClick = {},
                        homeViewModel = viewModel,
                    )
                }
            }
        }

        composeRule.runOnUiThread {
            viewModel.onPermissionsResult(granted = true)
            locationSource.emit(CACHED_INSIDE_BOX)
        }
        composeRule.waitForIdle()

        waitUntilDisplayed(ColonyGpsUi.LAST_KNOWN_LABEL)
        waitUntilDisplayed(COORD_TEXT)
        waitUntilDisplayed(ColonyGpsUi.WAITING_FOR_GPS)
        waitUntilDisplayed(ColonyGpsUi.WAITING_FOR_GPS_SHORT)
        composeRule.onNodeWithText(COLONY_NAME).assertDoesNotExist()
        composeRule.runOnUiThread {
            assertNull(viewModel.autoDetectedColony.value)
            assertNull(viewModel.getColonyLocation())
        }

        composeRule.runOnUiThread {
            locationSource.emit(LIVE_INSIDE_BOX)
        }
        composeRule.waitForIdle()

        // Home colony row and Tag/Retag header both show the live colony.
        waitUntilNodeCount(COLONY_NAME, 2)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            !nodeExists(ColonyGpsUi.LAST_KNOWN_LABEL)
        }
        waitUntilNodeCount(COORD_TEXT, 2)
        composeRule.onNodeWithText(ColonyGpsUi.WAITING_FOR_GPS).assertDoesNotExist()
        composeRule.runOnUiThread {
            assertEquals(COLONY_NAME, viewModel.autoDetectedColony.value?.location)
            assertEquals(LIVE_INSIDE_BOX, viewModel.getColonyLocation())
        }
    }

    private fun waitUntilDisplayed(text: String) {
        waitUntilNodeCount(text, 1)
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun waitUntilNodeCount(text: String, count: Int) {
        composeRule.waitUntil(timeoutMillis = 10_000) { nodeCount(text) == count }
        assertEquals(count, nodeCount(text))
    }

    private fun nodeExists(text: String): Boolean = nodeCount(text) > 0

    private fun nodeCount(text: String): Int =
        composeRule.onAllNodes(hasText(text, substring = false), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size
}
