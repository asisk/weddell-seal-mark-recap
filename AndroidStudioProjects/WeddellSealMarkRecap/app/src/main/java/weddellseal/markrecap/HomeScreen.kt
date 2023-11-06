package weddellseal.markrecap

import android.os.Build.getSerial
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import weddellseal.markrecap.entryfields.DropdownField
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme

@Composable
fun HomeScreen (navController: NavHostController){
    val lifecycleOwner = LocalLifecycleOwner.current
    homeScaffold(navController)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun homeScaffold(navController: NavHostController) {
    Scaffold(
        // region UI - Top Bar & Action Button
        topBar = {
            TopAppBar(
                modifier = Modifier.height(60.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Row(
                        Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(14.dp),
                            text = "Weddell Seal Mark Recap",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Button(
                    onClick = { /* TODO: Add click action */ },
                    Modifier.padding(4.dp)
                ) {
                    Icon(Icons.Filled.Dataset, contentDescription = "View Recent Observations")
                }
                Text(
                    modifier = Modifier.fillMaxWidth(.4f),
                    text = "View Recent Observations",
                    softWrap = true)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row {
                val image = painterResource(R.drawable.pup1_2)
                Image(painter = image, contentDescription = null)
            }
            Card(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
//                    .fillMaxWidth(.5f)
//                        .size(width = 240.dp, height = 100.dp)
            ) {
                // Metadata
                Row(
                    modifier = Modifier
//                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(.3f)
                ) {
                    var observers by remember { mutableStateOf("") }
                    ObservationCardOutlinedTextField(
                        placeholderText = "observers",
                        labelText = "Observers",
                        fieldVal = observers,
                        onValueChange = {
                            observers = it
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    var site by remember { mutableStateOf("") }
                    val censusOptions = listOf("Location A", "Location B")
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(text = "Location")
                    }
                    Column(modifier = Modifier.padding(4.dp)) {
                        DropdownField(censusOptions) { newText ->
                            site = newText
                        }
                    }
                }
                val censusOptions = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8")
                var selection = "0"
                Row() {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(text = "Census #")
                    }
                    Column(modifier = Modifier.padding(4.dp)) {
                        DropdownField(censusOptions) { newText ->
                            selection = newText
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                ) {
                    //TODO, pull a system field and use it in place of This
                    var compId by remember { mutableStateOf("This") }
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(text = "Tablet Id: TBD")
                    }
//                    val serialNumber = getSerial()
//
//                    Column {
//                        // Display the serial number in your Composable
//                        Text(text = "Serial Number: $serialNumber")
//                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ){
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = { (navController.navigate(Screens.AddObservationLog.route)) },
                        icon = { Icon(Icons.Filled.PostAdd, "Start Observation") },
                        text = { Text(text = "Start Census") })
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = { (navController.navigate(Screens.AddObservationLog.route)) },
                        icon = { Icon(Icons.Filled.PostAdd, "Start Observation") },
                        text = { Text(text = "Start Observation") })
            }
        }
    }
}

@Composable
fun SerialNumberDisplay() {
    val serialNumber = getSerial()

    Column {
        // Display the serial number in your Composable
        Text(text = "Serial Number: $serialNumber")
    }
}
//
//@Composable
//fun CardWithClickableImages() {
//    var clickedImage by remember { mutableStateOf(0) }
//
//    Card(
//        modifier = Modifier
//            .padding(16.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            verticalArrangement = Arrangement.Center
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 1 })
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 2 })
//                ClickableImage(imageResId = R.drawable.pup1_2, onClick = { clickedImage = 3 })
//            }
//
//            // Optionally, display some content based on the clickedImage value
//            when (clickedImage) {
//                1 -> Text("You clicked Image 1")
//                2 -> Text("You clicked Image 2")
//                3 -> Text("You clicked Image 3")
//            }
//        }
//    }
//}
//
//@Composable
//fun ClickableImage(imageResId: Int, onClick: () -> Unit) {
//    Image(
//        painter = painterResource(id = imageResId),
//        contentDescription = null, // Provide a proper content description
//        modifier = Modifier
//            .clickable { onClick() }
//            .padding(8.dp)
//    )
//}


@Preview
@Composable
fun HomeScreen() {
    WeddellSealMarkRecapTheme {
        val navController = rememberNavController()
        homeScaffold(navController)
    }
}

//@Preview
//@Composable
//fun ImageCard() {
//    WeddellSealMarkRecapTheme {
//    }
//}