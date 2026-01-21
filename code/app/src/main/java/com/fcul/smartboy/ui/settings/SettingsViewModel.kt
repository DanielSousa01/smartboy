package com.fcul.smartboy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.user.MeasurementUnit
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
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _isLoading.value = true
            try {
                profileRepository.observeProfile(userId).collect { profile ->
                    _profile.value = profile
                }
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
            } finally {
                _isLoading.value = false
            }
        }
    }
}
