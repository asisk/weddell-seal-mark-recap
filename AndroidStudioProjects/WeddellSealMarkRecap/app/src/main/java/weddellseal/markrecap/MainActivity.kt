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
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.SupportingDataRepository
import weddellseal.markrecap.data.WedCheckRepository
import weddellseal.markrecap.locationFramework.FusedLocationSource
import weddellseal.markrecap.models.AddLogViewModelFactory
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.HomeViewModel
import weddellseal.markrecap.models.HomeViewModelFactory
import weddellseal.markrecap.models.RecentObservationsViewModel
import weddellseal.markrecap.models.RecentObservationsViewModelFactory
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.models.WedCheckViewModelFactory
import weddellseal.markrecap.ui.screens.AddObservationLogScreen
import weddellseal.markrecap.ui.screens.AdminScreen
import weddellseal.markrecap.ui.screens.HomeScreen
import weddellseal.markrecap.ui.screens.LocationPermissionView
import weddellseal.markrecap.ui.screens.ObservationViewer
import weddellseal.markrecap.ui.screens.RecentObservationsScreen
import weddellseal.markrecap.ui.screens.SealLookupScreen
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme


class MainActivity : ComponentActivity() {
    private lateinit var wedCheckRepository: WedCheckRepository
    private lateinit var observationRepository: ObservationRepository
    private lateinit var supportingDataRepository: SupportingDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access the ObservationLogApplication instance
        val observationLogApplication = application as ObservationLogApplication

        // Set up the WedCheck model to be shared between views
        val wedCheckDao = observationLogApplication.getWedCheckDao()
        val fileUploadDao = observationLogApplication.getFileUploadDao()
        wedCheckRepository = WedCheckRepository(wedCheckDao, fileUploadDao)

        val wedCheckViewModelFactory = WedCheckViewModelFactory(application, wedCheckRepository)
        val wedCheckViewModel: WedCheckViewModel by viewModels { wedCheckViewModelFactory }

        val observationDao = observationLogApplication.getObservationDao()
        observationRepository = ObservationRepository(observationDao)

        val observersDao = observationLogApplication.getObserversDao()
        val sealColoniesDao = observationLogApplication.getSealColoniesDao()
        supportingDataRepository =
            SupportingDataRepository(observersDao, sealColoniesDao, fileUploadDao)

        val addLogViewModelFactory =
            AddLogViewModelFactory(application, observationRepository, supportingDataRepository)
        val addObservationLogViewModel: AddObservationLogViewModel by viewModels { addLogViewModelFactory }

        val homeViewModelFactory =
            HomeViewModelFactory(
                observationRepository,
                supportingDataRepository,
                FusedLocationSource(applicationContext)
            )
        val homeViewModel: HomeViewModel by viewModels { homeViewModelFactory }

        val recentObservationsViewModelFactory = RecentObservationsViewModelFactory()
        val recentObservationsViewModel: RecentObservationsViewModel by viewModels { recentObservationsViewModelFactory }

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
                                addObservationLogViewModel,
                                homeViewModel
                            )
                        }
                        composable(Screens.AddObservationLog.route) {
                            AddObservationLogScreen(
                                navController,
                                addObservationLogViewModel,
                                wedCheckViewModel,
                                recentObservationsViewModel
                            )
                        }
                        composable(Screens.RecentObservations.route) {
                            RecentObservationsScreen(
                                navController,
                                recentObservationsViewModel,
                                addObservationLogViewModel
                            )
                        }
                        composable(Screens.SealLookupScreen.route) {
                            SealLookupScreen(
                                navController,
                                wedCheckViewModel,
                                addObservationLogViewModel
                            )
                        }
                        composable(Screens.ObservationViewer.route) {
                            ObservationViewer(
                                navController,
                                addObservationLogViewModel
                            )
                        }
                        composable(Screens.AdminScreen.route) {
                            AdminScreen(
                                navController,
                                wedCheckViewModel,
                                homeViewModel,
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

