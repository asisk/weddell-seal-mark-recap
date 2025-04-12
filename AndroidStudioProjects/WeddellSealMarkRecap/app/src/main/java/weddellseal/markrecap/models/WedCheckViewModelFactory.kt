package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.frameworks.room.SupportingDataRepository
import weddellseal.markrecap.frameworks.room.WedCheckRepository

class WedCheckViewModelFactory(private val application: Application, private val wedCheckRepository: WedCheckRepository, private val supportingDataRepository: SupportingDataRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WedCheckViewModel::class.java)) {
            return WedCheckViewModel(application, wedCheckRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
