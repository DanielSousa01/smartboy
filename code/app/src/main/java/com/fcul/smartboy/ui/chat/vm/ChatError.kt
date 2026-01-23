package com.fcul.smartboy.ui.chat.vm

sealed class ChatError {
    object MessageSendFailed : ChatError()
    object MessageLoadFailed : ChatError()
    object ConversationLoadFailed : ChatError()
    object MessageImageUploadFailed : ChatError()
    data class Generic(val message: String? = null) : ChatError()
}