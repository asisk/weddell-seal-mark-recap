package weddellseal.markrecap.ui

//TODO, apply this throughout the implementation
sealed class UiEvent {
    data class ShowEditToast(val message: String) : UiEvent()
    data class ShowSavedToast(val message: String) : UiEvent()
    object ShowEditDialog : UiEvent()
    object ShowArchiveDialog : UiEvent()
    object ShowDeleteRecordsDialog : UiEvent()
}