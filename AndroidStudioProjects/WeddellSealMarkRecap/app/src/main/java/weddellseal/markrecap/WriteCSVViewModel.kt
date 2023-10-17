package weddellseal.markrecap

/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//import com.example.photolog_start.AppDatabase.Companion.DB_NAME
//import com.google.android.gms.location.LocationServices
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.launch

class WriteCSVViewModel(
    application: Application,
    private val observationSaver: ObservationSaverRepository
) : AndroidViewModel(application) {
    // region ViewModel setup
    private val context: Context
        get() = getApplication()

    private val db = AppDatabase.getDatabase(context)
    // endregion

    // region UI state
    data class UiState(
        val isWriting: Boolean = false,
        val doneWriting: Boolean = false,
    )

    var uiState by mutableStateOf(
        UiState(
            isWriting = false,
            doneWriting = false
        )
    )
        private set

    fun isValid(): Boolean {
        if (uiState.isWriting) {
            return false
        }
        return true
    }

    fun createSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.fromParts("package", context.packageName, null)
        }

        return intent
    }
    // endregion
    fun writeToCSV() {
        if (!isValid()) {
            return
        }

        uiState = uiState.copy(isWriting = true)

        viewModelScope.launch {
            db.observationDao().getObservationsForCSVWrite()

            uiState = uiState.copy(doneWriting = true)
        }
    }
    // endregion
}

class WriteCSVViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[APPLICATION_KEY] as ObservationLogApplication
        return AddObservationLogViewModel(app, app.observationSaver) as T
    }
}