package weddellseal.markrecap.models

/*
* Main model that stores data entered from the observation page for up to three seals
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.launch
import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.data.SupportingDataRepository
import weddellseal.markrecap.data.WedCheckSeal
import weddellseal.markrecap.ui.utils.notebookEntryValueSeal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddObservationLogViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
    private val supportingDataRepository: SupportingDataRepository
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    // for determining if GPS provider is active
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // region UI state
    data class UiState(
        val hasLocationAccess: Boolean,
        //val hasCameraAccess: Boolean,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val date: String,
        val hasGPS: Boolean = false,
        var hasGooglePlay: Int,
        val currentLocation: String = "current location empty",
        val lastKnownLocation: String = "last known location empty",
        val latLong: String = "",
        val latitude: String = "",
        val longitude: String = "",
        val isError: Boolean = false,
        val errorMessage: String = "",
        val season: String = "",
        val yearMonthDay: String = "",
        val time: String = "",
        val deviceID: String,
        val observerInitials: String = "Select an option",
        val censusNumber: String = "Select an option",
        val colonyLocation: String = "Select an option",
    )

    fun updateColonySelection(observationSiteSelected: String) {
        uiState = uiState.copy(colonyLocation = observationSiteSelected)
    }

    fun updateObserverInitials(initials: String) {
        uiState = uiState.copy(observerInitials = initials)
    }

    fun updateCensusNumber(censusNumber: String) {
        uiState = uiState.copy(censusNumber = censusNumber)
    }

    var uiState by mutableStateOf(
        UiState(
            hasLocationAccess = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
            hasGooglePlay = 999,
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z",
                Locale.US
            ).format(System.currentTimeMillis()),
            season = getCurrentYear().toString(),
            yearMonthDay = getCurrentDateFormatted(),
            time = getCurrentTimeFormatted(),
            deviceID = getDeviceName(context),
        )
    )
        private set

    var primarySeal by mutableStateOf(
        Seal(
            name = "primary",
            isStarted = false
        )
    )
        private set

    var pupOne by mutableStateOf(
        Seal(
            name = "pupOne",
            age = "Pup",
//            numRelatives = 1,
            isStarted = false
        )
    )
        private set

    var pupTwo by mutableStateOf(
        Seal(
            name = "pupTwo",
            age = "Pup",
//            numRelatives = 2,
            isStarted = false
        )
    )
        private set

    fun startPup(seal: Seal) {
        when (seal.name) {
            "pupOne" -> {
                pupOne = pupOne.copy(numRelatives = primarySeal.numRelatives, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(numRelatives = primarySeal.numRelatives, isStarted = true)
            }
        }
    }

    // called after navigation command from the summary screen to prevent the summary screen from
    // preemptively navigating back to the observation screen
    fun resetSaved() {
        // reset the values in the model once the records are save successfully
        primarySeal = Seal(
            name = "primary",
            isStarted = false
        )
        pupOne = Seal(
            name = "pupOne",
            age = "Pup",
            numRelatives = 1,
            isStarted = false
        )
        pupTwo = Seal(
            name = "pupTwo",
            age = "Pup",
            numRelatives = 2,
            isStarted = false
        )

        uiState = uiState.copy(isSaved = false)
    }

    fun updateCondition(sealName: String, input: String) {
        var condSelected = input
        if (input == "Select an option") {
            condSelected = ""
        }
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(condition = condSelected, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(condition = condSelected, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(condition = condSelected, isStarted = true)
            }
        }
    }


    fun updateWeight(seal: Seal, number: Int) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(weight = number, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(weight = number, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(weight = number, isStarted = true)
            }
        }
    }

    fun clearTagOne(seal: Seal) {
        updateTagOneNumber(seal, 0)
        updateTagOneAlpha(seal, "")
        updateTagIdOne(seal)

        updateNotebookEntry(seal)
    }

    fun clearTagTwo(seal: Seal) {
        updateTagTwoNumber(seal, 0)
        updateTagTwoAlpha(seal, "")
        updateTagIdTwo(seal)

        updateNotebookEntry(seal)
    }

    fun clearTag(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0
                )
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun resetTags(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    numTags = "",
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0,
                    tagEventType = ""
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    numTags = "",
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0,
                    tagEventType = ""
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    numTags = "",
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0,
                    tagEventType = ""
                )
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun updateNoTag(sealName: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    isNoTag = true,
                    numTags = "",
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0,
                    tagEventType = ""
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    isNoTag = true,
                    numTags = "",
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0,
                    tagEventType = ""
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    isNoTag = true,
                    numTags = "",
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = 0,
                    tagTwoNumber = 0,
                    tagEventType = ""
                )
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun updateAge(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(age = input, isStarted = true)
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(age = input, isStarted = true)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(age = input, isStarted = true)
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun updateNumRelatives(seal: Seal, input: String) {
        val number: Int? = input.toIntOrNull()
        if (number != null) {
            when (number) {
                0 -> {
                    removePups()
                }

                1 -> {
                    pupOne = pupOne.copy(numRelatives = number, isStarted = true)
                    updateNotebookEntry(pupOne)
                }

                2 -> {
                    pupTwo = pupTwo.copy(numRelatives = number, isStarted = true)
                    updateNotebookEntry(pupTwo)
                }
            }

            primarySeal = primarySeal.copy(numRelatives = number, isStarted = true)
            updateNotebookEntry(primarySeal)
        }
    }

    fun updateSex(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(sex = input, isStarted = true)
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(sex = input, isStarted = true)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(sex = input, isStarted = true)
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun updatePupPeed(sealName: String, input: Boolean) {
        when (sealName) {
            "pupOne" -> {
                pupOne = pupOne.copy(pupPeed = input, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(pupPeed = input, isStarted = true)
            }
        }
    }

    fun updateTagOneAlpha(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagOneAlpha = input, isStarted = true)
                updateTagIdOne(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagOneAlpha = input, isStarted = true)
                updateTagIdOne(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagOneAlpha = input, isStarted = true)
                updateTagIdOne(pupTwo)
            }
        }
    }

    fun updateTagTwoAlpha(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagTwoAlpha = input, isStarted = true)
                updateTagIdTwo(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagTwoAlpha = input, isStarted = true)
                updateTagIdTwo(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagTwoAlpha = input, isStarted = true)
                updateTagIdTwo(pupTwo)
            }
        }
    }

    private fun updateTagIdOne(seal: Seal) {
        var tagOneNumStr = ""
        if (seal.tagOneNumber > 0) {
            tagOneNumStr = seal.tagOneNumber.toString()
        }

        var tagIdStr = tagOneNumStr + seal.tagOneAlpha
        val number: Int? = seal.numTags.toIntOrNull()

        if (number != null && number > 1) {
            tagIdStr = tagOneNumStr + seal.tagOneAlpha + seal.tagOneAlpha
        }

        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagIdOne = tagIdStr, isStarted = true)
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagIdOne = tagIdStr, isStarted = true)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagIdOne = tagIdStr, isStarted = true)
                updateNotebookEntry(pupTwo)
            }
        }
    }

    private fun updateTagIdTwo(seal: Seal) {
        var tagIdStr = seal.tagOneNumber.toString() + seal.tagOneAlpha
        val number: Int? = seal.numTags.toIntOrNull()

        if (number != null && number > 1) {
            tagIdStr = seal.tagOneNumber.toString() + seal.tagOneAlpha + seal.tagOneAlpha
        }

        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagIdTwo = tagIdStr)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagIdTwo = tagIdStr)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagIdTwo = tagIdStr)
            }
        }
    }


    fun updateTagOneNumber(seal: Seal, input: Int) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagOneNumber = input, isStarted = true)
                updateTagIdOne(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagOneNumber = input, isStarted = true)
                updateTagIdOne(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagOneNumber = input, isStarted = true)
                updateTagIdOne(pupTwo)
            }
        }
    }

    fun updateTagTwoNumber(seal: Seal, input: Int) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagTwoNumber = input, isStarted = true)
                updateTagIdTwo(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagTwoNumber = input, isStarted = true)
                updateTagIdTwo(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagTwoNumber = input, isStarted = true)
                updateTagIdTwo(pupTwo)
            }
        }
    }

    fun updateTagEventType(seal: Seal, input: String) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagEventType = input, isStarted = true)
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagEventType = input, isStarted = true)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagEventType = input, isStarted = true)
            }
        }
    }

    fun updateOldTags(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    oldTagIdOne = seal.tagIdOne,
                    oldTagIdTwo = seal.tagIdTwo,
                    isStarted = true
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    oldTagIdOne = seal.tagIdOne,
                    oldTagIdTwo = seal.tagIdTwo,
                    isStarted = true
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    oldTagIdOne = seal.tagIdOne,
                    oldTagIdTwo = seal.tagIdTwo,
                    isStarted = true
                )
            }
        }

    }

    fun updateComment(sealName: String, input: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(comment = input, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(comment = input, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(comment = input, isStarted = true)
            }
        }
    }

    fun updateNumTags(sealName: String, input: String) {
        val isTwoTags = input.toInt() == 2
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(numTags = input, isStarted = true)
                updateTagIdOne(primarySeal)
                if (isTwoTags) {
                    updateTagIdTwo(primarySeal)
                }
            }

            "pupOne" -> {
                pupOne = pupOne.copy(numTags = input, isStarted = true)
                updateTagIdOne(pupOne)
                if (isTwoTags) {
                    updateTagIdTwo(pupOne)
                }
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(numTags = input, isStarted = true)
                updateTagIdOne(pupTwo)
                if (isTwoTags) {
                    updateTagIdTwo(pupTwo)
                }
            }
        }
    }

    fun updateTissueTaken(sealName: String, input: Boolean) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(tissueTaken = input, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tissueTaken = input, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tissueTaken = input, isStarted = true)
            }
        }
    }


    fun updateSpeNo(sealName: String, speNo: Int) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(speNo = speNo, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(speNo = speNo, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(speNo = speNo, isStarted = true)
            }
        }
    }

    fun resetPupFields(sealName: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    pupPeed = false,
                    weightTaken = false,
                    weight = 0,
                    isStarted = true
                )
            }

            "pupOne" -> {
                pupOne =
                    pupOne.copy(pupPeed = false, weightTaken = false, weight = 0, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo =
                    pupTwo.copy(pupPeed = false, weightTaken = false, weight = 0, isStarted = true)
            }
        }
    }


    fun updateIsWeightTaken(sealName: String, checked: Boolean) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(weightTaken = checked, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(weightTaken = checked, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(weightTaken = checked, isStarted = true)
            }
        }
    }

    fun updateNotebookEntry(seal: Seal) {
        val notebookEntry = notebookEntryValueSeal(seal)

        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(notebookDataString = notebookEntry)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(notebookDataString = notebookEntry)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(notebookDataString = notebookEntry)
            }
        }
    }

//    private fun validateSeal(seal: Seal): Boolean {
//        if (seal.age != "" && seal.sex != "" && seal.tagIdOne != "" && seal.tagEventType != "" && seal.tagOneNumber > 0) {
//            return true
//        }
//        return false
//    }

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen
// prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun populateSeal(wedCheckSeal: WedCheckSeal) {
        primarySeal = primarySeal.copy(
            speNo = wedCheckSeal.speNo,
            age = wedCheckSeal.age,
            sex = wedCheckSeal.sex,
            numRelatives = wedCheckSeal.numRelatives,
            tagIdOne = wedCheckSeal.tagIdOne,
            tagOneNumber = wedCheckSeal.tagOneNumber,
            tagOneAlpha = wedCheckSeal.tagOneAlpha,
            oldTagIdOne = wedCheckSeal.tagIdOne,
            oldTagIdTwo = wedCheckSeal.tagIdTwo,
            tagEventType = "Marked",
            lastPhysio = wedCheckSeal.lastPhysio,
            colony = wedCheckSeal.colony,
            isWedCheck = true,
        )
        updateNotebookEntry(primarySeal)
    }

    fun resetSeal(sealName: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = Seal(
                    name = "primary",
                    isStarted = false
                )
                // removing the primary seal results in removing pups, if present, as well
                pupOne = Seal(
                    name = "pupOne",
                    age = "Pup",
                    numRelatives = 1,
                    isStarted = false
                )
                pupTwo = Seal(
                    name = "pupTwo",
                    age = "Pup",
                    numRelatives = 2,
                    isStarted = false
                )
            }

            "pupOne" -> {
                // if pupOne is removed and there's a second pup
                if (pupTwo.isStarted) {
                    // rename the second pup and update it's number of relatives
                    pupTwo = pupTwo.copy(name = "PupOne", numRelatives = 1)
                    //reassign it to pupOne
                    pupOne = pupTwo
                } else {
                    pupOne = Seal(
                        name = "pupOne",
                        age = "Pup",
                        numRelatives = 1,
                        isStarted = false
                    )
                }

                val parentNumRels = primarySeal.numRelatives - 1
                primarySeal = primarySeal.copy(numRelatives = parentNumRels)
            }

            "pupTwo" -> {
                pupTwo = Seal(
                    name = "pupTwo",
                    age = "Pup",
                    numRelatives = 2,
                    isStarted = false
                )
                // update parent num rels
                val parentNumRels = primarySeal.numRelatives - 1
                primarySeal = primarySeal.copy(numRelatives = parentNumRels)
            }
        }
    }

    fun removePups() {
        primarySeal = primarySeal.copy(numRelatives = 0)

        //called when primary seal number of relatives is set to zero
        pupOne = Seal(
            name = "pupOne",
            age = "Pup",
            numRelatives = 1,
            isStarted = false
        )
        pupTwo = Seal(
            name = "pupTwo",
            age = "Pup",
            numRelatives = 2,
            isStarted = false
        )
    }


    fun isValid(): Boolean {
//        if (!observationSaver.isEmpty() && !uiState.isSaving) {
        if (!uiState.isSaving) {
            return true
        }
        return false
//        return !observationSaver.isEmpty() && !uiState.isSaving
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionChange(permission: String, isGranted: Boolean) {
        when (permission) {
            /*            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            uiState = uiState.copy(hasLocationAccess = isGranted)
                        }*/
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                uiState = uiState.copy(hasLocationAccess = isGranted)
            }
            /*            Manifest.permission.CAMERA -> {
                            uiState = uiState.copy(hasCameraAccess = isGranted)
                        }*/
            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }
