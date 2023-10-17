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

import android.content.ContentResolver
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


const val CSV_FILE_PATH = "./result.csv"
class ObservationSaverRepository(context: Context, private val contentResolver: ContentResolver) {
    private val db = AppDatabase.getDatabase(context)

    var _observations = mutableListOf<ObservationLogEntry>()
    fun getObservations() = _observations.toList()
    private val obsFolder = File(context.filesDir, "observations").also { it.mkdir() }
    private fun generateFileName() = "${System.currentTimeMillis()}.csv"
    fun generateObservationLogFile() = File(obsFolder, generateFileName())
    suspend fun saveObservations(): Boolean {
        return withContext(Dispatchers.IO) {
            var success = false
            try {
                _observations = selectAllObservationsfromDB()
                if (_observations.isEmpty()) {
//                    success = false
                } else {
                    success = writeObservationsToCSV(generateObservationLogFile(),_observations)
                }
            } catch (e : Exception) {
                println(e)
//                success = false
            }
            success
        }
    }

    fun writeObservationsToCSV(file: File, obsList: List<ObservationLogEntry>) : Boolean {
        val data: MutableList<Array<String>> = ArrayList()

        for (obs in obsList) {
            val obsFields = StringBuilder().append(obs?.date ?: "")
                .append(obs?.currentLocation ?: "")
                .append(obs?.lastKnownLocation ?: "").toString()
            data.add(arrayOf(obsFields))
        }

        return try {
            CSVUtils().writeDataAtOnce(file.path, data)
            true
        }catch (e : Exception){
            false
        }
    }

    fun isEmpty() = _observations.isEmpty()
    fun canAddObservation() = true
    suspend fun writeObservationtoDB(log: ObservationLogEntry) {
        db.observationDao().insert(log)
    }

    private fun selectAllObservationsfromDB() : MutableList<ObservationLogEntry> {
        val logs : List<ObservationLogEntry?> = db.observationDao().getObservationsForCSVWrite()
        var list = mutableListOf<ObservationLogEntry>()
        for (log in logs) {
            if (log != null) {
                list.add(log)
            }
        }
        return list
    }

}
