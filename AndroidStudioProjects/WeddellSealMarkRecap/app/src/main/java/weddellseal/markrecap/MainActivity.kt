package weddellseal.markrecap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import weddellseal.markrecap.frameworks.google.fusedLocation.FusedLocationSource
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.files.FilesRepository
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.models.ObserversViewModelFactory
import weddellseal.markrecap.viewmodelfactories.TagRetagViewModelFactory
import weddellseal.markrecap.ui.tagretag.TagRetagModel
import weddellseal.markrecap.ui.admin.AdminViewModel
import weddellseal.markrecap.ui.tagretag.ObserversViewModel
import weddellseal.markrecap.viewmodelfactories.AdminViewModelFactory
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.viewmodelfactories.HomeViewModelFactory
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel
import weddellseal.markrecap.viewmodelfactories.RecentObservationsViewModelFactory
import weddellseal.markrecap.ui.home.SealColoniesViewModel
import weddellseal.markrecap.viewmodelfactories.SealColoniesViewModelFactory
import weddellseal.markrecap.ui.lookup.SealLookupViewModel
import weddellseal.markrecap.viewmodelfactories.SealLookupViewModelFactory
import weddellseal.markrecap.ui.admin.WedCheckViewModel
import weddellseal.markrecap.viewmodelfactories.WedCheckViewModelFactory
import weddellseal.markrecap.ui.tagretag.TagRetagScreen
import weddellseal.markrecap.ui.admin.AdminScreen
import weddellseal.markrecap.ui.home.HomeScreen
import weddellseal.markrecap.ui.permissions.LocationPermissionView
import weddellseal.markrecap.ui.recentobservations.ObservationViewer
import weddellseal.markrecap.ui.recentobservations.RecentObservationsScreen
import weddellseal.markrecap.ui.lookup.SealLookupScreen
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme


class MainActivity : ComponentActivity() {
    private lateinit var filesRepository: FilesRepository
    private lateinit var wedCheckRepository: WedCheckRepository
    private lateinit var observationRepository: ObservationRepository
    private lateinit var sealColonyRepository: SealColonyRepository
    private lateinit var observersRepository: ObserversRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access the ObservationLogApplication instance
        val observationLogApplication = application as ObservationLogApplication

        // Initialize the repositories
        val fileUploadDao = observationLogApplication.getFileUploadDao()
        filesRepository = FilesRepository(fileUploadDao)

        val wedCheckDao = observationLogApplication.getWedCheckDao()
        wedCheckRepository = WedCheckRepository(wedCheckDao, fileUploadDao)

        val observationDao = observationLogApplication.getObservationDao()
        observationRepository = ObservationRepository(observationDao)

        val sealColoniesDao = observationLogApplication.getSealColoniesDao()
        sealColonyRepository = SealColonyRepository(sealColoniesDao)

        val observersDao = observationLogApplication.getObserversDao()
        observersRepository = ObserversRepository(observersDao)

        // Initialize the view models
        val homeViewModelFactory =
            HomeViewModelFactory(
                FusedLocationSource(applicationContext),
                sealColonyRepository,
                observersRepository
            )
        val homeViewModel: HomeViewModel by viewModels { homeViewModelFactory }

        val tagRetagViewModelFactory =
            TagRetagViewModelFactory(
                application,
                observationRepository,
                homeViewModel.uiState)
        val tagRetagModel: TagRetagModel by viewModels { tagRetagViewModelFactory }

        val recentObservationsViewModelFactory = RecentObservationsViewModelFactory()
        val recentObservationsViewModel: RecentObservationsViewModel by viewModels { recentObservationsViewModelFactory }

        val adminViewModelFactory = AdminViewModelFactory(application, filesRepository)
        val adminViewModel: AdminViewModel by viewModels { adminViewModelFactory }

        val wedCheckViewModelFactory = WedCheckViewModelFactory(application, wedCheckRepository)
        val wedCheckViewModel: WedCheckViewModel by viewModels { wedCheckViewModelFactory }

        val sealLookupViewModelFactory = SealLookupViewModelFactory(application, wedCheckRepository)
        val sealLookupViewModel: SealLookupViewModel by viewModels { sealLookupViewModelFactory }

        val sealColoniesViewModelFactory = SealColoniesViewModelFactory(application, sealColonyRepository, filesRepository)
        val sealColoniesViewModel: SealColoniesViewModel by viewModels { sealColoniesViewModelFactory }

        val observersViewModelFactory = ObserversViewModelFactory(application, observersRepository, filesRepository)
        val observersViewModel: ObserversViewModel by viewModels { observersViewModelFactory }

        // Set up the UI
        enableEdgeToEdge()
        setContent {
            WeddellSealMarkRecapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startNavigation = Screens.HomeScreen.route
                    NavHost(navController = navController, startDestination = startNavigation) {
                        composable(Screens.LocationPermissionScreen.route) {
                            LocationPermissionView(onNextClick = {
                                navController.navigate(Screens.HomeScreen.route)
                            })
                        }
                        composable(Screens.HomeScreen.route) {
                            HomeScreen(
                                navController,
                                homeViewModel
                            )
                        }
                        composable(Screens.AddObservationLog.route) {
                            TagRetagScreen(
                                navController,
                                tagRetagModel,
                                homeViewModel,
                                sealLookupViewModel,
                                recentObservationsViewModel
                            )
                        }
                        composable(Screens.RecentObservations.route) {
                            RecentObservationsScreen(
                                navController,
                                recentObservationsViewModel,
                                tagRetagModel
                            )
                        }
                        composable(Screens.SealLookupScreen.route) {
                            SealLookupScreen(
                                navController,
                                sealLookupViewModel,
                                tagRetagModel
                            )
                        }
                        composable(Screens.ObservationViewer.route) {
                            ObservationViewer(
                                navController,
                                tagRetagModel
                            )
                        }
                        composable(Screens.AdminScreen.route) {
                            AdminScreen(
                                navController,
                                wedCheckViewModel,
                                sealColoniesViewModel,
                                observersViewModel,
                                adminViewModel,
                                recentObservationsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screens(val route: String) {
    object LocationPermissionScreen : Screens("location_permissions")
    object HomeScreen : Screens("home")
    object AdminScreen : Screens("admin")
    object AddObservationLog : Screens("add_log")
    object RecentObservations : Screens("view_db")
    object SealLookupScreen : Screens("seal_lookup")
    object ObservationViewer : Screens("observation_viewer")
}

