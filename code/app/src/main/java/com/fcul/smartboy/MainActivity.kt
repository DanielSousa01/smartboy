package com.fcul.smartboy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fcul.smartboy.services.StepCounterService
import com.fcul.smartboy.ui.auth.AuthActivity
import com.fcul.smartboy.ui.theme.SmartBoyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
@ExperimentalMaterial3ExpressiveApi
class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        Log.d("MainActivity", "Permissions result: $permissions, all granted: $allGranted")

        // Only start service if all permissions are granted
        if (allGranted) {
            startStepCounterService()
        } else {
            Log.w("MainActivity", "Permissions denied, StepCounterService will not start")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        val user = auth.currentUser

        if (user == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            SmartBoyTheme {
                SmartBoyApp(viewModel)
            }
        }

        lifecycleScope.launch {
            viewModel.user.collect { user ->
                if (user == null) {
                    // Navigate to AuthActivity with proper flags for smooth transition
                    val intent = Intent(this@MainActivity, AuthActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        // Check and request permissions after UI is set up
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // Build permission list based on API level
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // Check if all permissions are already granted
        val allPermissionsGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            Log.d("MainActivity", "All permissions already granted")
            startStepCounterService()
        } else {
            Log.d("MainActivity", "Requesting permissions: $permissions")
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startStepCounterService() {
        // Double-check permissions before starting service
        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older Android versions
        }

        if (!hasActivityRecognition) {
            Log.w(
                "MainActivity",
                "Cannot start StepCounterService: Missing ACTIVITY_RECOGNITION permission"
            )
            return
        }

        Log.d("MainActivity", "Starting StepCounterService...")
        val intent = Intent(this, StepCounterService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                Log.d("MainActivity", "StepCounterService started as foreground service")
            } else {
                startService(intent)
                Log.d("MainActivity", "StepCounterService started as regular service")
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException when starting service: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start StepCounterService: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the service when activity is destroyed
        try {
            stopService(Intent(this, StepCounterService::class.java))
            Log.d("MainActivity", "StepCounterService stopped")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping service: ${e.message}")
        }
    }
}
