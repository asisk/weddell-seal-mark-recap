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
import weddellseal.markrecap.data.WedCheckRepository
import weddellseal.markrecap.models.AddLogViewModelFactory
import weddellseal.markrecap.models.AddObservationLogViewModel
import weddellseal.markrecap.models.WedCheckViewModel
import weddellseal.markrecap.models.WedCheckViewModelFactory
import weddellseal.markrecap.ui.screens.AddObservationLogScreen
import weddellseal.markrecap.ui.screens.AddObservationSummaryScreen
import weddellseal.markrecap.ui.screens.AdminScreen
import weddellseal.markrecap.ui.screens.HomeScreen
import weddellseal.markrecap.ui.screens.RecentObservationsScreen
import weddellseal.markrecap.ui.screens.SealLookupScreen
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme


class MainActivity : ComponentActivity() {
//    private lateinit var permissionManager: PermissionManager
    private lateinit var wedCheckRepository: WedCheckRepository
    private lateinit var observationRepository: ObservationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        permissionManager = (application as ObservationLogApplication).permissions
        // Access the ObservationLogApplication instance
        val observationLogApplication = application as ObservationLogApplication

        // Set up the WedCheck model to be shared between views
        val wedCheckDao = observationLogApplication.getWedCheckDao()
        wedCheckRepository = WedCheckRepository(wedCheckDao)
        val wedCheckViewModelFactory = WedCheckViewModelFactory(application, wedCheckRepository)
        val wedCheckViewModel: WedCheckViewModel by viewModels { wedCheckViewModelFactory }

        // Set up the LogView model to be shared between views
        val observationDao = observationLogApplication.getObservationDao()
        observationRepository = ObservationRepository(observationDao)
        val addLogViewModelFactory = AddLogViewModelFactory(application, observationRepository)
        val addObservationLogViewModel: AddObservationLogViewModel by viewModels { addLogViewModelFactory }

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
                        composable(Screens.HomeScreen.route) { HomeScreen(navController) }
                        composable(Screens.AddObservationLog.route) { AddObservationLogScreen(navController, addObservationLogViewModel) }
                        composable(Screens.AddObservationSummary.route) { AddObservationSummaryScreen(navController, addObservationLogViewModel) }
                        composable(Screens.RecentObservations.route) { RecentObservationsScreen(navController) }
                        composable(Screens.SealLookupScreen.route) { SealLookupScreen(navController, wedCheckViewModel) }
                        composable(Screens.AdminScreen.route) { AdminScreen(navController, wedCheckViewModel) }
                    }
                }
            }
        }
    }
}

sealed class Screens(val route: String) {
    object HomeScreen : Screens("home")
    object AdminScreen : Screens("admin")
    object AddObservationLog : Screens("add_log")
    object AddObservationSummary : Screens("add_log_summary")
    object RecentObservations : Screens ("view_db")
    object SealLookupScreen : Screens("seal_lookup")
}

