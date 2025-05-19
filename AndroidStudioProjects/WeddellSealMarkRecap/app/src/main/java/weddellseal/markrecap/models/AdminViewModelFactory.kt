package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.frameworks.room.files.FilesRepository

class AdminViewModelFactory(private val application: Application, private val supportingDataRepository: FilesRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            return AdminViewModel(application, supportingDataRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
