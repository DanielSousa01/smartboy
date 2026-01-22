package com.fcul.smartboy.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.lifecycle.lifecycleScope
import com.fcul.smartboy.MainActivity
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.domain.user.User
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.UserRepository
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalMaterial3ExpressiveApi
class AuthActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val signIn: ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract(), this::onSignInResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()

        // If there is no signed in user, launch FirebaseUI
        // Otherwise head to MainActivity
        if (Firebase.auth.currentUser == null) {
            // Sign in with FirebaseUI, see docs for more details:
            // https://firebase.google.com/docs/auth/android/firebaseui
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.mipmap.ic_launcher)
                .setAvailableProviders(
                    listOf(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                    )
                )
                .build()

            signIn.launch(signInIntent)
        } else {
            goToMainActivity()
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Sign in successful!")

            val user = auth.currentUser
            if (user != null) {
                lifecycleScope.launch {
                    try {
                        val existingProfile = profileRepository.read(user.uid)

                        if (existingProfile == null) {
                            Log.d(TAG, "New user detected, creating profile in both databases...")

                            val newProfile = Profile(
                                userId = user.uid,
                                username = user.displayName ?: "Guest",
                                steps = 0,
                                caps = 1000
                            )
                            profileRepository.create(newProfile)

                            val newUser = User(
                                userId = user.uid,
                                username = user.displayName ?: "Guest",
                                email = user.email ?: "",
                                photoUrl = user.photoUrl?.toString()
                            )
                            userRepository.create(newUser)

                            Log.d(TAG, "User profile created successfully in both databases")
                        } else {
                            Log.d(TAG, "Existing user, profile already exists - skipping creation")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating user profile", e)
                    } finally {
                        goToMainActivity()
                    }
                }
            } else {
                goToMainActivity()
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.sign_in_failed),
                Toast.LENGTH_LONG
            ).show()

            val response = result.idpResponse
            if (response == null) {
                Log.w(TAG, "Sign in canceled")
            } else {
                Log.w(TAG, "Sign in error", response.error)
            }
        }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "SignInActivity"
    }
}