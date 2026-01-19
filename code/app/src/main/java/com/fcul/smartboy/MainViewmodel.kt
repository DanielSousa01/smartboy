package com.fcul.smartboy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class MainViewmodel(
    private val auth: FirebaseAuth,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _userProfile = MutableStateFlow<Profile?>(null)
    val userProfile: StateFlow<Profile?> = _userProfile.asStateFlow()

    init {
        auth.currentUser?.uid?.let { userId ->
            loadProfile(userId)
        }
    }

    private fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                _userProfile.value = profileRepository.read(userId)
            } catch (_: Exception) {
                _userProfile.value = null
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
        _userProfile.value = null
    }
}