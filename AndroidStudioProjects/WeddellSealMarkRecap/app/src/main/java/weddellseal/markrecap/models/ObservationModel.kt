package weddellseal.markrecap.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

//currently unused
class ObservationModel : ViewModel() {
    private val _text1: MutableLiveData<String> = MutableLiveData("")
    val text1: LiveData<String> = _text1

    fun onNameChange (newText: String) {
        _text1.value = newText
    }
}