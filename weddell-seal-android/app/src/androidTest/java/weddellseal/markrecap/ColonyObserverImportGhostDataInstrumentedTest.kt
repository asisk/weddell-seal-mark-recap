package weddellseal.markrecap

import android.Manifest
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import weddellseal.markrecap.frameworks.google.fusedLocation.FusedLocationSource
import weddellseal.markrecap.frameworks.room.files.FilesRepository
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.home.SealColoniesViewModel
import weddellseal.markrecap.ui.tagretag.ObserversViewModel
import weddellseal.markrecap.viewmodelfactories.HomeViewModelFactory
import weddellseal.markrecap.viewmodelfactories.ObserversViewModelFactory
import weddellseal.markrecap.viewmodelfactories.SealColoniesViewModelFactory
import java.io.File

/**
 * Regression for Parker 2025 season recap: colony and observer dropdowns were accumulating
 * rows from every CSV ever imported (misspelled names stayed next to the correction) instead
 * of reflecting only the most recent upload. Import now clears prior rows before inserting
 * a successful parse.
 */
@RunWith(AndroidJUnit4::class)
class ColonyObserverImportGhostDataInstrumentedTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @get:Rule
    val testRules: TestRule = InstrumentedComposeTestSupport.ruleChain(
        grantPermissionRule,
        composeRule,
    )

    private val app: ObservationLogApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as ObservationLogApplication

    private lateinit var observersRepository: ObserversRepository
    private lateinit var sealColonyRepository: SealColonyRepository
    private lateinit var filesRepository: FilesRepository

    @Before
    fun setUp() {
        observersRepository = ObserversRepository(app.getObserversDao())
        sealColonyRepository = SealColonyRepository(app.getSealColoniesDao())
        filesRepository = FilesRepository(app.getFileUploadDao())
        clearColonyAndObserverTables()
    }

    @Test
    fun reimportObservers_onlyMostRecentCsvShouldBeUsed() {
        val observersViewModel = getObserversViewModel()

        importObservers(
            observersViewModel,
            filename = "observers_batch_one.csv",
            initials = listOf("GH1", "GH2", "GH3"),
        )
        assertObserverInitials(
            expected = setOf("GH1", "GH2", "GH3"),
            message = "First observers CSV should seed the database",
        )

        importObservers(
            observersViewModel,
            filename = "observers_batch_two.csv",
            initials = listOf("GH1", "GH4"),
        )

        assertObserverInitials(
            expected = setOf("GH1", "GH4"),
            message = "Re-import should replace prior observer rows; GH2 and GH3 are ghost data " +
                "from the first CSV if this assertion fails",
        )
    }

    @Test
    fun reimportColonies_onlyMostRecentCsvShouldBeUsed() {
        val coloniesViewModel = getSealColoniesViewModel()

        importColonies(
            coloniesViewModel,
            filename = "Colony_Locations_batch_one.csv",
            locations = listOf("GhostColonyA", "GhostColonyB", "GhostColonyC"),
        )
        assertColonyLocations(
            expected = setOf("GhostColonyA", "GhostColonyB", "GhostColonyC"),
            message = "First colony CSV should seed the database",
        )

        importColonies(
            coloniesViewModel,
            filename = "Colony_Locations_batch_two.csv",
            locations = listOf("GhostColonyA", "GhostColonyD"),
        )

        assertColonyLocations(
            expected = setOf("GhostColonyA", "GhostColonyD"),
            message = "Re-import should replace prior colony rows; GhostColonyB and GhostColonyC " +
                "are ghost data from the first CSV if this assertion fails",
        )
    }

    @Test
    fun reimportObservers_ghostDataSurvivesFreshViewModel_simulatingAppRestart() {
        val observersViewModel = getObserversViewModel()

        importObservers(
            observersViewModel,
            filename = "observers_restart_one.csv",
            initials = listOf("RS1", "RS2"),
        )
        importObservers(
            observersViewModel,
            filename = "observers_restart_two.csv",
            initials = listOf("RS3"),
        )

        // A new HomeViewModel uses the same persistent Room DB as a cold app start.
        val freshHomeViewModel = getHomeViewModel()
        val afterRestart = runBlocking {
            freshHomeViewModel.observersList.first { it.isNotEmpty() }.toSet()
        }

        assertEquals(
            "After re-import, only RS3 should remain even after a fresh ViewModel (simulating " +
                "app reopen). RS1 and RS2 are ghost data from the first CSV when this fails.",
            setOf("RS3"),
            afterRestart,
        )
    }

    private fun clearColonyAndObserverTables() {
        runBlocking {
            app.getObserversDao().clearObserversTable()
            app.getSealColoniesDao().clearColoniesTable()
        }
    }

    private fun importObservers(
        viewModel: ObserversViewModel,
        filename: String,
        initials: List<String>,
    ) {
        val uri = writeCsvToCache(filename, observersCsv(initials))
        composeRule.runOnUiThread {
            viewModel.loadObserversFile(uri, filename)
        }
        waitForImportSuccess(viewModel)
    }

    private fun importColonies(
        viewModel: SealColoniesViewModel,
        filename: String,
        locations: List<String>,
    ) {
        val uri = writeCsvToCache(filename, colonyCsv(locations))
        composeRule.runOnUiThread {
            viewModel.loadSealColoniesFile(uri, filename)
        }
        waitForImportSuccess(viewModel)
    }

    private fun waitForImportSuccess(viewModel: ObserversViewModel) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            var success = false
            composeRule.runOnUiThread {
                success = viewModel.fileState.value.status == FileStatus.SUCCESS
            }
            success
        }
        composeRule.waitForIdle()
    }

    private fun waitForImportSuccess(viewModel: SealColoniesViewModel) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            var success = false
            composeRule.runOnUiThread {
                success = viewModel.fileState.value.status == FileStatus.SUCCESS
            }
            success
        }
        composeRule.waitForIdle()
    }

    private fun assertObserverInitials(expected: Set<String>, message: String) {
        val actual = runBlocking { observersRepository.observersList.first().toSet() }
        assertEquals(message, expected, actual)
    }

    private fun assertColonyLocations(expected: Set<String>, message: String) {
        val actual = runBlocking { sealColonyRepository.coloniesList.first().toSet() }
        assertEquals(message, expected, actual)
    }

    private fun writeCsvToCache(filename: String, content: String): Uri {
        val file = File(app.cacheDir, filename)
        file.writeText(content)
        return Uri.fromFile(file)
    }

    private fun observersCsv(initials: List<String>): String =
        buildString {
            appendLine("Initials")
            initials.forEach { appendLine(it) }
        }

    private fun colonyCsv(locations: List<String>): String =
        buildString {
            appendLine("In/Out,Location,N_Limit,S_Limit,W_Limit,E_Limit,Adj_Lat,Adj_Long")
            locations.forEach { location ->
                appendLine("in,$location,-76.0,-78.0,165.0,167.0,-77.0,166.0")
            }
        }

    private fun getObserversViewModel(): ObserversViewModel {
        val factory = ObserversViewModelFactory(app, observersRepository, filesRepository)
        return ViewModelProvider(composeRule.activity, factory)[ObserversViewModel::class.java]
    }

    private fun getSealColoniesViewModel(): SealColoniesViewModel {
        val factory = SealColoniesViewModelFactory(app, sealColonyRepository, filesRepository)
        return ViewModelProvider(composeRule.activity, factory)[SealColoniesViewModel::class.java]
    }

    private fun getHomeViewModel(): HomeViewModel {
        val factory = HomeViewModelFactory(
            FusedLocationSource(composeRule.activity),
            sealColonyRepository,
            observersRepository,
        )
        return ViewModelProvider(composeRule.activity, factory)[HomeViewModel::class.java]
    }
}
