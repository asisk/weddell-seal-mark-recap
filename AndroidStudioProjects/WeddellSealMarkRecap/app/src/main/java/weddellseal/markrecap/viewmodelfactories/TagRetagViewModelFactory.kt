package weddellseal.markrecap.viewmodelfactories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel

class TagRetagViewModelFactory(
    private val application: Application,
    private val observationRepository: ObservationRepository,
    private val wedCheckRepo: WedCheckRepository,
    private val uiState: StateFlow<HomeViewModel.UiState>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TagRetagViewModel::class.java)) {
            return TagRetagViewModel(application, observationRepository, wedCheckRepo, uiState) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}