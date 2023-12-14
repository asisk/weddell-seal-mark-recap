package weddellseal.markrecap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import weddellseal.markrecap.ui.AddObservationLogScreen
import weddellseal.markrecap.ui.HomeScreen
import weddellseal.markrecap.ui.RecentObservationsScreen
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme


class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = (application as ObservationLogApplication).permissions

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
                        composable(Screens.AddObservationLog.route) { AddObservationLogScreen(navController) }
                        composable(Screens.RecentObservations.route) { RecentObservationsScreen(navController) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
/*        lifecycleScope.launch {
            permissionManager.checkPermissions()
        }*/
    }
}

sealed class Screens(val route: String) {
    object HomeScreen : Screens("home")
    object AddObservationLog : Screens("add_log")
    object RecentObservations : Screens ("view_db")
}

