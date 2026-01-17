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
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "step_counter_channel"
        private const val SYNC_THRESHOLD = 100 // Sync every 100 steps
    }

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounterSensor != null) {
            sensorManager.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val steps = event.values[0].toLong()

            if (!isInitialized) {
                initialStepCount = steps
                isInitialized = true
            }

            currentStepCount = steps
            sessionSteps = currentStepCount - initialStepCount

            // Update notification
            val notification = createNotification(sessionSteps)
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            // Sync to Firebase periodically
            if (sessionSteps - lastSyncedSteps >= SYNC_THRESHOLD) {
                syncStepsToFirebase()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    private fun syncStepsToFirebase() {
        val userId = auth.currentUser?.uid ?: return

        serviceScope.launch {
            try {
                val increment = sessionSteps - lastSyncedSteps
                profileRepository.incrementSteps(userId, increment)
                lastSyncedSteps = sessionSteps
            } catch (e: Exception) {
                Log.e("StepCounterService", "Failed to sync steps", e)
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
        sensorManager.unregisterListener(this)
        syncStepsToFirebase() // Final sync before stopping
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

