package com.fcul.smartboy.ui.userdetails

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.SellingRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UserDetailsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sellingRepository: SellingRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile

    private val _sellingItems = MutableStateFlow<List<SellingItem>>(emptyList())
    val sellingItems: StateFlow<List<SellingItem>> = _sellingItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load profile
                val userProfile = profileRepository.read(userId)
                _profile.value = userProfile

                // Load selling items
                loadSellingItems(userId)

                Log.d("UserDetailsViewModel", "Loaded profile for user: $userId")
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                Log.e("UserDetailsViewModel", "Failed to load user profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSellingItems(userId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("selling")
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.fcul.smartboy.domain.inventory.SellingItemEntity::class.java)
                    ?.toSellingItem()
            }

            _sellingItems.value = items
            Log.d("UserDetailsViewModel", "Loaded ${items.size} selling items for user: $userId")
        } catch (e: Exception) {
            Log.e("UserDetailsViewModel", "Failed to load selling items", e)
            _sellingItems.value = emptyList()
        }
    }
}
