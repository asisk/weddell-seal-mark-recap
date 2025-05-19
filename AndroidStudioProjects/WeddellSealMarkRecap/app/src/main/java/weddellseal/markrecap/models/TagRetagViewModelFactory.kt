package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository

class TagRetagViewModelFactory (
    private val application: Application,
    private val observationRepository: ObservationRepository,
    private val sealColonyRepository: SealColonyRepository): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TagRetagModel::class.java)) {
            return TagRetagModel(application, observationRepository, sealColonyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}