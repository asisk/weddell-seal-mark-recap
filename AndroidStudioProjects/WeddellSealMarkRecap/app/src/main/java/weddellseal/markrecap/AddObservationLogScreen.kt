package weddellseal.markrecap

/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//import coil.compose.AsyncImage
//import android.Manifest.permission.ACCESS_COARSE_LOCATION

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AddObservationLogScreen(
    navController: NavHostController,
    viewModel: AddObservationLogViewModel = viewModel(factory = AddLogViewModelFactory())
) {
    // region State initialization
    val state = viewModel.uiState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val internalPhotoPickerState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    //    fun canAddPhoto(callback: () -> Unit) {
//        if (viewModel.canAddPhoto()) {
//            callback()
//        } else {
//            coroutineScope.launch {
//                snackbarHostState.showSnackbar("You can't add more than $MAX_LOG_PHOTOS_LIMIT photos")
//            }
//        }
//    }
    // endregion

    // TODO: Step 1. Register ActivityResult to request Camera permission

    // TODO: Step 3. Add explanation dialog for Camera permission

    // TODO: Step 5. Register ActivityResult to request Location permissions

    val requestLocationPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                //viewModel.onPermissionChange(ACCESS_COARSE_LOCATION, isGranted)
                viewModel.onPermissionChange(ACCESS_FINE_LOCATION, isGranted)
                viewModel.fetchCurrentLocation()
            } else {
                //coroutineScope.launch {
                //    snackbarHostState.showSnackbar("Location currently disabled due to denied permission.")
                //}
            }
        }

    // TODO: Step 8. Change activity result to only request Coarse Location

    // TODO: Step 6. Add explanation dialog for Location permissions
    var showExplanationDialogForLocationPermission by remember { mutableStateOf(false) }
    if (showExplanationDialogForLocationPermission) {
        LocationExplanationDialog(
            onConfirm = {
                requestLocationPermissions.launch(ACCESS_FINE_LOCATION)
                showExplanationDialogForLocationPermission = false
                viewModel.fetchCurrentLocation()
//                viewModel.fetchGeoCoderLocation()
            },
            onDismiss = { showExplanationDialogForLocationPermission = false },
        )
    }

    // method called on the initial load of the ObservationLog Screen
    // if permissions are in place it gathers information about the
    // current and last known locations to populate location fields
    fun canAddLocation() {
        if (viewModel.hasPermission(ACCESS_FINE_LOCATION)) {
            viewModel.fetchCurrentLocation()
//            viewModel.fetchGeoCoderLocation()
        } else {
            //requestLocationPermissions.launch(ACCESS_FINE_LOCATION)
            showExplanationDialogForLocationPermission = true
        }
    }

    // TODO: Step 11. Register ActivityResult to launch the Photo Picker
    // region helper functions

    LaunchedEffect(Unit) {
        //viewModel.refreshSavedPhotos()
        // preload the model with location data

        //trigger location data population
        canAddLocation()
    }

    //send the user back to the home screen when a log is saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.navigate(Screens.HomeScreen.route) {
                popUpTo(Screens.HomeScreen.route) {
                    inclusive = false
                }
            }
        }
    }

    fun canSaveLog(callback: () -> Unit) {
        if (viewModel.isValid()) {
            callback()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Successfully saved!")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("You haven't completed all details")
            }
        }
    }
    // endregion

    // region UI - Bottom Sheet
    ModalBottomSheetLayout(
        sheetState = internalPhotoPickerState,
        sheetContent = {
            /* PhotoPicker(
                modifier = Modifier.fillMaxSize(),
                entries = state.localPickerPhotos,
                onSelect = { uri ->
                    coroutineScope.launch {
                        internalPhotoPickerState.hide()
                        viewModel.onLocalPhotoPickerSelect(uri)
                    }
                }
            )*/
        }
    )
    // endregion
    {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // region UI - Top Bar & Action Button
            topBar = {
                TopAppBar(title = { Text("Add ObservationLog", fontFamily = FontFamily.Serif) },
                    navigationIcon = {
                        if (navController.previousBackStackEntry != null) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Save log") },
                    icon = {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.size(24.0.dp))
                        } else {
                            Icon(Icons.Filled.Check, null)
                        }
                    },
                    onClick = {
                        canSaveLog {
                            viewModel.createLog()
                        }
                    }
                )
            }
            // endregion
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center
            ) {
                SealCard(viewModel, viewModel.adultSeal)

                var addPup by remember { mutableStateOf(false) }
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    onClick = { addPup = true },
                    icon = { Icon(Icons.Filled.Add, "Add Pup") },
                    text = { Text(text = "Add Pup") }
                )
                if (addPup) {
                    // show new card and show summary fields from parent

                    val stringBuilder = StringBuilder()
                    var age = viewModel.adultSeal.age[0]
                    var sex = viewModel.adultSeal.sex[0]
                    var numRels = viewModel.adultSeal.numRelatives
                    var tag = viewModel.adultSeal.tagId
                    var event = viewModel.adultSeal.tagEventType[0]

                    // Append strings to the StringBuilder
                    stringBuilder.append(age)
                    stringBuilder.append(sex)
                    stringBuilder.append(numRels)
                    stringBuilder.append("  ")
                    stringBuilder.append(tag)
                    stringBuilder.append("  ")
                    stringBuilder.append(event)

                    // Get the final string
                    val resultString = stringBuilder.toString()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(.7f)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ListItem(
                            headlineContent = { Text("Adult Seal Details: ", style = MaterialTheme.typography.titleLarge) },
                            trailingContent = {
                                Text(text = resultString, style = MaterialTheme.typography.titleLarge)
                            }
                        )
                    }
                }
            }
        }
    }
}
//                HorizontalDivider()
//                ListItem(
//                    headlineContent = { Text("TagId") },
//                    trailingContent = { Text(text = viewModel.uiState.tagId)}
//                )
//                HorizontalDivider()
//                ListItem(
//                    headlineContent = { Text("Age") },
//                    trailingContent = { Text(text = viewModel.uiState.age)}
//                )
//                }
//                HorizontalDivider()
//                ListItem(
//                    headlineContent = { Text("GPS Locator Available") },
//                    trailingContent = {
//                        Text(text = viewModel.uiState.hasGPS.toString())
//                    }
//                )
//                HorizontalDivider()
//                ListItem(
//                    headlineContent = { Text("Google Play Services Available") },
//                    trailingContent = {
//                        if (viewModel.uiState.hasGooglePlay != null) {
//                            Text(text = viewModel.uiState.hasGooglePlay.toString())
//                        } else {
//                            Text(text = "Google Play is not currently enabled")
//                        }
//                    }
//                )
//                // region Location
//                HorizontalDivider()
//                // endregion
//                ListItem(
//                    headlineContent = { Text("Last Known Location") },
//                    trailingContent = {
//                        Text(text = viewModel.uiState.lastKnownLocation)
//                    }
//                )
//
//                ListItem(
//                    headlineContent = { Text("Device GPS") },
//                    trailingContent = { Text(text = viewModel.uiState.currentLocation) }
//                )
//        }
//    }

