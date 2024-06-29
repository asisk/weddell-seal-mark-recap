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
import java.text.SimpleDateFormat
import java.util.Locale

class AddObservationLogViewModel(
    application: Application,
    private val observationRepo: ObservationRepository
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()
//    val observationDao = AppDatabase.getDatabase(application).observationDao()

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
        val isError: Boolean = false,
        val errorMessage: String = "",
    )

    var uiState by mutableStateOf(
        UiState(
            hasLocationAccess = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
            hasGooglePlay = 999,
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z",
                Locale.US
            ).format(System.currentTimeMillis()),
        )
    )
        private set

    // region Seal state
    data class Seal(
        val age: String = "",
        val ageYears: Int = 0,
        val comment: String = "",
        val condition: String = "",
        var isStarted: Boolean = false,
        val isWedCheckRecord: Boolean = false,
        val lastSeenSeason: Int = 0,
        val massPups: String = "",
        val name: String = "",
        val notebookDataString: String = "",
        val numRelatives: Int = 0,
        val numTags: Int = 0,
        val photoYears: String = "",
        val previousPups: String = "",
        val pupPeed: Boolean = false,
        val sex: String = "",
        val speNo: Int = 0,
        val swimPups: String = "",
        val tagAlpha: String = "",
        val tagEventType: String = "",
        val tagId: String = "",
        val tagNumber: Int = 0,
        val tissueTaken: Boolean = false,
        val tissueSampled: String = ""
    )

    var adultSeal by mutableStateOf(Seal(name = "adult", age = "Adult"))
        private set
    var pupOne by mutableStateOf(Seal(name = "pupOne", age = "Pup"))
        private set
    var pupTwo by mutableStateOf(Seal(name = "pupTwo", age = "Pup"))
        private set

    var wedCheckSeal by mutableStateOf(Seal())

    fun resetWedCheckSeal() {
        wedCheckSeal = Seal()
    }

    fun startPup(seal: Seal) {
        when (seal.name) {
            "pupOne" -> {
                pupOne = pupOne.copy(numRelatives = adultSeal.numRelatives, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(numRelatives = adultSeal.numRelatives, isStarted = true)
            }
        }
    }

    fun updateCondition(sealName: String, input: String) {
        var condSelected = input
        if (input == "Select an option") {
            condSelected = ""
        }
        when (sealName) {
            "adult" -> {
                adultSeal = adultSeal.copy(condition = condSelected, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(condition = condSelected, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(condition = condSelected, isStarted = true)
            }
        }
    }

    fun clearTag(seal: Seal) {
        updateTagNumber(seal, 0)
        updateTagAlpha(seal, "")
        updateTagId(seal)
        updateNotebookEntry(seal)
    }

    fun updateAge(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(age = input, isStarted = true)
                updateNotebookEntry(adultSeal)
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
            when (seal.name) {
                "adult" -> {
                    adultSeal = adultSeal.copy(numRelatives = number, isStarted = true)
                    updateNotebookEntry(adultSeal)
                }

                "pupOne" -> {
                    pupOne = pupOne.copy(numRelatives = number, isStarted = true)
                    updateNotebookEntry(pupOne)
                }

                "pupTwo" -> {
                    pupTwo = pupTwo.copy(numRelatives = number, isStarted = true)
                    updateNotebookEntry(pupTwo)
                }
            }
        }
    }

    fun updateSex(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(sex = input, isStarted = true)
                updateNotebookEntry(adultSeal)
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

    fun updateTagAlpha(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(tagAlpha = input, isStarted = true)
                updateTagId(adultSeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagAlpha = input, isStarted = true)
                updateTagId(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagAlpha = input, isStarted = true)
                updateTagId(pupTwo)
            }
        }
    }

    private fun updateTagId(seal: Seal) {
        var tagIdStr = seal.tagNumber.toString() + seal.tagAlpha
        if (seal.numTags > 3) {
            tagIdStr = seal.tagNumber.toString() + seal.tagAlpha + seal.tagAlpha
        }
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(tagId = tagIdStr, isStarted = true)
                updateNotebookEntry(adultSeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagId = tagIdStr, isStarted = true)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagId = tagIdStr, isStarted = true)
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun updateTagNumber(seal: Seal, input: Int) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(tagNumber = input, isStarted = true)
                updateTagId(adultSeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagNumber = input, isStarted = true)
                updateTagId(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagNumber = input, isStarted = true)
                updateTagId(pupTwo)
            }
        }
    }

    fun updateTagEventType(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(tagEventType = input, isStarted = true)
                updateNotebookEntry(adultSeal)
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

    fun updateComment(sealName: String, input: String) {
        when (sealName) {
            "adult" -> {
                adultSeal = adultSeal.copy(comment = input, isStarted = true)
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
        val number: Int? = input.toIntOrNull()
        if (number != null) {

            when (sealName) {
                "adult" -> {
                    adultSeal = adultSeal.copy(numTags = number, isStarted = true)
                    updateTagId(adultSeal)
                }

                "pupOne" -> {
                    pupOne = pupOne.copy(numTags = number, isStarted = true)
                    updateTagId(pupOne)
                }

                "pupTwo" -> {
                    pupTwo = pupTwo.copy(numTags = number, isStarted = true)
                    updateTagId(pupTwo)
                }
            }
        }
    }

    fun updateTissueTaken(sealName: String, input: Boolean) {
        when (sealName) {
            "adult" -> {
                adultSeal = adultSeal.copy(tissueTaken = input, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tissueTaken = input, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tissueTaken = input, isStarted = true)
            }
        }
    }

    fun updateNotebookEntry(seal: Seal) {
        val sb = StringBuilder()
        var age = if (seal.age.isNotEmpty()) {
            seal.age[0].toString()
        } else {
            ""
        }

        var sex = if (seal.sex.isNotEmpty()) {
            seal.sex[0].toString()
        } else {
            ""
        }

        val numRels = if (seal.numRelatives > 0) {
            seal.numRelatives.toString()
        } else {
            ""
        }

        var tag = seal.tagId

        var event = if (seal.tagEventType.isNotEmpty()) {
            seal.tagEventType[0]
        } else {
            ""
        }
        sb.append(age)
        sb.append(sex)
        sb.append(numRels)
        sb.append("  ")
        sb.append(tag)
        sb.append("  ")
        sb.append(event)

        var notebookEntry = sb.toString();

        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(notebookDataString = notebookEntry)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(notebookDataString = notebookEntry)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(notebookDataString = notebookEntry)
            }
        }
    }

    fun isValid(): Boolean {
//        if (!observationSaver.isEmpty() && !uiState.isSaving) {
        if (observationRepo.canAddObservation() && !uiState.isSaving) {
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
                        latLong = "Lat : $lat " + "Long : $lon"
                    )
                }
            }
    }

    // endregion
    fun createLog() {
        if (!isValid()) {
            return
        }

        uiState = uiState.copy(isSaving = true)

        viewModelScope.launch {
            val log = ObservationLogEntry(
                // passing zero, but Room entity will autopopulate the id
                id = 0,
                date = uiState.date,
                currentLocation = uiState.currentLocation,
                lastKnownLocation = uiState.lastKnownLocation
            )
            //TODO, consider a validation check to see if fields are populated before inserting to database
            observationRepo.addObservation(log)
            //saving state triggers the navigation to route to home
            uiState = uiState.copy(isSaved = true)
        }
    }

    fun dismissError() {
        TODO("Not yet implemented")
    }
}