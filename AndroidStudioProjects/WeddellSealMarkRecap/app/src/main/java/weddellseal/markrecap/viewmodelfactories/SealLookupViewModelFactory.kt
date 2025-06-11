package weddellseal.markrecap.viewmodelfactories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.lookup.SealLookupViewModel

class SealLookupViewModelFactory(private val application: Application, private val wedCheckRepository: WedCheckRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SealLookupViewModel::class.java)) {
            return SealLookupViewModel(application, wedCheckRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
