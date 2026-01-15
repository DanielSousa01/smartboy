package com.fcul.smartboy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.services.StepCounterService
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startStepCounterService()
        }
    }

    private val requestLocationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Fine location access granted
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Coarse location access granted
            }
            else -> {
                // No location access granted
            }
        }
    }

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

            // Request location permissions first
            checkAndRequestLocationPermissions()

            // Start step counter service
            checkAndRequestActivityRecognitionPermission()

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

    private fun checkAndRequestLocationPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted || !coarseLocationGranted) {
            requestLocationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkAndRequestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startStepCounterService()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            startStepCounterService()
        }
    }

    private fun startStepCounterService() {
        val intent = Intent(this, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop step counter service when activity is destroyed
        val intent = Intent(this, StepCounterService::class.java)
        stopService(intent)
    }
}
