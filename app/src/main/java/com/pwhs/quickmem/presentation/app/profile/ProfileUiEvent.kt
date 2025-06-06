package com.pwhs.quickmem.presentation.app.profile


sealed class ProfileUiEvent {
    data object Loading : ProfileUiEvent()
    data class ShowError(val message: String) : ProfileUiEvent()
}