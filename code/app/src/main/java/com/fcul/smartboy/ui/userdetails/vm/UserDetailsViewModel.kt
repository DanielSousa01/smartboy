package com.fcul.smartboy.ui.userdetails.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.SellingRepository
import com.fcul.smartboy.ui.profile.vm.ProfileError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDetailsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sellingRepository: SellingRepository,
) : ViewModel() {
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _sellingItems = MutableStateFlow<List<SellingItem>>(emptyList())
    val sellingItems: StateFlow<List<SellingItem>> = _sellingItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<ProfileError?>(null)
    val error: StateFlow<ProfileError?> = _error.asStateFlow()

    fun onDismissError() {
        _error.value = null
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Load profile
                val userProfile = profileRepository.read(userId)
                _profile.value = userProfile

                // Load selling items
                loadSellingItems(userId)

                Log.d(TAG, "Loaded profile for user: $userId")
            } catch (e: Exception) {
                _error.value = ProfileError.FailedToLoadProfile
                Log.e(TAG, "Failed to load user profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSellingItems(userId: String) {
        try {
            sellingRepository.observeSellingItemsForUser(
                userId
            ).collect {
                _sellingItems.value = it
                _isLoading.value = false
                Log.d(TAG, "Loaded ${it.size} selling items for user: $userId")
            }
        } catch (e: Exception) {
            _error.value = ProfileError.FailedToLoadSellingItems
            Log.e(TAG, "Failed to load selling items", e)
            _sellingItems.value = emptyList()
        }
    }

    companion object {
        private const val TAG = "UserDetailsViewModel"
    }
}
