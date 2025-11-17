package com.fcul.smartboy

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class MainViewmodel(
    auth: FirebaseAuth
) : ViewModel() {
    private var _user: MutableStateFlow<FirebaseUser?> = MutableStateFlow(null)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    init {
        val authUser = auth.currentUser ?: throw IllegalStateException("User must be logged in")
        _user.value = authUser
    }
}