package weddellseal.markrecap.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import weddellseal.markrecap.ObservationLogApplication
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository

class HomeViewModelFactory(
    private val observationRepository: ObservationRepository,
    private val locationSource: LocationSource,
    private val sealColonyRepository : SealColonyRepository,
    private val observersRepository: ObserversRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ObservationLogApplication
        return HomeViewModel(app, observationRepository, locationSource, sealColonyRepository, observersRepository) as T
    }
}