package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import weddellseal.markrecap.frameworks.room.files.FilesRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository

class SealColoniesViewModelFactory(
    private val application: Application,
    private val sealColonyRepository: SealColonyRepository,
    private val supportingDataRepository: FilesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SealColoniesViewModel::class.java)) {
            return SealColoniesViewModel(
                application,
                sealColonyRepository,
                supportingDataRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