@Composable
fun SingleSelectButtonGroup(
    txtOptions: List<String>,
    onValChangeDo: (String) -> Unit
) {
    var selectedButton by remember { mutableStateOf("") }
    txtOptions.forEach { option ->
        ElevatedButton(
            onClick = {
                selectedButton = option
                onValChangeDo(option) // Call the callback when the button is clicked
            },
            colors = ButtonDefaults.elevatedButtonColors(MaterialTheme.colorScheme.tertiary),
            enabled = selectedButton != option
        ) {
            Text(
                color = Color.White,
                text = option
            )
        }
    }
}


@Composable
fun CommentField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var textEntered by remember { mutableStateOf(value) }
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = textEntered,
        onValueChange = {
            textEntered = it
        },
        modifier = Modifier
            .background(color = Color.White)
            .border(1.dp, color = Color.LightGray)
            .padding(16.dp)
            .height(40.dp)
            .fillMaxWidth()
            .verticalScroll(state = scrollState, enabled = true),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Handle "Done" button action
                onValueChange(textEntered)
                keyboardController?.hide()
            }
        ),
        textStyle = TextStyle(fontSize = 16.sp),
        singleLine = false,
        maxLines = 5
    )
}


@ExperimentalMaterial3Api
@Composable
fun NumberInputField(
    fieldVal: String,
    placeholderText: String,
    labelText: String,
    onValChangeDo: (String) -> Unit
) {
    TextField(
        value = fieldVal,
        onValueChange = {
            onValChangeDo(it)
        },
        label = { Text(labelText) },
        placeholder = { Text(placeholderText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        trailingIcon = {
            Icon(Icons.Filled.Clear, contentDescription = "Clear text",
                Modifier.clickable { onValChangeDo("") })
        }
    )
}

@Composable
fun ObservationCardOutlinedTextField(
    placeholderText: String,
    labelText: String,
    fieldVal: String,
    onValueChange: (String) -> Unit
) {
    val paddingModifier = Modifier.padding(10.dp)
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = fieldVal,
        placeholder = { Text(placeholderText) },
        onValueChange = {
            onValueChange(it)
            isFocused = it.isNotBlank()
        },
        label = { Text(text = labelText) },
        modifier = Modifier
            .background(
                color = if (isFocused) Color.LightGray else Color.Transparent, // Change border color when focused
            ),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                isFocused = fieldVal.isNotBlank()
                defaultKeyboardAction((ImeAction.Done))
            }
        )
    )
}
//
//@Composable
//fun DropdownField(onValueChange: (String) -> Unit) {
//    var expanded by remember { mutableStateOf(false) }
//    var selectedOption by remember { mutableStateOf("Select an option") }
//    val options = listOf("Adult", "Pup", "Unknown")
//
//    Column {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { expanded = true },
//            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
//            border = BorderStroke(1.dp, Color.Gray), // Border appearance
////            contentColor = Color.Black // Text color
//        ) {
//            Row (
//                modifier = Modifier.padding(8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ){
//                BasicTextField(
//                    value = selectedOption,
//                    onValueChange = { onValueChange(selectedOption) },
//                    enabled = false,
//                    keyboardOptions = KeyboardOptions.Default.copy(
//                        imeAction = ImeAction.Done
//                    ),
//                    keyboardActions = KeyboardActions(
//                        onDone = {
//                            expanded = !expanded
//                        }
//                    )
//                )
//                Icon(
//                    imageVector = Icons.Default.ArrowDropDown,
//                    contentDescription = "Dropdown Icon",
//                    tint = Color.Black
//                )
//            }
//        }
//        if (expanded) {
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable { expanded = true },
//                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
//                border = BorderStroke(1.dp, Color.Gray), // Border appearance
//            ) {
//                DropdownMenu(
//                    expanded = expanded,
//                    onDismissRequest = { expanded = false }
//                ) {
//                    options.forEach { option ->
//                        DropdownMenuItem(text = { Text(text = option) },
//                            onClick = {
//                                selectedOption = option
//                                onValueChange(selectedOption)
//                                expanded = false
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

