package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.home.HomeViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class TagRetagViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun writeObservationRecord_persistsNewSeal() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.NEW)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "B")
        vm.updateNumTags(SealType.PRIMARY, "1")

        assertTrue(vm.uiState.value.isSaveEnabled)

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertEquals("456B", written[0].tagIDOne)
        assertEquals(TagEventType.NEW.alpha, written[0].tagEvent)
    }
}
