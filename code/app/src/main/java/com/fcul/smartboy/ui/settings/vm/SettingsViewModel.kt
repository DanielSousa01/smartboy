package com.fcul.smartboy.ui.settings.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.user.MeasurementUnit
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.ui.profile.vm.ProfileError
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
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
    }

    fun onDismissError() {
        _error.value = null
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isLoading.value = true
            try {
                profileRepository.observeProfile(userId).collect { profile ->
                    _profile.value = profile
                }
            } catch (e: Exception) {
                _error.value = ProfileError.FailedToLoadProfile
                Log.e(TAG, "Error loading profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMeasurementUnit(unit: MeasurementUnit) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isLoading.value = true
            try {
                profileRepository.updateMeasurementUnit(userId, unit)
            } catch (e: Exception) {
                _error.value = ProfileError.FailedToUpdateProfile
                Log.e(TAG, "Error updating measurement unit", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}