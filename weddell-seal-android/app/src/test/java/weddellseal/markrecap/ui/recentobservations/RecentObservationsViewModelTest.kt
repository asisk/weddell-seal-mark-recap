package weddellseal.markrecap.ui.recentobservations

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.frameworks.room.files.FilesRepository
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.ui.admin.ExportType
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import java.io.ByteArrayOutputStream

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
        val vm = viewModel()
        vm.setErrAcked(true)
        assertTrue(vm.uiState.value.errAcked)
        vm.setErrAcked(false)
        assertFalse(vm.uiState.value.errAcked)
    }

    @Test
    fun exportRecords_recordsSuccessfulCurrentExport() {
        val filename = "observations_H_20260825_155900.csv"
        val filesRepo = mockk<FilesRepository>(relaxed = true)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = viewModel(filesRepo, app = app)
        collectObservationLists(vm)
        val uri = writableCsvUri(app)

        mockkStatic(DocumentFile::class)
        try {
            val document = mockk<DocumentFile>()
            every { document.name } returns filename
            every { DocumentFile.fromSingleUri(any(), any()) } returns document

            vm.updateURI(uri)
            vm.exportRecords(app, ExportType.CURRENT)
        } finally {
            unmockkStatic(DocumentFile::class)
        }

        assertEquals(FileStatus.SUCCESS, vm.wedDataCurrentExportState.value.status)
        coVerify {
            filesRepo.insertFileUpload(match {
                it.fileAction == FileAction.EXPORT.name &&
                    it.fileType == FileType.WEDDATACURRENT &&
                    it.filename == filename &&
                    it.recordCount == 1 &&
                    it.status == FileStatus.SUCCESS &&
                    it.statusMessage == "exported"
            })
        }
    }

    @Test
    fun exportRecords_recordsSuccessfulFullExport() {
        val filename = "all_observations_H_20260825_155900.csv"
        val filesRepo = mockk<FilesRepository>(relaxed = true)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = viewModel(filesRepo, app = app)
        collectObservationLists(vm)
        val uri = writableCsvUri(app)

        mockkStatic(DocumentFile::class)
        try {
            val document = mockk<DocumentFile>()
            every { document.name } returns filename
            every { DocumentFile.fromSingleUri(any(), any()) } returns document

            vm.updateURI(uri)
            vm.exportRecords(app, ExportType.ALL)
        } finally {
            unmockkStatic(DocumentFile::class)
        }

        assertEquals(FileStatus.SUCCESS, vm.wedDataFullExportState.value.status)
        coVerify {
            filesRepo.insertFileUpload(match {
                it.fileAction == FileAction.EXPORT.name &&
                    it.fileType == FileType.WEDDATAFULL &&
                    it.filename == filename &&
                    it.recordCount == 1 &&
                    it.status == FileStatus.SUCCESS
            })
        }
    }

    @Test
    fun exportRecords_doesNotRecordAFailedExport() {
        val filesRepo = mockk<FilesRepository>(relaxed = true)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = viewModel(filesRepo, app = app)
        collectObservationLists(vm)

        vm.updateURI(Uri.EMPTY)
        vm.exportRecords(app, ExportType.CURRENT)

        assertEquals(FileStatus.ERROR, vm.wedDataCurrentExportState.value.status)
        coVerify(exactly = 0) { filesRepo.insertFileUpload(any()) }
    }

    @Test
    fun markObservationsAsDeleted_recordsArchive() {
        val filesRepo = mockk<FilesRepository>(relaxed = true)
        val vm = viewModel(filesRepo)
        collectObservationLists(vm)

        vm.markObservationsAsDeleted()

        coVerify {
            filesRepo.insertFileUpload(match {
                it.fileAction == FileAction.ARCHIVE.name &&
                    it.fileType == FileType.ARCHIVE &&
                    it.filename == "Current observations" &&
                    it.recordCount == 1 &&
                    it.status == FileStatus.SUCCESS &&
                    it.statusMessage == "archived"
            })
        }
    }

    @Test
    fun markObservationsAsDeleted_skipsHistoryWhenNothingToArchive() {
        val filesRepo = mockk<FilesRepository>(relaxed = true)
        val vm = viewModel(filesRepo, observations = emptyList())
        collectObservationLists(vm)

        vm.markObservationsAsDeleted()

        coVerify(exactly = 0) { filesRepo.insertFileUpload(any()) }
    }

    private fun viewModel(
        filesRepo: FilesRepository = mockk(relaxed = true),
        observations: List<ObservationRecord> = listOf(TestFixtures.minimalObservationRecord()),
        app: Application = ApplicationProvider.getApplicationContext(),
    ): RecentObservationsViewModel {
        val repo = mockk<ObservationRepository>(relaxed = true)
        every { repo.currentObservationsDescByID } returns flowOf(observations)
        every { repo.currentObservationsByID } returns flowOf(observations)
        every { repo.allObservationsDescByID } returns flowOf(observations)
        return RecentObservationsViewModel(app, repo, filesRepo)
    }

    private fun collectObservationLists(vm: RecentObservationsViewModel) {
        CoroutineScope(testDispatcher).launch { vm.currentObservations.collect {} }
        CoroutineScope(testDispatcher).launch { vm.allObservations.collect {} }
    }

    private fun writableCsvUri(app: Application): Uri {
        val authority = "weddell.export.test"
        val provider = CsvMimeProvider()
        provider.attachInfo(app, ProviderInfo().apply { this.authority = authority })
        ShadowContentResolver.registerProviderInternal(authority, provider)
        val uri = Uri.parse("content://$authority/observations.csv")
        shadowOf(app.contentResolver).registerOutputStream(uri, ByteArrayOutputStream())
        return uri
    }

    private class CsvMimeProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun getType(uri: Uri): String = "text/comma-separated-values"

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }
}
