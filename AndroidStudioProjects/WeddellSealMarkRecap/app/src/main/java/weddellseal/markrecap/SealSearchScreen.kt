//package weddellseal.markrecap
//
///*
// * Copyright (C) 2022 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
////import coil.compose.AsyncImage
////import android.Manifest.permission.ACCESS_COARSE_LOCATION
//
//import android.Manifest.permission.ACCESS_FINE_LOCATION
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.selection.toggleable
//import androidx.compose.material.ExperimentalMaterialApi
//import androidx.compose.material.ModalBottomSheetLayout
//import androidx.compose.material.ModalBottomSheetValue
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.rememberModalBottomSheetState
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.Checkbox
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.ExtendedFloatingActionButton
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SnackbarHost
//import androidx.compose.material3.SnackbarHostState
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.semantics.Role
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import kotlinx.coroutines.launch
//import weddellseal.markrecap.entryfields.TextFieldValidateOnCharCount
//
//
//@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
//@Composable
//fun SealSearchScreen(
//    navController: NavHostController,
//    viewModel: AddObservationLogViewModel = viewModel(factory = AddLogViewModelFactory())
//) {
//    // region State initialization
//    val state = viewModel.uiState
//    val context = LocalContext.current
//    val snackbarHostState = remember { SnackbarHostState() }
//    val coroutineScope = rememberCoroutineScope()
//    val internalPhotoPickerState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
//    //    fun canAddPhoto(callback: () -> Unit) {
////        if (viewModel.canAddPhoto()) {
////            callback()
////        } else {
////            coroutineScope.launch {
////                snackbarHostState.showSnackbar("You can't add more than $MAX_LOG_PHOTOS_LIMIT photos")
////            }
////        }
////    }
//    // endregion
//
//    // TODO: Step 1. Register ActivityResult to request Camera permission
//
//    // TODO: Step 3. Add explanation dialog for Camera permission
//
//    // TODO: Step 5. Register ActivityResult to request Location permissions
//
//    val requestLocationPermissions =
//        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                //viewModel.onPermissionChange(ACCESS_COARSE_LOCATION, isGranted)
//                viewModel.onPermissionChange(ACCESS_FINE_LOCATION, isGranted)
//                viewModel.fetchCurrentLocation()
//            } else {
//                //coroutineScope.launch {
//                //    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
//                //}
//            }
//        }
//
//    // TODO: Step 8. Change activity result to only request Coarse Location
//
//    // TODO: Step 6. Add explanation dialog for Location permissions
//    var showExplanationDialogForLocationPermission by remember { mutableStateOf(false) }
//    if (showExplanationDialogForLocationPermission) {
//        LocationExplanationDialog(
//            onConfirm = {
//                requestLocationPermissions.launch(ACCESS_FINE_LOCATION)
//                showExplanationDialogForLocationPermission = false
//                viewModel.fetchCurrentLocation()
////                viewModel.fetchGeoCoderLocation()
//            },
//            onDismiss = { showExplanationDialogForLocationPermission = false },
//        )
//    }
//
//    // method called on the initial load of the ObservationLog Screen
//    // if permissions are in place it gathers information about the
//    // current and last known locations to populate location fields
//    fun canAddLocation() {
//        if (viewModel.hasPermission(ACCESS_FINE_LOCATION)) {
//            viewModel.fetchCurrentLocation()
////            viewModel.fetchGeoCoderLocation()
//        } else {
//            //requestLocationPermissions.launch(ACCESS_FINE_LOCATION)
//            showExplanationDialogForLocationPermission = true
//        }
//    }
//
//    // TODO: Step 11. Register ActivityResult to launch the Photo Picker
//    // region helper functions
//
//    LaunchedEffect(Unit) {
//        //viewModel.refreshSavedPhotos()
//        // preload the model with location data
//
//        //trigger location data population
//        canAddLocation()
//    }
//
//    //send the user back to the home screen when a log is saved
//    LaunchedEffect(state.isSaved) {
//        if (state.isSaved) {
//            navController.navigate(Screens.HomeScreen.route) {
//                popUpTo(Screens.HomeScreen.route) {
//                    inclusive = false
//                }
//            }
//        }
//    }
//
//    fun canSaveLog(callback: () -> Unit) {
//        if (viewModel.isValid()) {
//            callback()
//            coroutineScope.launch {
//                snackbarHostState.showSnackbar("Successfully saved!")
//            }
//        } else {
//            coroutineScope.launch {
//                snackbarHostState.showSnackbar("You haven't completed all details")
//            }
//        }
//    }
//    // endregion
//
//    // region UI - Bottom Sheet
//    ModalBottomSheetLayout(
//        sheetState = internalPhotoPickerState,
//        sheetContent = {
//            /* PhotoPicker(
//                modifier = Modifier.fillMaxSize(),
//                entries = state.localPickerPhotos,
//                onSelect = { uri ->
//                    coroutineScope.launch {
//                        internalPhotoPickerState.hide()
//                        viewModel.onLocalPhotoPickerSelect(uri)
//                    }
//                }
//            )*/
//        }
//    )
//    // endregion
//    {
//        Scaffold(
//            snackbarHost = { SnackbarHost(snackbarHostState) },
//            // region UI - Top Bar & Action Button
//            topBar = {
//                TopAppBar(title = { Text("Add ObservationLog", fontFamily = FontFamily.Serif) },
//                    navigationIcon = {
//                        if (navController.previousBackStackEntry != null) {
//                            IconButton(onClick = { navController.navigateUp() }) {
//                                Icon(
//                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                    contentDescription = "Back"
//                                )
//                            }
//                        }
//                    }
//                )
//            },
//                        floatingActionButton = {
//                ExtendedFloatingActionButton(
//                    text = { Text("Save log") },
//                    icon = {
//                        if (state.isSaving) {
//                            CircularProgressIndicator(Modifier.size(24.0.dp))
//                        } else {
//                            Icon(Icons.Filled.Check, null)
//                        }
//                    },
//                    onClick = {
//                        canSaveLog {
//                            viewModel.createLog()
//                        }
//                    }
//                )
//            }
//            // endregion
//        ) { innerPadding ->
//            Column(
//                modifier = Modifier
//                    .padding(innerPadding),
//                verticalArrangement = Arrangement.Center
//            ) {
//                Card(
//                    elevation = CardDefaults.cardElevation(
//                        defaultElevation = 6.dp
//                    ),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
//                    ),
//                    modifier = Modifier
//                        .padding(8.dp)
//                        .fillMaxWidth()
//                ) {
//                    //TAG ID
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(4.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        NumberInputField(
//                            "Enter TagId",
//                            "TagId",
//                            viewModel.uiState.tagId
//                        ) { newText ->
//                            viewModel.updateTagIdNum(newText)
//                        }
//                        Button(
//                            onClick = { viewModel.appendAlphaToTagID("A") },
//                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow), // Change the background color
//                        ) { Text("A", color = Color.Black) }
//                        Button(
//                            onClick = { viewModel.appendAlphaToTagID("C") },
//                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
//                        ) { Text("C", color = Color.Black) }
//                        Button(
//                            onClick = { viewModel.appendAlphaToTagID("D") },
//                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
//                        ) { Text("D", color = Color.White) }
//                    }
//                    // NUMBER OF TAGS && NUMBER OF RElATIVES
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(4.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        TextFieldValidateOnCharCount(
//                            1,
//                            "Number of Tags",
//                            "Enter number of tags present",
//                            null
//                        ) { newVal -> viewModel.updateNumTags(newVal.toInt()) }
//
//                        TextFieldValidateOnCharCount(
//                            1,
//                            "Number of Relatives",
//                            "Enter number of relatives present",
//                            null
//                        ) { newVal -> viewModel.updateNumTags(newVal.toInt()) }
//                    }
//                    //AGE
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(4.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(text = "Age:")
//                        Text(text = viewModel.uiState.age)
//                        Button(onClick = { viewModel.updateAge("Adult") }) { Text("Adult") }
//                        Button(onClick = { viewModel.updateAge("Pup") }) { Text("Pup") }
//                        Button(onClick = { viewModel.updateAge("Yearling") }) { Text("Yearling") }
//                    }
//                    // SEX
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(4.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(text = "Sex:")
//                        Text(text = viewModel.uiState.sex)
//                        Button(onClick = { viewModel.updateSex("Female") }) { Text("Female") }
//                        Button(onClick = { viewModel.updateSex("Male") }) { Text("Male") }
//                        Button(onClick = { viewModel.updateSex("Unknown") }) { Text("Unknown") }
//                    }
//                    val (checkedState, onStateChange) = remember { mutableStateOf(false) }
//                    Row(
//                        Modifier
//                            .fillMaxWidth()
//                            .height(56.dp)
//                            .toggleable(
//                                value = checkedState,
//                                onValueChange = { onStateChange(!checkedState) },
//                                role = Role.Checkbox
//                            )
//                            .padding(horizontal = 16.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Checkbox(
//                            checked = checkedState,
//                            onCheckedChange = null // null recommended for accessibility with screenreaders
//                        )
//                        Text(
//                            text = "Tissue Sampled",
//                            style = MaterialTheme.typography.bodyLarge,
//                            modifier = Modifier.padding(start = 16.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}