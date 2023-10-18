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
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme

//const val ACCESS_COARSE_LOCATION = 0

class MainActivity : ComponentActivity() {
    lateinit var permissionManager: PermissionManager
//    lateinit var logWriter : CSVLogWriter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = (application as ObservationLogApplication).permissions
//        logWriter = CSVLogWriter((this as MainActivity).activityResultRegistry)
//        lifecycle.addObserver(logWriter)
        // register data access callback

        setContent {
            WeddellSealMarkRecapTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startNavigation = Screens.HomeScreen.route
                    NavHost(navController = navController, startDestination = startNavigation) {
                        composable(Screens.HomeScreen.route) { HomeScreen(navController)}
                        composable(Screens.AddObservationLog.route) { AddObservationLogScreen(navController) }
                        //composable(Screens.Camera.route) { CameraScreen(navController) }
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

    // TODO: Step 1. Create Data Access Audit Listener Object
}

sealed class Screens(val route: String) {
    object HomeScreen : Screens("home")
    object AddObservationLog : Screens("add_log")
    //object Camera : Screens("camera")
}
/*
data class Seal(val speNo: String, val age: String)

class ObservationViewModel : ViewModel() {
    private val _gps: MutableLiveData<String> = MutableLiveData("")
    val gps: LiveData<String> = _gps

    private val _tagId: MutableLiveData<String> = MutableLiveData("")
    val tagId: LiveData<String> = _tagId

    fun onPopGPS (newGPS: String) {
        _gps.value = newGPS
    }

    fun onTagIdChange (newTag: String) {
        _tagId.value = newTag
    }
}

@Composable
fun ObservationEntryScreen(observationViewModel: ObservationViewModel = viewModel()){
    val gps : String by observationViewModel.gps.observeAsState(initial = "")
    val tagId : String by observationViewModel.tagId.observeAsState(initial = "")
    SealCard(gps = gps, tagId = tagId) { observationViewModel.onPopGPS(it); observationViewModel.onTagIdChange(it) }
}

@Composable
fun SealCard(gps: String, tagId: String, onTagIdChange: (String) -> Unit) {
    Column (modifier = Modifier.padding(all = 16.dp)) {
        // field label
        OutlinedTextField(
            value = tagId,
            onValueChange = onTagIdChange,
            label = {Text("TagId")}
        )

        // Add a vertical space between the speNo and age texts
        Spacer(modifier = Modifier.height(4.dp))
        Text (
            text = "Data entered",
            modifier = Modifier.padding(all = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "TagId: $tagId",
            modifier = Modifier.padding(all = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "GPS: $gps",
            modifier = Modifier.padding(all = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
*//*
@Composable
fun getLatLong(lm: LocationManager, context: Context): String {
    var longitude: Double = 0.0
    var latitude: Double = 0.0
    val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) as Location
    longitude = loc.longitude
    latitude = loc.latitude

    val long = longitude.toString()
    val lat = latitude.toString()
    return lat.plus(", ").plus(long)
}
*//*
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PreviewSealCard() {
    WeddellSealMarkRecapTheme {
        ObservationEntryScreen()
    }
}*/

