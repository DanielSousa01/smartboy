package com.fcul.smartboy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.lifecycle.lifecycleScope
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.ui.auth.AuthActivity
import com.fcul.smartboy.ui.theme.SmartBoyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
@ExperimentalMaterial3ExpressiveApi
class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        profileRepository = ProfileRepository(
            firestore = Firebase.firestore
        )

        val user = auth.currentUser

        if (user == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val profile = profileRepository.read(user.uid)
            if (profile == null) {
                profileRepository.create(Profile(userId = user.uid))
            }

            val vm = MainViewmodel(auth = auth)

            enableEdgeToEdge()

            setContent {
                SmartBoyTheme {
                    SmartBoyApp(vm)
                }
            }

            lifecycleScope.launch {
                vm.user.collect { user ->
                    if (user == null) {
                        startActivity(
                            Intent(this@MainActivity, AuthActivity::class.java)
                        )
                        finish()
                    }
                }
            }
        }
    }
}