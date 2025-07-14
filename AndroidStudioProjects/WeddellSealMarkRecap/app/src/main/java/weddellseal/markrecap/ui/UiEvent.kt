package weddellseal.markrecap.ui

//TODO, apply this throughout the implementation
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    object ShowEditDialog : UiEvent()
    object ShowArchiveDialog : UiEvent()
    object ShowDeleteRecordsDialog : UiEvent()
}