// endregion

    // region Location management
    @SuppressLint("MissingPermission")
    fun fetchGeoCoderLocation() {
        val isGooglePlay =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (isGooglePlay != null) {
            uiState.hasGooglePlay = isGooglePlay
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location ?: return@addOnSuccessListener

            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    val address = addresses.firstOrNull()
                    val place = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                    ?: address?.countryName
                }
            } else {
                val address =
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        ?.firstOrNull()
                        ?: return@addOnSuccessListener
                val place =
                    address.locality ?: address.subAdminArea ?: address.adminArea
                    ?: address.countryName
                    ?: return@addOnSuccessListener
            }
        }
    }

    // region Location management
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            })
            .addOnSuccessListener { currentLocation: Location? ->
                if (currentLocation == null) {
                    val errorMessage = "Cannot get current location"
                    uiState = uiState.copy(currentLocation = errorMessage)
                } else {
                    val lat = currentLocation.latitude
                    val lon = currentLocation.longitude
                    val date = SimpleDateFormat("dd.MM.yyyy HH:mm:ss aaa z", Locale.US).format(
                        System.currentTimeMillis()
                    )
                    uiState = uiState.copy(
                        currentLocation =
                        "Lat : ${lat}\n" +
                                "Long : ${lon}\n" +
                                "updated: $date"
                    )
                    uiState = uiState.copy(
                        latLong = "Lat : $lat Long : $lon"
                    )
                    uiState = uiState.copy(latitude = lat.toString(), longitude = lon.toString())
                }
            }
    }

    fun createLog(vararg seals: Seal) {
        uiState = uiState.copy(isSaving = true, isSaved = false)
        for (seal in seals) {
            if (seal.isStarted) {
//                if (seal.isStarted && validateSeal(seal)) {
                val log = buildLogEntry(seal)

                //write an entry to the database for each seal that has valid input
                viewModelScope.launch {
                    observationRepo.addObservation(log)
                }

                uiState = uiState.copy(isSaved = true)
            }
        }
        uiState = uiState.copy(isSaving = false)
    }

    private fun buildLogEntry(seal: Seal): ObservationLogEntry {
        var censusNumber = ""
        if (uiState.censusNumber != "Select an option") {
            censusNumber = uiState.censusNumber
        }

        var observers = ""
        if (uiState.observerInitials != "Select an option") {
            observers = uiState.observerInitials
        }

        var ageClass = ""
        if (seal.age != "") {
            ageClass = seal.age[0].toString()
        }

        var sex = ""
        if (seal.sex != "") {
            sex = seal.sex[0].toString()
        }

        var numRels = ""
        var pupOneTagIdOne = ""
        var pupTwoTagIdOne = ""
        if (seal.numRelatives == 1) {
            numRels = seal.numRelatives.toString()
            pupOneTagIdOne = getPupTagIdOne(pupOne)
        }
        if (seal.numRelatives == 2) {
            numRels = seal.numRelatives.toString()
            pupOneTagIdOne = getPupTagIdOne(pupOne)
            pupTwoTagIdOne = getPupTagIdOne(pupTwo)
        }

        var eventType = ""
        var tagOneIndicator = ""
        var tagTwoIndicator = ""
        if (seal.tagEventType.isNotEmpty()) {
            eventType = when (seal.tagEventType) {
                "Marked" -> {
                    "M"
                }

                "New" -> {
                    tagOneIndicator = "+"
                    val number: Int? = seal.numTags.toIntOrNull()
                    if (number != null && number > 1) { // this is the definition for two tags
                        tagTwoIndicator = "+"
                    }
                    "N"
                }

                "Retag" -> {
                    "R2"
                }

                else -> {
                    ""
                }
            }
        }

        if(seal.isNoTag) {
            eventType = "M"
        }

        var condition = ""
        if (seal.condition != "" && seal.condition != "None") {
            condition = seal.condition.last().toString()
        }

        var pupWeight = ""
        if (seal.weight > 0) {
            pupWeight = seal.weight.toString()
        }

        var tissue = ""
        if (seal.tissueTaken) {
            tissue = "Tissue"
        }

        val log = ObservationLogEntry(
            // passing zero, but Room entity will autopopulate the id
            id = 0,
            deviceID = uiState.deviceID,
            season = uiState.season,
            speno = seal.speNo.toString(),
            date = uiState.yearMonthDay, // date format: yyyy-MM-dd
            time = uiState.time, // time format: hh:mm:ss
            censusID = censusNumber,
            latitude = uiState.latitude,  // example -77.73004, could also be 4 decimal precision
            longitude = uiState.longitude, // example 166.7941, could also be 2 decimal precision
            ageClass = ageClass,
            sex = sex,
            numRelatives = numRels,
            oldTagIDOne = seal.oldTagIdOne,
            oldTagIDTwo = seal.oldTagIdTwo,
            tagIDOne = seal.tagIdOne,
            tagOneIndicator = tagOneIndicator,
            tagIDTwo = seal.tagIdTwo,
            tagTwoIndicator = tagTwoIndicator,
            relativeTagIDOne = pupOneTagIdOne,
            relativeTagIDTwo = pupTwoTagIdOne,
            sealCondition = condition,
            observerInitials = observers,
            flaggedEntry = "", // TODO, need to figure out when this gets triggered
            tagEvent = eventType,
            weight = pupWeight,
            tissueSampled = tissue,
            comments = seal.comment,
            colony = uiState.colonyLocation,
        )
        return log
    }

    private fun getCurrentYear(): Int {
        return LocalDate.now().year
    }

    private fun getCurrentDateFormatted(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return currentDate.format(formatter)
    }

    private fun getCurrentTimeFormatted(): String {
        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return currentTime.format(formatter)
    }

    private fun getPupTagIdOne(pup: Seal): String {
        return if (pup.isStarted) {
//            return if (pup.isStarted && validateSeal(pup)) {
            pup.tagIdOne
        } else {
            ""
        }
    }

    private fun getDeviceName(context: Context): String {
        return Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: "Unknown Device"
    }


}