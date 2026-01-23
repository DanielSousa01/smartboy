package com.fcul.smartboy.ui.profile.vm

sealed class ProfileError {
    object FailedToLoadProfile : ProfileError()
    object FailedToUpdateProfile : ProfileError()
    object FailedToLoadSellingItems : ProfileError()
}