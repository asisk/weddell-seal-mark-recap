package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.frameworks.room.files.FilesRepository
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository

class ObserversViewModelFactory(
    private val application: Application,
    private val observersRepository: ObserversRepository,
    private val filesRepository: FilesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ObserversViewModel::class.java)) {
            return ObserversViewModel(application, observersRepository, filesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
