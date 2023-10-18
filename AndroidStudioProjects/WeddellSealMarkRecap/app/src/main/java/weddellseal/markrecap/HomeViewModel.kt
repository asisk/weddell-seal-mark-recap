package weddellseal.markrecap

/*
 * Primary model that acts as a communication center between the
 * ObservationSaverRepository (data) and the UI.
 * The UI no longer needs to worry about the origin of the data.
 * ViewModel instances survive Activity/Fragment recreation.
 */

import android.app.Application
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.launch


class HomeViewModel(
    application: Application,
    private val observationSaver: ObservationSaverRepository,
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()
    private lateinit var createDoc : ActivityResultLauncher<String>

    data class UiState(
        val loading: Boolean = true,
        val observationLogs: List<ObservationLog> = emptyList(),
        var uriForCSVWrite : String = ""
    )

    var uiState by mutableStateOf(UiState())
        private set
//    suspend fun getCameraProvider(): ActivityResultRegistry {
//        return suspendCoroutine { continuation ->
//            ActivityResultRegistryOwner .getInstance(context).apply {
//                addListener({ continuation.resume(get()) }, cameraExecutor)
//            }
//        }
//    }

    fun formatDateTime(timeInMillis: Long): String {
        return DateUtils.formatDateTime(context, timeInMillis, DateUtils.FORMAT_ABBREV_ALL)
    }

    fun exportLogs() {
        viewModelScope.launch {

            val savedFile = observationSaver.saveObservations()
            uiState = uiState.copy(
                loading = false,
            )
        }
    }

    fun delete(observationLog: ObservationLog) {
       // viewModelScope.launch {
       //     db.logDao().delete(observationLog.toLogEntry())
       //     loadLogs()
       // }
    }

        private fun createFileIntent(): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "invoice.pdf")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
//            putExtra(DocumentsContract.EXTRA_INITIAL_URI, fileUriForCSV)
        }

        return intent
    }

}

class HomeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ObservationLogApplication
        return HomeViewModel(app, app.observationSaver) as T
    }
}
//
//class CSVLogWriter(private val registry : ActivityResultRegistry) : DefaultLifecycleObserver {
//
//    var fileUriForCSV : Uri? = null
//    private lateinit var createDoc : ActivityResultLauncher<String>
////    override fun onStart(owner: LifecycleOwner) {
////        super.onStart(owner)
//        println("onStart: $owner")
//        val savedFile = photoSaver.generatePhotoCacheFile()
//
//        var createDoc = registry.register("key", owner, ActivityResultContracts.CreateDocument("csv")) { uri : Uri? ->
//            uri?.let {
//                fileUriForCSV = uri
//            }
//        }
//    }
//
//    //ask the user to pick the place to create the document
//    fun writeLogs() {
//        createDoc.launch("observations.csv")
//    }
//
//    private fun createFileIntent(): Intent {
//        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "application/pdf"
//            putExtra(Intent.EXTRA_TITLE, "invoice.pdf")
//
//            // Optionally, specify a URI for the directory that should be opened in
//            // the system file picker before your app creates the document.
//            putExtra(DocumentsContract.EXTRA_INITIAL_URI, fileUriForCSV)
//        }
//
//        return intent
//    }
//}
