package com.amgm.notebook.ui.common

import com.amgm.notebook.ui.navigation.Destination

/** ViewModel が発行する一回限りのイベント。 */
sealed interface UiEvent {
    data class Navigate(val destination: Destination) : UiEvent
    data object NavigateBack : UiEvent
    data class ShowMessage(val message: UiMessage) : UiEvent
}
