package weddellseal.markrecap

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AddObservationLogViewModel(
    application: Application,
    private val observationSaver: ObservationSaverRepository
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()
//    val observationDao = AppDatabase.getDatabase(application).observationDao()

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    // endregion

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
        val latLong: String = "gps data empty",
    )

    data class Seal(
        val name: String = "",
        val tagNumber: Int = 0,
        val tagAlpha: String = "",
        val tagId: String = "",
        val condition: String = "",
        val age: String = "",
        val sex: String = "",
        val numTags: Int = 0,
        val numRelatives: Int = 0,
        val tagEventType: String = "",
        val comment: String = ""
    )

    var adultSeal by mutableStateOf(Seal(name = "adult"))
        private set
    var pupOne by mutableStateOf(Seal(name = "pupOne"))
        private set
    var pupTwo by mutableStateOf(Seal(name = "pupTwo"))
        private set

    fun updateCondition(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(condition = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(condition = input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(condition = input)
            }
        }
    }

    private fun updateTagId(seal: Seal) {
        when (seal.name) {
            "adult" -> {
                adultSeal =
                    adultSeal.copy(tagId = adultSeal.tagNumber.toString() + adultSeal.tagAlpha)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagId = pupOne.tagNumber.toString() + pupOne.tagAlpha)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagId = pupTwo.tagNumber.toString() + pupTwo.tagAlpha)
            }
        }
    }

    fun clearTag(seal: Seal) {
        // set tag back to original values
        // val tagNumber: Int = 0,
        // val tagAlpha : String = "",
        // val tagId: String ="",
        updateTagNumber(seal, 0)
        updateTagAlpha(seal, "")
        updateTagId(seal)
    }

    fun updateTagAlpha(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(tagAlpha = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagAlpha = input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagAlpha = input)
            }
        }
    }

    fun updateTagNumber(seal: Seal, input: Int) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(tagNumber = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagNumber = input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagNumber = input)
            }
        }
        updateTagId(seal)
    }

    fun appendAlphaToTagID(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal =
                    adultSeal.copy(tagAlpha = input, tagId = adultSeal.tagNumber.toString() + input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagAlpha = input, tagId = pupOne.tagNumber.toString() + input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagAlpha = input, tagId = pupTwo.tagNumber.toString() + input)
            }
        }
    }

    fun updateAge(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(age = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(age = input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(age = input)
            }
        }
    }

    fun updateSex(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(sex = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(sex = input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(sex = input)
            }
        }
    }

    fun updateTagEventType(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(tagEventType = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagEventType = input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagEventType = input)
            }
        }
    }

    fun updateNumRelatives(seal: Seal, input: String) {
        val number: Int? = input.toIntOrNull()
        if (number != null) {
            when (seal.name) {
                "adult" -> {
                    adultSeal = adultSeal.copy(numRelatives = number)
                }

                "pupOne" -> {
                    pupOne = pupOne.copy(numRelatives = number)
                }

                "pupTwo" -> {
                    pupTwo = pupTwo.copy(numRelatives = number)
                }
            }
        }
    }

    fun updateComment(seal: Seal, input: String) {
        when (seal.name) {
            "adult" -> {
                adultSeal = adultSeal.copy(comment = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(comment = input)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(comment = input)
            }
        }
    }

    fun updateNumTags(seal: Seal, input: String) {
        val number: Int? = input.toIntOrNull()
        if (number != null) {

            when (seal.name) {
                "adult" -> {
                    adultSeal = adultSeal.copy(numTags = number)
                }

                "pupOne" -> {
                    pupOne = pupOne.copy(numTags = number)
                }

                "pupTwo" -> {
                    pupTwo = pupTwo.copy(numTags = number)
                }
            }
        }
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
        )
    )
        private set

    fun isValid(): Boolean {
//        if (!observationSaver.isEmpty() && !uiState.isSaving) {
        if (observationSaver.canAddObservation() && !uiState.isSaving) {
            return true
        }
        return false
//        return !observationSaver.isEmpty() && !uiState.isSaving
    }

//    private fun getIsoDate(timeInMillis: Long): String {
//        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(timeInMillis)
//    }

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
                                "lat : ${lat}\n" +
                                "long : ${lon}\n" +
                                "updated: $date"
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
            observationSaver.addObservation(log)
            //saving state triggers the navigation to route to home
            uiState = uiState.copy(isSaved = true)
        }
    }

    // endregion
}

class AddLogViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[APPLICATION_KEY] as ObservationLogApplication
        return AddObservationLogViewModel(app, app.observationSaver) as T
    }
}