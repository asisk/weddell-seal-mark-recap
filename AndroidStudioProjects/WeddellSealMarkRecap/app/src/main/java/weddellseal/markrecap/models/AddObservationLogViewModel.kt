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
import weddellseal.markrecap.ui.utils.buildLogEntry
import weddellseal.markrecap.ui.utils.notebookEntryValueSeal
import weddellseal.markrecap.ui.utils.sealValidation
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
        val validationFailureReason: String = "",
        val isValidated: Boolean = false,
        val validEntry: Boolean = false,
    )

    var uiState by mutableStateOf(
        UiState(
            hasLocationAccess = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
            hasGooglePlay = 999,
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z", Locale.US
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

    private var wedCheckSealMap = mutableMapOf<Int, WedCheckSeal>()

    // add a WedCheckSeal to the map
    private fun addWedCheckSeal(seal: WedCheckSeal) {
        wedCheckSealMap[seal.speNo] = seal
    }

    //  retrieve a WedCheckSeal by speno
    private fun getWedCheckSeal(speNo: Int): WedCheckSeal? {
        return wedCheckSealMap[speNo]
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

    // validate is called when the observer attempts to save an entry
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
        val wedCheckSeal =
            getWedCheckSeal(seal.speNo) // attempt to locate the WedCheck seal in the map stored in the model

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
            errorMessage = ""
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

    fun updateColonySelection(observationSiteSelected: String) {
        uiState = uiState.copy(colonyLocation = observationSiteSelected)
    }

    fun updateObserverInitials(initials: String) {
        uiState = uiState.copy(observerInitials = initials)
    }

    fun updateCensusNumber(censusNumber: String) {
        uiState = uiState.copy(censusNumber = censusNumber)
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

    fun clearOldTags(sealName: String) {
        when (sealName) {
            "primary" -> {
                primarySeal = primarySeal.copy(oldTagIdOne = "", oldTagIdTwo = "")
            }

            "pupOne" -> {
                pupOne = pupOne.copy(oldTagIdOne = "", oldTagIdTwo = "")
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(oldTagIdOne = "", oldTagIdTwo = "")
            }
        }
    }

    fun clearTagOne(seal: Seal) {
        updateTagOneNumber(seal, "")
        updateTagOneAlpha(seal, "")
        updateTagIdOne(seal)

        updateNotebookEntry(seal)
    }

    fun clearTagTwo(seal: Seal) {
        updateTagTwoNumber(seal, "")
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
                    tagOneNumber = "",
                    tagTwoNumber = ""
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = "",
                    tagTwoNumber = ""
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    tagIdOne = "",
                    tagIdTwo = "",
                    tagOneAlpha = "",
                    tagTwoAlpha = "",
                    tagOneNumber = "",
                    tagTwoNumber = ""
                )
                updateNotebookEntry(pupTwo)
            }
        }
    }

    fun revertTagID(sealName: String, tagId: String) {
        when (sealName) {
            "primary" -> {
                primarySeal =
                    primarySeal.copy(tagIdOne = tagId, oldTagIdOne = "", oldTagIdTwo = "")
            }

            "pupOne" -> {
                pupOne = pupOne.copy(tagIdOne = tagId, oldTagIdOne = "", oldTagIdTwo = "")
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(tagIdOne = tagId, oldTagIdOne = "", oldTagIdTwo = "")
            }
        }
    }

    fun clearSpeNo(seal: Seal) {
        when (seal.name) {
            "primary" -> {
                primarySeal = primarySeal.copy(speNo = 0)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(speNo = 0)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(speNo = 0)
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
                    tagOneNumber = "",
                    tagTwoNumber = "",
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
                    tagOneNumber = "",
                    tagTwoNumber = "",
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
                    tagOneNumber = "",
                    tagTwoNumber = "",
                    tagEventType = ""
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

    fun updateNumRelatives(seal: Seal, input: String) {
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
        if (seal.tagOneNumber.isNotEmpty()) {
            tagOneNumStr = seal.tagOneNumber
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
        var tagIdStr = seal.tagOneNumber + seal.tagOneAlpha
        val number: Int? = seal.numTags.toIntOrNull()

        if (number != null && number > 1) {
            tagIdStr = seal.tagOneNumber + seal.tagOneAlpha + seal.tagOneAlpha
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


    fun updateTagOneNumber(seal: Seal, input: String) {
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

    fun updateTagTwoNumber(seal: Seal, input: String) {
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
                    oldTagIdOne = seal.tagIdOne, oldTagIdTwo = seal.tagIdTwo, isStarted = true
                )
                updateNotebookEntry(primarySeal)
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    oldTagIdOne = seal.tagIdOne, oldTagIdTwo = seal.tagIdTwo, isStarted = true
                )
                updateNotebookEntry(pupOne)
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    oldTagIdOne = seal.tagIdOne, oldTagIdTwo = seal.tagIdTwo, isStarted = true
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
        val isTwoTags = input != "" && input.toInt() == 2
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

    // used to pull over the fields from the WedCheckRecord upon Seal Lookup Screen
    // prepopulated fields: age, sex, #rels, tag event=marked per August 1 discussion
    fun populateSeal(wedCheckSeal: WedCheckSeal) {
        primarySeal = primarySeal.copy(
            speNo = wedCheckSeal.speNo,
            age = wedCheckSeal.age,
            sex = wedCheckSeal.sex,
            numRelatives = wedCheckSeal.numRelatives.toString(),
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

    fun mapWedCheckFields(name: String, wedCheckSeal: WedCheckSeal) {
        addWedCheckSeal(wedCheckSeal)

        when (name) {
            "primary" -> {
                primarySeal = primarySeal.copy(
                    speNo = wedCheckSeal.speNo,
                    hasWedCheckSpeno = true,
                    oldTagIdOne = wedCheckSeal.tagIdOne,
                    oldTagIdTwo = wedCheckSeal.tagIdTwo,
                )
            }

            "pupOne" -> {
                pupOne = pupOne.copy(
                    speNo = wedCheckSeal.speNo,
                    hasWedCheckSpeno = true,
                    oldTagIdOne = wedCheckSeal.tagIdOne,
                    oldTagIdTwo = wedCheckSeal.tagIdTwo,
                )
            }

            "pupTwo" -> {
                pupTwo = pupTwo.copy(
                    speNo = wedCheckSeal.speNo,
                    hasWedCheckSpeno = true,
                    oldTagIdOne = wedCheckSeal.tagIdOne,
                    oldTagIdTwo = wedCheckSeal.tagIdTwo,
                )
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

    fun removePups() {
        //called when primary seal number of relatives is set to zero
        pupOne = Seal(
            name = "pupOne", age = "Pup", isStarted = false
        )
        pupTwo = Seal(
            name = "pupTwo", age = "Pup", isStarted = false
        )
    }

    fun isValid(): Boolean {
        return !uiState.isSaving
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
                geocoder.getFromLocation(
                    location.latitude, location.longitude, 1
                ) { addresses ->
                    val address = addresses.firstOrNull()
                    val place = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                    ?: address?.countryName
                }
            } else {
                val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull() ?: return@addOnSuccessListener
                val place = address.locality ?: address.subAdminArea ?: address.adminArea
                ?: address.countryName ?: return@addOnSuccessListener
            }
        }
    }

    // region Location management
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
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
                val date = SimpleDateFormat("MM.dd.yyyy HH:mm:ss aaa z", Locale.US).format(
                    System.currentTimeMillis()
                )
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

    fun createLog(vararg seals: Seal) {
        uiState = uiState.copy(isSaving = true, isSaved = false)
        for (seal in seals) {
            if (seal.isStarted) {
                val log = buildLogEntry(uiState, seal, primarySeal, pupOne, pupTwo)

                //write an entry to the database for each seal that has valid input
                viewModelScope.launch {
                    observationRepo.addObservation(log)
                }

                uiState = uiState.copy(isSaved = true)
            }
        }
        uiState = uiState.copy(isSaving = false)
    }
}