package weddellseal.markrecap.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.location.data.toLocationString
import weddellseal.markrecap.ui.CenteredAppBar
import weddellseal.markrecap.ui.NavMenu
import weddellseal.markrecap.ui.permissions.RequestPermissions
import weddellseal.markrecap.ui.permissions.missingPermissions
import weddellseal.markrecap.ui.utils.cancelAllAndClear
import weddellseal.markrecap.ui.utils.getDeviceName

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel
) {
    HomeScaffold(navController, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    navController: NavHostController,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val autoDetectedColony by viewModel.autoDetectedColony.collectAsState()
    val location by viewModel.currentLocation.collectAsState()

    val observerOptions by viewModel.observersList.collectAsState() // Collecting the list of observers
    val coloniesList by viewModel.coloniesList.collectAsState()

    // Used to request permissions for Location
    RequestPermissionsEffect(viewModel)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.jobs.cancelAllAndClear()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavMenu(navController)
        },
    ) {
        Scaffold(
            topBar = {
                CenteredAppBar(
                    onNavigationIconClick = {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                )
            },
        ) { innerPadding ->

            Box(modifier = Modifier.fillMaxSize()) {

                // Background Image
                Image(
                    painter = painterResource(R.drawable.thirtytwoyearold),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.6f // Adjust this value for desired transparency
                        }
                )

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {

                    // Metadata values - Observers, Colony, Device Name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                            .padding(start = 30.dp, end = 30.dp, top = 24.dp, bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 18.dp, end = 18.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // GPS Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (location?.coordinates?.longitude != null && location?.coordinates?.latitude != null) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF1D9C06),
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(36.dp),
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.LocationOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(36.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = location?.toLocationString() ?: "Locating...",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    location?.updatedDate?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
//                                Text(
//                                    text = if (viewModel.hasPreciseLocation(context))
//                                        "Using precise location"
//                                    else
//                                        "Using approximate location"
//                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // OBSERVERS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(.45f),
                                ) {
                                    Text(
                                        text = "Observer Initials",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    ObserversDropDown(
                                        label = "Selected Observers",
                                        allOptions = observerOptions,
                                        selectedOptions = uiState.selectedObservers,
                                        onSelectionChanged = { updatedItems ->
                                            viewModel.updateObserversSelection(
                                                updatedItems
                                            )
                                        },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // COLONY
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(.45f),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                    ) {
                                        Text(
                                            text = "Colony",
                                            style = MaterialTheme.typography.headlineMedium
                                        )

                                        Spacer(modifier = Modifier.width(30.dp))

                                        Checkbox(
                                            checked = uiState.manualColonyCheckbox,
                                            onCheckedChange = {
                                                viewModel.setManualColonyCheckbox(it)
                                                if (!it) {
                                                    viewModel.clearColony()
                                                }
                                            },
                                        )

                                        Text(
                                            text = "Override",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    if (uiState.manualColonyCheckbox) {
                                        ColonyDropDown(
                                            label = "Selected Colony",
                                            options = coloniesList,
                                            selectedOption = uiState.selectedColony,
                                            onValueChange = { valueSelected ->
                                                viewModel.updateSelectedColony(valueSelected)
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = autoDetectedColony?.location
                                                ?: "...detecting proximity to a known colony...",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // DEVICE NAME
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var deviceName by remember { mutableStateOf("") }
                                deviceName = getDeviceName(context)
                                Column(
                                    modifier = Modifier.fillMaxWidth(.45f)
                                ) {
                                    Text(
                                        text = "Device Name",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = deviceName,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestPermissionsEffect(
    vm: HomeViewModel,
) {
    val missing = LocalContext.current.missingPermissions()
    if (missing.isEmpty()) {
        vm.onPermissionsResult(true)
        return
    }
    RequestPermissions(missing, vm::onPermissionsResult)
}