package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.SealColonyRepository
import weddellseal.markrecap.data.SupportingDataRepository

class AddLogViewModelFactory (
    private val application: Application,
    private val observationRepository: ObservationRepository,
    private val sealColonyRepository: SealColonyRepository): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddObservationLogViewModel::class.java)) {
            return AddObservationLogViewModel(application, observationRepository, sealColonyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}