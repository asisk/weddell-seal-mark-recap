package weddellseal.markrecap.frameworks.room

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony

class SealColonyRepository {
    private val _colony = MutableStateFlow<SealColony?>(null)
    val colony: StateFlow<SealColony?> = _colony

    fun setColony(colony: SealColony?) {
        _colony.value = colony
    }

    private val _overrideAutoColony = MutableStateFlow(false)
    val overrideAutoColony: StateFlow<Boolean> = _overrideAutoColony

    fun setOverrideAutoColony(value: Boolean) {
        _overrideAutoColony.value = value
    }
}