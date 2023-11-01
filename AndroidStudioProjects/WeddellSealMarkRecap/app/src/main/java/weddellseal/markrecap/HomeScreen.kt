package weddellseal.markrecap

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Bottom app bar",
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            Row {
//                ExtendedFloatingActionButton(
//                    modifier = Modifier.padding(16.dp),
//                    onClick = { (navController.navigate(Screens.AddObservationLog.route)) },
//                    icon = { Icon(Icons.Filled.PostAdd, "Start Observation") },
//                    text = { Text(text = "Start Observation") })
//            }
            Row {
                val image = painterResource(R.drawable.pup1_2)
                Image(painter = image, contentDescription = null)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier
                        .padding(8.dp)
//                        .fillMaxWidth(.5f)
//                        .size(width = 240.dp, height = 100.dp)
                ) {
                    // Content of the Census Carde
                    Text(
                        text = "Census",
                        modifier = Modifier
                            .padding(4.dp),
                    )

                    val censusOptions = listOf("0", "1", "2", "3", "4")
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
                    FloatingActionButton(
                        onClick = { /*TODO*/ },
                        modifier = Modifier
                            .padding(4.dp)
                            .align(CenterHorizontally),
                    contentColor = LocalContentColor.current
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Start",
                            tint = MaterialTheme.colorScheme.surfaceTint
                        )
                    }
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
//                        .size(width = 240.dp, height = 100.dp)
                ) {
                    // Content of the Observation Card
                    Text(
                        text = "Observation",
                        modifier = Modifier
                            .padding(4.dp)
//                            .fillMaxWidth()
                    )
                    FloatingActionButton(
                        onClick = { (navController.navigate(Screens.AddObservationLog.route)) },
                        modifier = Modifier
                            .padding(4.dp)
                            .align(CenterHorizontally),
                        contentColor = LocalContentColor.current
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Start",
                            tint = MaterialTheme.colorScheme.surfaceTint
                        )
                    }
                }
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
                // Content of the System Data
                Text(
                    text = "Metadata",
                    modifier = Modifier
                        .padding(4.dp)
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
//                        .fillMaxWidth(.3f),
                ) {
                    var observers by remember { mutableStateOf("") }
                    ObservationCardOutlinedTextField(
                        placeholderText = "observers",
                        labelText = "Observers",
                        sealField = observers,
                        onValueChange = {
                            observers = it
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                ) {
                    var site by remember { mutableStateOf("") }
                    val censusOptions = listOf("0", "1", "2", "3", "4")
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(text = "Site")
                    }
                    Column(modifier = Modifier.padding(4.dp)) {
                        DropdownField(censusOptions) { newText ->
                            site = newText
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
//                    modifier = Modifier.fillMaxWidth(.3f)
                ) {
                    //TODO, pull a system field and use it in place of This
                    var compId by remember { mutableStateOf("This") }
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(text = "Computer Id")
                    }
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(text = compId)
                    }
                }
            }
        }
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