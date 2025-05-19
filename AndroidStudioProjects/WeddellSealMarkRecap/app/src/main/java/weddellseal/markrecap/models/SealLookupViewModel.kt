package weddellseal.markrecap.models

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
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckSeal
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
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
        val isTagRetagLookup: Boolean = false,
    )

    fun resetUiState() {
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

    fun setTagRetagLookup(isTagRetagLookup: Boolean) {
        _uiState.update { it.copy(isTagRetagLookup = isTagRetagLookup) }
    }

    fun findSealbyTagID(sealTagID: String) {
        resetUiState()
        resetLookupSeal()

        if (sealTagID != "") {
            val searchValue = sealTagID.trim()
            findSeal { wedCheckRepo.findSealbyTagID(searchValue) }
            _uiState.update { it.copy(isSearching = true) }
        }
    }

    fun findSealbySpeno(speno: Int) {
        resetUiState()
        resetLookupSeal()

        if (speno > 0) {
            findSeal { wedCheckRepo.findSealbySpeNo(speno) }
            _uiState.update { it.copy(isSearching = true) }
        }
    }

    fun findSeal(query: suspend () -> WedCheckRecord?) {
        viewModelScope.launch {
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