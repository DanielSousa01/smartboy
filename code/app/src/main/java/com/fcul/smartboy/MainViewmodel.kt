package com.fcul.smartboy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _userProfile = MutableStateFlow<Profile?>(null)
    val userProfile: StateFlow<Profile?> = _userProfile.asStateFlow()

    private var profileObservationJob: Job? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUser = firebaseAuth.currentUser
        _user.value = newUser

        // Handle profile observation based on auth state
        if (newUser != null) {
            // User signed in, start observing profile
            observeProfile(newUser.uid)
        } else {
            // User signed out, stop observing and clear profile
            stopObservingProfile()
            _userProfile.value = null
        }
    }

    init {
        // Auth state listener for real-time user changes
        auth.addAuthStateListener(authStateListener)

        auth.currentUser?.uid?.let { userId ->
            observeProfile(userId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove listener when ViewModel is cleared
        auth.removeAuthStateListener(authStateListener)
        stopObservingProfile()
    }

    private fun observeProfile(userId: String) {
        // Cancel any existing observation
        stopObservingProfile()

        // Start new observation
        profileObservationJob = viewModelScope.launch {
            profileRepository.observeProfile(userId).collect { profile ->
                _userProfile.value = profile
            }
        }
    }

    private fun stopObservingProfile() {
        profileObservationJob?.cancel()
        profileObservationJob = null
    }

    fun signOut() {
        // Stop observing profile to prevent permission errors
        stopObservingProfile()

        // Clear profile data
        _userProfile.value = null

        // Sign out from Firebase (this will trigger authStateListener)
        auth.signOut()

        // Explicitly set user to null to ensure state update
        _user.value = null
    }
}

