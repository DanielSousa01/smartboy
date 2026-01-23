package com.fcul.smartboy.ui.profile.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<ProfileError?>(null)
    val error: StateFlow<ProfileError?> = _error.asStateFlow()

    init {
        loadProfile()
        observeProfile()
    }

    fun onDismissError() {
        _error.value = null
    }

    private fun observeProfile() {
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            profileRepository.observeProfile(user.uid).collect { profile ->
                if (profile != null) {
                    _profile.value = profile
                } else {
                    _error.value = ProfileError.FailedToLoadProfile
                    Log.e(TAG, "Observed null profile for user ${user.uid})")
                }
            }
        }
    }

    fun loadProfile() {
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userProfile = profileRepository.read(user.uid)
                _profile.value = userProfile
            } catch (e: Exception) {
                _error.value = ProfileError.FailedToLoadProfile
                Log.e(TAG, "Error loading profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val TAG = "ProfileViewmodel"
    }
}