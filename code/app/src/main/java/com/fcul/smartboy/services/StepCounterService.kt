package com.fcul.smartboy.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fcul.smartboy.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var auth: FirebaseAuth

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var initialStepCount: Long = 0
    private var currentStepCount: Long = 0
    private var sessionSteps: Long = 0
    private var lastSyncedSteps: Long = 0
    private var isInitialized = false

    companion object {
        private const val TAG = "StepCounterService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "step_counter_channel"
        private const val SYNC_THRESHOLD = 10 // Sync every 10 steps
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StepCounterService onCreate() called")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounterSensor != null) {
            Log.i(TAG, "Step counter sensor found: ${stepCounterSensor?.name}")
            val registered = sensorManager.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.i(TAG, "Sensor listener registered: $registered")
        } else {
            Log.e(TAG, "Step counter sensor NOT available on this device!")
            val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            Log.d(TAG, "Available sensors: ${allSensors.joinToString { it.name }}")
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(0))
        Log.d(TAG, "Service started in foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed for accuracy changes in this service
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val steps = event.values[0].toLong()
            Log.d(TAG, "onSensorChanged: Raw step count = $steps")

            if (!isInitialized) {
                initialStepCount = steps
                isInitialized = true
                Log.i(TAG, "Initialized with step count: $initialStepCount")
            }

            currentStepCount = steps
            sessionSteps = currentStepCount - initialStepCount
            Log.d(
                TAG,
                "Session steps: $sessionSteps (current: $currentStepCount, initial: $initialStepCount)"
            )

            val notification = createNotification(sessionSteps)
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            // Sync to Firebase periodically
            val stepsSinceLastSync = sessionSteps - lastSyncedSteps
            if (stepsSinceLastSync >= SYNC_THRESHOLD) {
                Log.i(TAG, "Syncing $stepsSinceLastSync steps to Firebase")
                syncStepsToFirebase()
            }
        }
    }

    private fun syncStepsToFirebase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot sync steps: User not authenticated")
            return
        }

        serviceScope.launch {
            try {
                val increment = sessionSteps - lastSyncedSteps
                Log.d(TAG, "Syncing $increment steps to Firebase for user $userId")
                profileRepository.incrementSteps(userId, increment)
                lastSyncedSteps = sessionSteps
                Log.i(TAG, "Successfully synced steps. Total session steps: $sessionSteps")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync steps to Firebase", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your steps in the background"
            }

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(steps: Long): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Counter Active")
            .setContentText("Steps today: $steps")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroying, performing final sync")
        sensorManager.unregisterListener(this)
        syncStepsToFirebase()
        serviceScope.cancel()
        Log.d(TAG, "StepCounterService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

