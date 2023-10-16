package weddellseal.markrecap

/*
 * Primary model that acts as a communication center between the
 * ObservationSaverRepository (data) and the UI.
 * The UI no longer needs to worry about the origin of the data.
 * ViewModel instances survive Activity/Fragment recreation.
 */

import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class HomeViewModel(
    application: Application,
    private val observationSaver: ObservationSaverRepository
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    // set up a new instance of the observations database
    //private val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()

    data class UiState(val loading: Boolean = true, val observationLogs: List<ObservationLog> = emptyList())

    var uiState by mutableStateOf(UiState())
        private set

    fun formatDateTime(timeInMillis: Long): String {
        return DateUtils.formatDateTime(context, timeInMillis, DateUtils.FORMAT_ABBREV_ALL)
    }
/*
    fun loadLogs() {
        viewModelScope.launch {
            uiState = uiState.copy(
                loading = false,
                observationLogs = db.logDao().getAllWithFiles(photoSaver.photoFolder)
            )
        }
    }
*/
    fun delete(observationLog: ObservationLog) {
       // viewModelScope.launch {
       //     db.logDao().delete(observationLog.toLogEntry())
       //     loadLogs()
       // }
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
