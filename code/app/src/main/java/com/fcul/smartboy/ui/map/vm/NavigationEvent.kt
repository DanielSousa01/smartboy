package com.fcul.smartboy.ui.map.vm

sealed class NavigationEvent {
    data class NavigateToUserDetails(val userId: String) : NavigationEvent()
}