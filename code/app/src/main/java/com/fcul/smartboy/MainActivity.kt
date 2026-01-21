package com.fcul.smartboy

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
        val isGranted = permissions.entries.all { it.value }
        Log.d("MainActivity", "Permissions result: $permissions, all granted: $isGranted")
        if (isGranted) {
            startStepCounterService()
        } else {
            Log.w("MainActivity", "Some permissions were denied")
            startStepCounterService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build permission list based on API level
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        Log.d("MainActivity", "Requesting permissions: $permissions")
        requestPermissionsLauncher.launch(permissions.toTypedArray())

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
                    startActivity(
                        Intent(this@MainActivity, AuthActivity::class.java)
                    )
                    finish()
                }
            }
        }
    }

    private fun startStepCounterService() {
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
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start StepCounterService", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop step counter service when activity is destroyed
        val intent = Intent(this, StepCounterService::class.java)
        stopService(intent)
    }
}
