package weddellseal.markrecap.viewmodelfactories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import weddellseal.markrecap.ObservationLogApplication
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

class RecentObservationsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ObservationLogApplication
        return RecentObservationsViewModel(app, app.observationRepo) as T
    }
}