package weddellseal.markrecap.models

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.Observers
import weddellseal.markrecap.data.SealColony
import weddellseal.markrecap.data.SupportingDataRepository
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

/*
 * Home Screen model
 */
class HomeViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
    private val supportingDataRepository: SupportingDataRepository
) : AndroidViewModel(application) {
    data class UiState(
        val hasFileAccess: Boolean,
        val loading: Boolean = true,
        val isSaving: Boolean = false,
        var uriForCSVWrite: Uri? = null,
        var colonyLocations: List<String> = emptyList(),
        var observerInitials: List<String> = emptyList(),
        val date: String,
    )

    private val context: Context
        get() = getApplication()
    var uiState by mutableStateOf(
        UiState(
            hasFileAccess = hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z",
                Locale.US
            ).format(System.currentTimeMillis()),
        )
    )
        private set

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val colonyLocations =
                withContext(Dispatchers.IO) { supportingDataRepository.getLocations() }
            val observerInitials =
                withContext(Dispatchers.IO) { supportingDataRepository.getObserverInitials() }
            uiState = uiState.copy(
                colonyLocations = colonyLocations,
                observerInitials = observerInitials
            )
        }
    }

    data class StudyArea(
        val location: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
    )

    var studyAreaState by mutableStateOf(StudyArea(location = "Default A"))
        private set

    fun isValid(): Boolean {
//        if (!observationSaver.isEmpty() && !uiState.isSaving) {
        if (observationRepo.canWriteStudyAreas() && !uiState.isSaving) {
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
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                uiState = uiState.copy(hasFileAccess = isGranted)
            }

            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }

    private fun readSealColoniesCsvData(
        contentResolver: ContentResolver,
        uri: Uri
    ): List<SealColony> {
        val sealColonies: MutableList<SealColony> = mutableListOf()
        val dropdownList = mutableListOf<String>()
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream).buffered().use { reader ->
                    val headerRow = reader.readLine()?.split(",") ?: emptyList()
                    val inOutIndex = headerRow.indexOf("InOut")
                    val locationIndex = headerRow.indexOf("Location")
                    val nLimitIndex = headerRow.indexOf("nLimit")
                    val sLimitIndex = headerRow.indexOf("sLimit")
                    val wLimitIndex = headerRow.indexOf("wLimit")
                    val eLimitIndex = headerRow.indexOf("eLimit")
                    val adjLatIndex = headerRow.indexOf("Adj_Lat")
                    val adjLongIndex = headerRow.indexOf("Adj_Long")

                    if (listOf(
                            inOutIndex,
                            locationIndex,
                            nLimitIndex,
                            sLimitIndex,
                            wLimitIndex,
                            eLimitIndex,
                            adjLatIndex,
                            adjLongIndex
                        ).all { it != -1 }
                    ) {
                        reader.forEachLine { line ->
                            val row = line.split(",")
                            val inOut = row.getOrNull(inOutIndex) ?: ""

                            val location = row.getOrNull(locationIndex) ?: ""
                            dropdownList.add(location)

                            val nLimit = row.getOrNull(nLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val sLimit = row.getOrNull(sLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val wLimit = row.getOrNull(wLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val eLimit = row.getOrNull(eLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val adjLat = row.getOrNull(adjLatIndex)?.toDoubleOrNull() ?: 0.0
                            val adjLong = row.getOrNull(adjLongIndex)?.toDoubleOrNull() ?: 0.0

                            val record = SealColony(
                                colonyId = 0, // Room will autopopulate, pass zero only to satisfy the instantiation of the WedCheckRecord
                                inOut = inOut,
                                location = location,
                                nLimit = nLimit,
                                sLimit = sLimit,
                                wLimit = wLimit,
                                eLimit = eLimit,
                                adjLat = adjLat,
                                adjLong = adjLong
                            )

                            // Add the parsed entity to the list
                            sealColonies.add(record)
                        }
                        uiState = uiState.copy(colonyLocations = dropdownList)
                    } else {
                        // Handle the case where one or more headers are missing
                        // This could be logging an error, showing a message to the user, etc.
                        throw IllegalArgumentException("CSV file missing required headers")
                    }
                }
            } ?: throw IOException("Unable to open input stream")
        } catch (e: IOException) {
            e.printStackTrace()
            //TODO,  Handle IO exception
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            //TODO
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            //TODO, Handle number format exception (if field3 cannot be parsed as an Int)
        } catch (e: Exception) {
            // Handle any other exceptions
            e.printStackTrace()
            // TODO, Show an error message to the user, log the error, etc.
        }

        return sealColonies
    }

    fun loadSealColoniesFile(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            // Read CSV data on IO dispatcher
            val csvData = withContext(Dispatchers.IO) {
                readSealColoniesCsvData(context.contentResolver, uri)
            }

            // Insert CSV data into the database
            supportingDataRepository.insertColoniesData(csvData)
        }
    }

    fun loadObserversFile(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            // Read CSV data on IO dispatcher
            val csvData = withContext(Dispatchers.IO) {
                readObserverCsvData(context.contentResolver, uri)
            }

            // Insert CSV data into the database
            supportingDataRepository.insertObserversData(csvData)
        }
    }


    private fun readObserverCsvData(contentResolver: ContentResolver, uri: Uri): List<Observers> {
        val observers: MutableList<Observers> = mutableListOf()
        val dropdownList = mutableListOf<String>()
        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->
                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val initialsIndex = headerRow.indexOf("ObserverInitials")

                reader.forEachLine { line ->
                    val row = line.split(",")
                    if (row.isNotEmpty() && initialsIndex != -1) {
                        val observer = row.getOrNull(initialsIndex) ?: ""
                        dropdownList.add(observer.toString())

                        val record = Observers(
                            observerId = 0,
                            initials = observer
                        )

                        observers.add(record)
                    } else {
                        // Handle invalid row or missing columns
                    }
                }
                uiState = uiState.copy(observerInitials = dropdownList)
            }
        }

        return observers
    }
}