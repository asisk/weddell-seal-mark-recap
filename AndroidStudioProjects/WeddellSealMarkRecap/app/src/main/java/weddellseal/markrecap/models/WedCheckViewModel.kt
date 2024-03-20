package weddellseal.markrecap.models

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import weddellseal.markrecap.ObservationLogApplication
import weddellseal.markrecap.data.WedCheckRecord
import weddellseal.markrecap.data.WedCheckRepository

class WedCheckViewModel (application: Application,
    private val wedCheckRepo: WedCheckRepository
) : AndroidViewModel(application) {

    //TODO, data load from csv to SQLite database

    data class UiState(
        val loading: Boolean = true,
        val uriForCSVDataLoad: Uri? = null,
        var sealRecord: WedCheckRecord? = null,
        )

    var uiState by mutableStateOf(
        UiState()
    )
        private set

    fun findSeal(speno : Int) {
        var seal: WedCheckRecord? = wedCheckRepo.findSeal(speno)
        if (seal != null) {
            println("Seal found")
        }
        uiState = uiState.copy(
            sealRecord = seal
        )
    }
}
class WedCheckViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ObservationLogApplication
        return WedCheckViewModel(app, app.wedCheckRepo) as T
    }
}