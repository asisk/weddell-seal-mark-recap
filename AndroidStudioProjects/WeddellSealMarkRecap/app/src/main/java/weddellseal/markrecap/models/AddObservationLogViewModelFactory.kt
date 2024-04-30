package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.data.ObservationRepository

class AddLogViewModelFactory (private val application: Application, private val observationRepository: ObservationRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddObservationLogViewModel::class.java)) {
            return AddObservationLogViewModel(application, observationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}