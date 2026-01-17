package com.fcul.smartboy.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.chat.ChatUser
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
class ChatViewmodel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _selectedUser = MutableStateFlow<ChatUser?>(null)
    val selectedUser: StateFlow<ChatUser?> = _selectedUser

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _viewState = MutableStateFlow("conversations")
    val viewState: StateFlow<String> = _viewState

    private val _userCaps = MutableStateFlow(0)
    val userCaps: StateFlow<Int> = _userCaps

    init {
        observeConversations()
        observeUserProfile()
    }

    private fun observeUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            profileRepository.observeProfile(userId).collect { profile ->
                _userCaps.value = profile?.caps ?: 0
            }
        }
    }

    fun selectUser(user: ChatUser) {
        _selectedUser.value = user
        _viewState.value = "chat"
        observeMessages(user.userId)
    }

    fun backToConversations() {
        _selectedUser.value = null
        _viewState.value = "conversations"
        _messages.value = emptyList()
    }


    fun openConversation(conversation: Conversation) {
        val otherUserId = conversation.participantIds.firstOrNull {
            it != auth.currentUser?.uid
        } ?: return

        val otherUserName = conversation.participantNames[otherUserId] ?: "User"

        val chatUser = ChatUser(
            userId = otherUserId,
            userName = otherUserName
        )
        selectUser(chatUser)
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.observeConversations()
                .catch { e ->
                    _error.value = "Failed to load conversations: ${e.message}"
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
                    _error.value = "Failed to load messages: ${e.message}"
                }
                .collect { messageList ->
                    _messages.value = messageList
                }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        val recipient = _selectedUser.value

        if (text.isEmpty() || recipient == null) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                chatRepository.sendMessage(
                    recipientId = recipient.userId,
                    recipientName = recipient.userName,
                    text = text
                )
                _messageText.value = ""
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendImageMessage(imageUri: Uri) {
        val recipient = _selectedUser.value ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val imageUrl = chatRepository.uploadImage(imageUri)
                chatRepository.sendMessage(
                    recipientId = recipient.userId,
                    recipientName = recipient.userName,
                    text = "",
                    imageUrl = imageUrl
                )
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to upload image: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

