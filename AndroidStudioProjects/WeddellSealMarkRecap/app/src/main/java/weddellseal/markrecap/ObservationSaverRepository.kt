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
import java.io.File

class ObservationSaverRepository(context: Context, private val contentResolver: ContentResolver) {
    private val _observations = mutableListOf<File>()
    fun getObservations() = _observations.toList()
    fun isEmpty() = _observations.isEmpty()
    fun canAddObservation() = true
}
