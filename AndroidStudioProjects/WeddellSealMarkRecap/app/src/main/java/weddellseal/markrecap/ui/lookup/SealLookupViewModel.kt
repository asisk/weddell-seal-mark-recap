package weddellseal.markrecap.ui.lookup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.wedCheck.toSeal

class SealLookupViewModel(
    application: Application,
    private val wedCheckRepo: WedCheckRepository
) : AndroidViewModel(application) {

    private val _lookupSeal = MutableStateFlow(WedCheckSeal())
    val lookupSeal: StateFlow<WedCheckSeal> = _lookupSeal

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    data class UiState(
        val loading: Boolean = false,
        val isSearching: Boolean = false,
        val sealFound: Boolean = false,
        val sealNotFound: Boolean = false,
    )

    fun resetLookupUiState() {
        _uiState.update {
            it.copy(
                loading = false,
                isSearching = false,
                sealFound = false,
                sealNotFound = false
            )
        }

        _lookupSeal.update { WedCheckSeal() }
    }

    fun resetLookupSeal() {
        _lookupSeal.update { WedCheckSeal() }
    }

    fun findSealbyTagID(sealTagID: String) {
        resetLookupUiState()
        resetLookupSeal()

        if (sealTagID != "") {
            findSeal { wedCheckRepo.findSealbyTagID(sealTagID.trim()) }
        }
    }

    fun findSealbySpeno(speno: Int) {
        resetLookupUiState()
        resetLookupSeal()

        if (speno > 0) {
            findSeal { wedCheckRepo.findSealbySpeNo(speno) }
        }
    }

    fun findSeal(query: suspend () -> WedCheckRecord?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val seal: WedCheckRecord? = withContext(Dispatchers.IO) {
                    query()
                }

                if (seal != null) {
                    _lookupSeal.update { seal.toSeal() }
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            sealFound = true,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            sealFound = false,
                            sealNotFound = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SealLookup", "Error fetching seal: ${e.localizedMessage}", e)
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        sealFound = false,
                        sealNotFound = true
                    )
                }
            }
        }
    }
}