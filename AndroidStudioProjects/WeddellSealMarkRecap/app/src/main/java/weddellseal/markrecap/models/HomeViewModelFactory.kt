package weddellseal.markrecap.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import weddellseal.markrecap.ObservationLogApplication
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.SealColonyRepository
import weddellseal.markrecap.frameworks.room.SupportingDataRepository
import weddellseal.markrecap.domain.location.LocationSource

class HomeViewModelFactory(
    private val observationRepository: ObservationRepository,
    private val supportingDataRepository: SupportingDataRepository,
    private val locationSource: LocationSource,
    private val sealColonyRepository : SealColonyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ObservationLogApplication
        return HomeViewModel(app, observationRepository, supportingDataRepository, locationSource, sealColonyRepository) as T
    }
}