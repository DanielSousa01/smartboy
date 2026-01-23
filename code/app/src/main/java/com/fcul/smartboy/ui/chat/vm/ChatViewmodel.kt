package com.fcul.smartboy.ui.chat.vm

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.chat.Conversation
import com.fcul.smartboy.domain.chat.Message
import com.fcul.smartboy.repository.ChatRepository
import com.fcul.smartboy.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<ChatError?>(null)
    val error: StateFlow<ChatError?> = _error

    private val _userCaps = MutableStateFlow(0)
    val userCaps: StateFlow<Int> = _userCaps

    private var currentChatUserId: String? = null

    init {
        observeConversations()
    }

    fun observeUserProfile(userId: String) {
        viewModelScope.launch {
            profileRepository.observeProfile(userId).collect { profile ->
                _userCaps.value = profile?.caps ?: 0
            }
        }
    }

    fun startChat(userId: String) {
        currentChatUserId = userId
        observeMessages(userId)
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.observeConversations()
                .catch { e ->
                    _error.value = ChatError.ConversationLoadFailed
                    Log.e(TAG, "Conversation load failed", e)
                }
                .collect { conversationList ->
                    _conversations.value = conversationList
                }
        }
    }

    private fun observeMessages(otherUserId: String) {
        viewModelScope.launch {
            chatRepository.observeConversation(otherUserId)
                .catch { e ->
                    _error.value = ChatError.MessageLoadFailed
                    Log.e(TAG, "Message load failed", e)
                }
                .collect { messageList ->
                    _messages.value = messageList
                }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun sendMessage(recipientName: String) {
        val text = _messageText.value.trim()
        val recipientId = currentChatUserId

        if (text.isEmpty() || recipientId == null) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                chatRepository.sendMessage(
                    recipientId = recipientId,
                    recipientName = recipientName,
                    text = text
                )
                _messageText.value = ""
                _error.value = null
            } catch (e: Exception) {
                _error.value = ChatError.MessageSendFailed
                Log.e(TAG, "Message send failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendImageMessage(imageUri: Uri, recipientName: String) {
        val recipientId = currentChatUserId ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val imageUrl = chatRepository.uploadImage(imageUri)
                chatRepository.sendMessage(
                    recipientId = recipientId,
                    recipientName = recipientName,
                    text = "",
                    imageUrl = imageUrl
                )
            } catch (e: Exception) {
                _error.value = ChatError.MessageImageUploadFailed
                Log.e(TAG, "Image message send failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}