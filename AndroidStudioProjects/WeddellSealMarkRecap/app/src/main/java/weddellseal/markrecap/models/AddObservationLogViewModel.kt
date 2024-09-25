package weddellseal.markrecap.models

/*
* Main model that stores data entered from the observation page for up to three seals
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import weddellseal.markrecap.data.processTags
import weddellseal.markrecap.ui.utils.buildLogEntry
import weddellseal.markrecap.ui.utils.notebookEntryValueSeal
import weddellseal.markrecap.ui.utils.sealValidation
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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
        val isCensusMode: Boolean = false,
        val isPrefilled: Boolean = false,
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
        val deviceID: String,
        val observerInitials: String = "Select an option",
        val censusNumber: String = "Select an option",
        val colonyLocation: String = "Select an option",
        val validationFailureReason: String = "",
        val isValidated: Boolean = false,
        val validEntry: Boolean = false,
        val observationLogEntry: ObservationLogEntry? = null,
        val isEditMode: Boolean = false,
        val recordID: Int = 0,
    )

    var uiState by mutableStateOf(
        UiState(
            hasLocationAccess = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
            hasGooglePlay = 999,
            season = getCurrentYear().toString(),
            deviceID = getDeviceName(context),
        )
    )
        private set

    var primarySeal by mutableStateOf(
        Seal(
            name = "primary", isStarted = false
        )
    )
        private set

    var pupOne by mutableStateOf(
        Seal(
            name = "pupOne", age = "Pup", isStarted = false
        )
    )
        private set

    var pupTwo by mutableStateOf(
        Seal(
            name = "pupTwo", age = "Pup", isStarted = false
        )
    )
        private set

    private var wedCheckSealMap = mutableMapOf<String, WedCheckSeal>()

    // add a WedCheckSeal to the map
    fun addWedCheckSeal(tag: String, seal: WedCheckSeal) {
        wedCheckSealMap[tag] = seal
    }

    // retrieve a WedCheckSeal by speno
    fun getWedCheckSeal(tagID: String): WedCheckSeal? {
        return wedCheckSealMap[tagID]
    }

    private fun getCurrentYear(): Int {
        return LocalDate.now().year
    }

    private fun getCoordinatesLastUpdated(): String {
        val currentDateTime =
            ZonedDateTime.now(ZoneId.systemDefault()) // Get the current date and time with timezone
        val formatter = DateTimeFormatter.ofPattern(
            "MM.dd.yyyy HH:mm:ss a z",
            Locale.US
        ) // Define the desired format
        return currentDateTime.format(formatter) // Format the current date and time
    }

    private fun getDeviceName(context: Context): String {
        return Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: "Unknown Device"
    }

    fun clearValidationState() {
        uiState =
            uiState.copy(
                validationFailureReason = "",
                isValidated = false,
                validEntry = false
            )

        primarySeal = primarySeal.copy(
            isValid = false,
            isValidated = false,
            reasonNotValid = ""
        )

        pupOne = pupOne.copy(
            isValid = false,
            isValidated = false,
            reasonNotValid = ""
        )

        pupTwo = pupTwo.copy(
            isValid = false,
            isValidated = false,
            reasonNotValid = ""
        )
    }

    fun updateColonySelection(observationSiteSelected: String) {
        uiState = uiState.copy(colonyLocation = observationSiteSelected)
    }

    fun updateObserverInitials(initials: String) {
        uiState = uiState.copy(observerInitials = initials)
    }

    fun updateCensusNumber(censusNumber: String) {
        uiState = uiState.copy(censusNumber = censusNumber)
    }

    fun updateIsObservationMode(observationMode: Boolean) {
        uiState = uiState.copy(isCensusMode = observationMode)
    }

    fun updateObservationEntry(observation: ObservationLogEntry) {
        uiState = uiState.copy(observationLogEntry = observation)
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

    fun updateOldTagMarks(name: String, oldTagMarks: Boolean) {
        when (name) {
            "primary" -> {
                primarySeal = primarySeal.copy(oldTagMarks = oldTagMarks)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(oldTagMarks = oldTagMarks)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(oldTagMarks = oldTagMarks)
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

    fun clearOldTag(sealName: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(oldTagId = "")
            }

            "pupOne" -> {
                pupOne = pupOne.copy(oldTagId = "")
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(oldTagId = "")
            }
        }
    }

    fun clearTag(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    tagAlpha = "",
                    tagNumber = "",
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    tagAlpha = "",
                    tagNumber = "",
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    tagAlpha = "",
                    tagNumber = "",
                )
                updateNotebookEntry(pupTwo)
            }
        }
    }
//
//    fun updateWedCheckFields(seal: Seal, wedCheckViewModel: WedCheckViewModel, searchStr: String) {
//        if (!wedCheckViewModel.uiState.value.isSearching) {
//            wedCheckViewModel.findSealbyTagID(searchStr) // find the seal
//        }
//    }


    fun clearSpeNo(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(speNo = 0, hasWedCheckSpeno = false)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(speNo = 0, hasWedCheckSpeno = false)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(speNo = 0, hasWedCheckSpeno = false)
            }
        }
    }

    fun updateNoTag(sealName: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    isNoTag = true,
                    numTags = "",
                    tagAlpha = "",
                    tagNumber = "",
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    isNoTag = true,
                    numTags = "",
                    tagAlpha = "",
                    tagNumber = "",
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    isNoTag = true,
                    numTags = "",
                    tagAlpha = "",
                    tagNumber = "",
                )
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun updateRetagReason(sealName: String, input: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(reasonForRetag = input)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(reasonForRetag = input)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(reasonForRetag = input)
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

    fun updateNumRelatives(input: String) {
        val number: Int? = input.toIntOrNull()
        if (number != null) {
            when (number) {
                0 -> {
                    removePups()
                }

                1 -> {
                    pupOne = pupOne.copy(numRelatives = input, isStarted = true)
                    pupTwo = pupTwo.copy(numRelatives = input, isStarted = false)
                    updateNotebookEntry(pupOne)
                    updateNotebookEntry(pupTwo)
                }

                2 -> {
                    pupOne = pupOne.copy(numRelatives = input, isStarted = true)
                    pupTwo = pupTwo.copy(numRelatives = input, isStarted = true)
                    updateNotebookEntry(pupOne)
                    updateNotebookEntry(pupTwo)
                }
            }

            primarySeal = primarySeal.copy(numRelatives = input, isStarted = true)
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
            "primary" -> {
                if (primarySeal.age == "Pup") {
                    primarySeal = primarySeal.copy(pupPeed = true, isStarted = true)
                }
            }

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
            "primary" -> {
                primarySeal = primarySeal.copy(tagAlpha = input, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagAlpha = input, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagAlpha = input, isStarted = true)
            }
        }
    }

    fun updateTagNumber(seal: Seal, input: String) {
        var tagNumber = input
        // Function to extract numeric value
        if (input.toIntOrNull() != null) {
            tagNumber = input
        }

        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(tagNumber = tagNumber, isStarted = true)
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagNumber = tagNumber, isStarted = true)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagNumber = tagNumber, isStarted = true)
                updateNotebookEntry(pupTwo)
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

    fun updateOldTag(seal: Seal, oldTagIdOne: String) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(oldTagId = oldTagIdOne, isStarted = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(oldTagId = oldTagIdOne, isStarted = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(oldTagId = oldTagIdOne, isStarted = true)
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
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(numTags = input, isStarted = true)
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(numTags = input, isStarted = true)
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(numTags = input, isStarted = true)
                updateNotebookEntry(pupTwo)
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

    fun resetPupFields(sealName: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    pupPeed = false, weightTaken = false, weight = 0, isStarted = true
                )
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    pupPeed = false, weightTaken = false, weight = 0, isStarted = true
                )
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    pupPeed = false, weightTaken = false, weight = 0, isStarted = true
                )
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

    private fun updateNotebookEntry(seal: Seal) {
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

    fun resetSeal(sealName: String) {
        var parentNumRels = primarySeal.numRelatives
        if (primarySeal.numRelatives != "" && primarySeal.numRelatives.toIntOrNull() != null) {
            var number = parentNumRels.toInt()
            number -= 1
            parentNumRels = number.toString()
        }

        when (sealName) {
            "primary" -> {
                primarySeal = Seal(
                    name = "primary", isStarted = false
                )
                // removing the primary seal results in removing pups, if present, as well
                pupOne = Seal(
                    name = "pupOne",
                    age = "Pup",
                    isStarted = false
                )
                pupTwo = Seal(
                    name = "pupTwo",
                    age = "Pup",
                    isStarted = false
                )
            }

            "pupOne" -> {
                // update parent num rels when pup one is removed
                primarySeal = primarySeal.copy(numRelatives = parentNumRels)

                // if pupOne is removed and there's a second pup
                if (pupTwo.isStarted) {
                    // rename the second pup and update it's number of relatives
                    pupTwo =
                        pupTwo.copy(name = "pupOne", numRelatives = primarySeal.numRelatives)
                    //reassign it to pupOne
                    pupOne = pupTwo
                    //deactivate pupTwo
                    pupTwo = Seal(name = "pupTwo", age = "Pup", isStarted = false)
                } else {
                    pupOne = Seal(
                        name = "pupOne",
                        age = "Pup",
                        numRelatives = primarySeal.numRelatives,
                        isStarted = false
                    )
                }
            }

            "pupTwo" -> {
                pupTwo = Seal(
                    name = "pupTwo", age = "Pup", isStarted = false
                )

                if (pupOne.isStarted) {
                    pupOne = pupOne.copy(numRelatives = parentNumRels)
                }
            }
        }
        updateNotebookEntry(primarySeal)
        updateNotebookEntry(pupOne)
        updateNotebookEntry(pupTwo)
    }

    private fun removePups() {
        //called when primary seal number of relatives is set to zero
        pupOne = Seal(
            name = "pupOne", age = "Pup", isStarted = false
        )
        pupTwo = Seal(
            name = "pupTwo", age = "Pup", isStarted = false
        )
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionChange(permission: String, isGranted: Boolean) {
        when (permission) {/*            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            uiState = uiState.copy(hasLocationAccess = isGranted)
                        }*/
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                uiState = uiState.copy(hasLocationAccess = isGranted)
            }/*            Manifest.permission.CAMERA -> {
                            uiState = uiState.copy(hasCameraAccess = isGranted)
                        }*/
            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen
    // prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun populateSeal(wedCheckSeal: WedCheckSeal) {
        // advance the age based on the last seen season
        val currentYear = getCurrentYear()
        var sealAgeAdvanced = "Adult"
        when (wedCheckSeal.lastSeenSeason) {
            currentYear -> { // seal last seen this year
                // Age class CANNOT change for seals seen twice in a season
                sealAgeAdvanced = wedCheckSeal.age
            }

            currentYear - 1 -> { // seal last seen last year
                // Age class must advance for seals seen last year
                val expectedAge = when (wedCheckSeal.age) {
                    "Pup" -> "Yearling"
                    "Yearling" -> "Adult"
                    else -> "Adult"
                }

                sealAgeAdvanced = expectedAge
            }

            currentYear - 2 -> { // seal last seen 2 or more years ago
                // Age class must be Adult if seal was observed two or more years ago
                sealAgeAdvanced = "Adult"
            }
        }

        // number of Relatives shouldn't be populated for Female seals because it's likely that the seal has a pup
        val numberRels = if (wedCheckSeal.sex == "Female") "" else "0"

        primarySeal = primarySeal.copy(
            speNo = wedCheckSeal.speNo,
            age = sealAgeAdvanced, // expecting to advance the seal age based on the last season seen
            sex = wedCheckSeal.sex,
            numRelatives = numberRels,
            tagNumber = wedCheckSeal.tagOneNumber,
            tagAlpha = wedCheckSeal.tagOneAlpha,
            oldTagId = wedCheckSeal.tagIdOne,
            tagEventType = "Marked",
            lastPhysio = wedCheckSeal.lastPhysio,
            colony = wedCheckSeal.colony,
            isWedCheck = true,
        )
        val tagID = wedCheckSeal.tagOneNumber + wedCheckSeal.tagOneAlpha
        addWedCheckSeal(tagID, wedCheckSeal)
        updateNotebookEntry(primarySeal)
    }

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen
    // prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun populateSealFromObservation(log: ObservationLogEntry?) {
        // Create a String array for the data
        if (log != null) {
            var ageString = when (log.ageClass) {
                "A" -> "Adult"
                "P" -> "Pup"
                "Y" -> "Yearling"
                else -> ""
            }

            val sealSex = when (log.sex) {
                "F" -> "Female"
                "M" -> "Male"
                "U" -> "Unknown"
                else -> ""
            }

            val tagEvent = when (log.tagEvent) {
                "M" -> "Marked"
                "N" -> "New"
                "R2" -> "Retag"
                else -> ""
            }

            val condition = when (log.sealCondition) {
                "0" -> "Dead - 0"
                "1" -> "Poor - 1"
                "2" -> "Fair - 2"
                "3" -> "Good - 3"
                "4" -> "Newborn - 4"
                else -> "Select an option"
            }

            var processedTagOneNumber = ""
            var processedTagOneAlpha = ""
            var numTags = 0

            if (log.tagIDOne != "NoTag") {
                val processedTagOne = processTags(log.tagIDOne)
                if (processedTagOne.tagValid) {
                    numTags++
                    processedTagOneNumber = processedTagOne.tagNumber
                    processedTagOneAlpha = processedTagOne.tagAlpha
                }

                val processedTagTwo = processTags(log.tagIDTwo)
                if (processedTagTwo.tagValid) {
                    numTags++
                }
            }

            primarySeal = primarySeal.copy(
                colony = log.colony,
                speNo = log.speno.toInt(),
                age = ageString, // expecting to advance the seal age based on the last season seen
                sex = sealSex,
                numRelatives = log.numRelatives,
                condition = condition,
                tagNumber = processedTagOneNumber,
                tagAlpha = processedTagOneAlpha,
                oldTagId = log.oldTagIDOne,
                tagEventType = tagEvent,
                reasonForRetag = log.retagReason,
                numTags = if (numTags > 0) numTags.toString() else "",
                isNoTag = log.tagIDOne == "NoTag" && log.tagEvent == "Marked",
                comment = log.comments,
                weightTaken = log.weight != "",
                weight = if (log.weight != "") log.weight.toInt() else 0,
                tissueTaken = log.tissueSampled != "",
                flaggedForReview = log.flaggedEntry != "",
                isStarted = true,
                hasWedCheckSpeno = true,
                observationID = log.id,
                isObservationLogEntry = true,
            )

            uiState = uiState.copy(
                deviceID = log.deviceID,
                season = log.season,
                observerInitials = log.observerInitials,
                censusNumber = log.censusID,
                isCensusMode = false,
                colonyLocation = log.colony
            )
        }
        updateNotebookEntry(primarySeal)
    }


    fun mapSpeno(name: String, wedCheckSeal: WedCheckSeal) {
        when (name) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    speNo = wedCheckSeal.speNo,
                    hasWedCheckSpeno = true,
                )
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    speNo = wedCheckSeal.speNo,
                    hasWedCheckSpeno = true,
                )
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    speNo = wedCheckSeal.speNo,
                    hasWedCheckSpeno = true,
                )
            }
        }
    }


    // region Location management
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val isGooglePlay =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        uiState.hasGooglePlay = isGooglePlay

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            }).addOnSuccessListener { currentLocation: Location? ->
            if (currentLocation == null) {
                val errorMessage = "Cannot get current location"
                uiState = uiState.copy(currentLocation = errorMessage)
            } else {
                val lat = currentLocation.latitude
                val lon = currentLocation.longitude
                val date = getCoordinatesLastUpdated()

                uiState = uiState.copy(
                    currentLocation = "Lat : ${lat}    " + "Long : ${lon}\n" + "Updated: $date"
                )
                uiState = uiState.copy(
                    latLong = "Lat : $lat Long : $lon"
                )
                uiState = uiState.copy(latitude = lat.toString(), longitude = lon.toString())
            }
        }
    }

    private fun getRelativesTags(sealName: String): Pair<String, String> {
        var relOneTagId = ""
        var relTwoTagId = ""
        when (sealName) {
            "primary" -> {
                relOneTagId = pupOne.tagNumber + pupOne.tagAlpha
                relTwoTagId = pupTwo.tagNumber + pupTwo.tagAlpha
            }

            "pupOne" -> {
                relOneTagId = primarySeal.tagNumber + primarySeal.tagAlpha
                relTwoTagId = pupTwo.tagNumber + pupTwo.tagAlpha
            }

            "pupTwo" -> {
                relOneTagId = primarySeal.tagNumber + primarySeal.tagAlpha
                relTwoTagId = pupOne.tagNumber + pupOne.tagAlpha
            }
        }
        return Pair(relOneTagId, relTwoTagId)
    }

    // called after navigation command from the summary screen to prevent the summary screen from
    // preemptively navigating back to the observation screen
    fun resetSaved() {
        // reset the values in the model once the records are save successfully
        wedCheckSealMap = mutableMapOf()

        uiState = uiState.copy(
            validationFailureReason = "",
            isValidated = false,
            validEntry = false,
            isSaved = false,
            isSaving = false,
            isError = false,
            errorMessage = "",
            isPrefilled = false,
        )

        primarySeal = Seal(
            name = "primary", isStarted = false
        )
        pupOne = Seal(
            name = "pupOne", age = "Pup", isStarted = false
        )
        pupTwo = Seal(
            name = "pupTwo", age = "Pup", isStarted = false
        )
    }

    fun createLog() {
        val sealsList = listOf(primarySeal, pupOne, pupTwo)
        uiState = uiState.copy(isSaving = true, isSaved = false)

        for (seal in sealsList) {
            if (seal.isStarted) {
                // get the tags for this seal's relatives
                val (relOneTag, relTwoTag) = getRelativesTags(seal.name)
                val log = buildLogEntry(uiState, seal, relOneTag, relTwoTag)

                //write an entry to the database for each seal that has valid input
                viewModelScope.launch {
                    observationRepo.addObservation(log)
                }

                //TODO, consider overwriting the database entry, instead of appending a new entry
//                if (primarySeal.observationID != 0) {
//
//                }

                uiState = uiState.copy(isSaved = true)
            }
        }
        uiState = uiState.copy(isSaving = false)
    }

    // validate is called when the observer saves an entry
    // the model state is used to prompt the user to review and confirm their entry if validation fails
    // validation errors are saved to the seal state for inclusion in the output file
    fun validate(seal: Seal) {
        // reset the model state values that help determine whether the seal entry is valid
        uiState =
            uiState.copy(validationFailureReason = "", isValidated = false, validEntry = false)

        // set the name used in the validation string to match the UI display
        var sealName = seal.name
        if (seal.name == "primary") {
            sealName = "Seal"
        }

        // if the event type is marked or retag, the seal must have a speNo(matching WedCheck record)
        var tagID = seal.tagNumber + seal.tagAlpha
        if (seal.tagEventType == "Retag") { // if retag set the tag id to the old tag
            tagID = seal.oldTagId
        }
        val wedCheckSeal =
            getWedCheckSeal(tagID) // attempt to locate the WedCheck seal in the map stored in the model

        // use the utility method to validate the seal
        val (sealValid, validationErrors) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        // update the model state with the validation state
        if (sealValid) {
            uiState = uiState.copy(isValidated = true, validEntry = true)
        } else {
            val invalidEntrySB = StringBuilder()
            invalidEntrySB.append("$sealName failed validation!")
            invalidEntrySB.append(uiState.validationFailureReason)
            invalidEntrySB.append("\n")
            invalidEntrySB.append(validationErrors)
            uiState = uiState.copy(
                validationFailureReason = invalidEntrySB.toString(),
                isValidated = true
            )
        }

        // update the seal with the validation state
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    isValid = sealValid,
                    isValidated = true,
                    reasonNotValid = validationErrors
                )
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    isValid = sealValid,
                    isValidated = true,
                    reasonNotValid = validationErrors
                )
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    isValid = sealValid,
                    isValidated = true,
                    reasonNotValid = validationErrors
                )
            }
        }
    }

    fun flagSealForReview(name: String) {
        when (name) {
            "primary" -> {
                primarySeal = primarySeal.copy(flaggedForReview = true)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(flaggedForReview = true)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(flaggedForReview = true)
            }
        }
    }

    fun prefillSingleMale() {
        primarySeal = primarySeal.copy(
            age = "Adult",
            sex = "Male",
            numRelatives = "0",
            isStarted = true
        )
        uiState = uiState.copy(isPrefilled = true)
    }

    fun prefillSingleFemale() {
        primarySeal = primarySeal.copy(
            age = "Adult",
            sex = "Female",
            numRelatives = "0",
            isStarted = true
        )
        uiState = uiState.copy(isPrefilled = true)
    }

    fun prefillMomAndPup() {
        primarySeal = primarySeal.copy(
            age = "Adult",
            sex = "Female",
            numRelatives = "1",
            isStarted = true
        )
        pupOne = pupOne.copy(numRelatives = "1", isStarted = true)
        uiState = uiState.copy(isPrefilled = true)
    }

    fun clearCensus() {
        uiState = uiState.copy(censusNumber = "Select an option")
    }
}