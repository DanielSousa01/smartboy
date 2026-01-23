package com.fcul.smartboy.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.lifecycle.lifecycleScope
import com.fcul.smartboy.MainActivity
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.domain.user.User
import com.fcul.smartboy.repository.InventoryRepository
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.UserRepository
import com.fcul.smartboy.ui.theme.SmartBoyTheme
import com.fcul.smartboy.utils.SampleItems
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

    @Inject
    lateinit var inventoryRepository: InventoryRepository

    private val signIn: ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract(), this::onSignInResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        // If user is already signed in, go to main activity
        if (auth.currentUser != null) {
            goToMainActivity()
            return
        }

        // Show custom sign-in screen
        setContent {
            SmartBoyTheme {
                SignInScreen(
                    onEmailSignInClick = { launchFirebaseUIEmailSignIn() },
                    onGoogleSignInClick = { launchFirebaseUIGoogleSignIn() }
                )
            }
        }
    }

    private fun launchFirebaseUIEmailSignIn() {
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setLogo(R.mipmap.ic_launcher)
            .setAvailableProviders(
                listOf(
                    AuthUI.IdpConfig.EmailBuilder().build()
                )
            )
            .build()

        signIn.launch(signInIntent)
    }

    private fun launchFirebaseUIGoogleSignIn() {
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setLogo(R.mipmap.ic_launcher)
            .setAvailableProviders(
                listOf(
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )
            )
            .build()

        signIn.launch(signInIntent)
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
                                caps = CAPS_FOR_NEW_USER
                            )
                            profileRepository.create(newProfile)

                            val newUser = User(
                                userId = user.uid,
                                username = user.displayName ?: "Guest",
                                email = user.email ?: "",
                                photoUrl = user.photoUrl?.toString()
                            )
                            userRepository.create(newUser)

                            // Populate inventory with starter items
                            Log.d(TAG, "Populating starter items for new user...")
                            val starterItems = SampleItems.getStarterItems()
                            starterItems.forEach { item ->
                                try {
                                    inventoryRepository.create(item)
                                    Log.d(TAG, "Added starter item: ${item.name}")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to add item ${item.name}: ${e.message}")
                                }
                            }

                            Log.d(TAG, "User profile created successfully with ${starterItems.size} starter items")
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
        private const val CAPS_FOR_NEW_USER = 1000
    }
}