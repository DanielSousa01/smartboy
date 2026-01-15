package com.fcul.smartboy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewmodel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadProfile()
    }

    fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userProfile = profileRepository.read(userId)
                if (userProfile == null) {
                    val newProfile = Profile(userId = userId, steps = 0, caps = 0)
                    profileRepository.create(newProfile)
                    _profile.value = newProfile
                } else {
                    _profile.value = userProfile
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadProfile()
    }
}

