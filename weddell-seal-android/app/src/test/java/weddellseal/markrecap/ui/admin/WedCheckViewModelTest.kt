package weddellseal.markrecap.ui.admin

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WedCheckViewModelTest {

    @Test
    fun resetWedCheckUploadState_clearsErrorAndCount() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = mockk<WedCheckRepository>(relaxed = true)
        val vm = WedCheckViewModel(app, repo)
        vm.setWedCheckFileErrorStatus("bad")
        assertEquals(FileStatus.ERROR, vm.wedCheckUploadState.value.status)

        vm.resetWedCheckUploadState()

        assertEquals(FileStatus.IDLE, vm.wedCheckUploadState.value.status)
        assertEquals(0, vm.wedCheckUploadState.value.recordCount)
        assertEquals("", vm.wedCheckUploadState.value.message)
    }

    @Test
    fun updateWedCheckFileStatus_setsSuccess() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = mockk<WedCheckRepository>(relaxed = true)
        val vm = WedCheckViewModel(app, repo)
        vm.updateWedCheckFileStatus(42)
        assertEquals(FileStatus.SUCCESS, vm.wedCheckUploadState.value.status)
        assertEquals(42, vm.wedCheckUploadState.value.recordCount)
    }

    @Test
    fun setErrAcked_updatesUiState() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = mockk<WedCheckRepository>(relaxed = true)
        val vm = WedCheckViewModel(app, repo)
        vm.setErrAcked(true)
        assertTrue(vm.uiState.value.errAcked)
        vm.setErrAcked(false)
        assertFalse(vm.uiState.value.errAcked)
    }
}
