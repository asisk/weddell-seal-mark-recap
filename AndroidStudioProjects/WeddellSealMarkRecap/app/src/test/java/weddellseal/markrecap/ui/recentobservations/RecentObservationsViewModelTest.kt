package weddellseal.markrecap.ui.recentobservations

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class RecentObservationsViewModelTest {

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
    fun setErrAcked_updatesUiState() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = mockk<ObservationRepository>()
        every { repo.currentObservationsDescByID } returns flowOf(emptyList())
        every { repo.currentObservationsByID } returns flowOf(emptyList())
        every { repo.allObservationsDescByID } returns flowOf(emptyList())

        val vm = RecentObservationsViewModel(app, repo)
        vm.setErrAcked(true)
        assertTrue(vm.uiState.value.errAcked)
        vm.setErrAcked(false)
        assertFalse(vm.uiState.value.errAcked)
    }
}