@Composable
fun sealCard() {
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
    ) {

    }
}

@Composable
fun LocationPicker(address: String?, fetchLocation: () -> Unit) {
    TextButton(onClick = { fetchLocation() }) {
        Icon(Icons.Filled.Explore, null)
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(address ?: "Get location")
    }
}

@Composable
fun LocationExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location access") },
        text = { Text("Weddell Seal Mark Recap app would like access to your location to save it when creating a log") },
        icon = {
            Icon(
                Icons.Filled.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceTint
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Preview
@Composable
fun ObservationScreen() {
    WeddellSealMarkRecapTheme {
//        ChipInputForm(chips)
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun TextFieldWithLengthValidation() {
////   Row {
//       var text by rememberSaveable { mutableStateOf("") }
//       val errorMessage = "Text input too long"
//       var isError by rememberSaveable { mutableStateOf(false) }
//       val charLimit = 1
//
//       fun validate(text: String) {
//           isError = text.length > charLimit
//       }
//       TextField(
//           value = text,
//           onValueChange = {
//               text = it
//               validate(text)
//           },
//           label = { Text("Label") },
//           placeholder = { Text("example@gmail.com") },
//           singleLine = true,
//           supportingText = {
//               Text(
//                   modifier = Modifier.fillMaxWidth(),
//                   text = "Limit: ${text.length}/$charLimit",
//                   textAlign = TextAlign.End,
//               )
//           },
//           isError = isError,
//           keyboardActions = KeyboardActions { validate(text) },
//           modifier = Modifier.semantics {
//               // Provide localized description of the error
//               if (isError) error(errorMessage)
//           },
//           leadingIcon = {
//               Icon(
//                   Icons.Filled.Favorite,
//                   contentDescription = "Localized description"
//               )
//           },
//           trailingIcon = { Icon(Icons.Filled.Info, contentDescription = "Localized description") }
//       )
//    }
//    Row {
//        ElevatedButton(onClick = { /* Do something! */ }) { Text("Elevated Button") }
//    }
//    Row {
//        FilledTonalButton(onClick = { /* Do something! */ }) { Text("Filled Tonal Button") }
//    }
//    Row {
//        var checked by remember { mutableStateOf(false) }
//        FilledIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
//            if (checked) {
//                Icon(Icons.Filled.Lock, contentDescription = "Localized description")
//            } else {
//                Icon(Icons.Outlined.Lock, contentDescription = "Localized description")
//            }
//        }
//    }

//    val (checkedState, onStateChange) = remember { mutableStateOf(true) }
//    Row(
//        Modifier
//            .fillMaxWidth()
//            .height(56.dp)
//            .toggleable(
//                value = checkedState,
//                onValueChange = { onStateChange(!checkedState) },
//                role = Role.Checkbox
//            )
//            .padding(horizontal = 16.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Checkbox(
//            checked = checkedState,
//            onCheckedChange = null // null recommended for accessibility with screenreaders
//        )
//        Text(
//            text = "Option selection",
//            style = MaterialTheme.typography.bodyLarge,
//            modifier = Modifier.padding(start = 16.dp)
//        )
//    }




