package com.example.pocketplanner.ui.ml

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.pocketplanner.R
import com.example.pocketplanner.MainActivity

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var classifier: FallDetectionClassifier

    private val windowSize = 128
    private val stepSize = 64
    private val sensorDataBuffer = mutableListOf<FloatArray>()

    private var lastAlertTime = 0L // 35-second cooldown tracker
    private var serviceStartTime = 0L // NEW: Tracks when the service booted up

    // Notification IDs and Channels
    private val CHANNEL_ID = "FallDetectionChannel"
    private val ALARM_CHANNEL_ID = "FallAlarmChannel"
    private val NOTIFICATION_ID = 1
    private val ALARM_NOTIFICATION_ID = 2

    private var consecutiveFallTriggers = 0
    private var isHighSensitivity = false

    override fun onCreate() {
        super.onCreate()

        classifier = FallDetectionClassifier(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        // NEW: Record exactly when the user turned the setting on
        serviceStartTime = System.currentTimeMillis()

        // ADD THIS LINE: Read the sensitivity toggle passed from your UI!
        isHighSensitivity = intent?.getBooleanExtra("HIGH_SENSITIVITY", false) ?: false

        // NEW: Lock the sensor speed to exactly 50Hz (20,000 microseconds)
        // to match your Jupyter Notebook training data perfectly.
        accelerometer?.let {
            sensorManager.registerListener(this, it, 20000)
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            // NEW: The 10-Second Grace Period!
            // Ignores all movement while the user is tapping the screen and putting the phone away.
            if (System.currentTimeMillis() - serviceStartTime < 10_000) {
                return
            }

            // 1. Add current X, Y, Z to the buffer
            sensorDataBuffer.add(floatArrayOf(event.values[0], event.values[1], event.values[2]))

            // 2. Once we hit 128 readings, run the model
            if (sensorDataBuffer.size >= windowSize) {

                val inputData = Array(1) { Array(windowSize) { FloatArray(3) } }
                for (i in 0 until windowSize) {
                    inputData[0][i] = sensorDataBuffer[i]
                }

                val probability = classifier.classifyFall(inputData)

                // DYNAMIC LOGIC: Only the confidence threshold changes based on the toggle!
                val requiredConfidence = if (isHighSensitivity) 0.85f else 0.95f

                if (probability > requiredConfidence) {
                    consecutiveFallTriggers++

                    // Always require 2 overlapping windows to agree it is a fall
                    if (consecutiveFallTriggers >= 2) {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastAlertTime > 35_000) {
                            lastAlertTime = currentTime
                            triggerEmergencyAlert()
                        }

                        sensorDataBuffer.clear()
                        consecutiveFallTriggers = 0 // Reset after triggering
                    } else {
                        // We have 1 trigger, but we need 2. Slide the window forward to check the next 50%.
                        for (i in 0 until stepSize) {
                            sensorDataBuffer.removeAt(0)
                        }
                    }
                } else {
                    // Model is not confident it was a fall, reset the consecutive counter
                    consecutiveFallTriggers = 0

                    // Slide window normally
                    for (i in 0 until stepSize) {
                        sensorDataBuffer.removeAt(0)
                    }
                }
            }
        }
    }

    /**
     * This function fires when the AI confirms a fall.
     * It launches the full-screen EmergencyAlertActivity.
     */
    private fun triggerEmergencyAlert() {
        // We use the exact path to the new Activity we just created
        val alertIntent = android.content.Intent(
            this,
            com.example.pocketplanner.ui.alerts.EmergencyAlertActivity::class.java
        ).apply {
            // CRITICAL: A background Service does not have a UI task stack.
            // You MUST include FLAG_ACTIVITY_NEW_TASK, or Android will crash the app.
            // CLEAR_TOP ensures we don't open 5 identical screens if the sensor goes crazy.
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        startActivity(alertIntent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        classifier.close()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Safety Guard Active")
            .setContentText("Monitoring for falls in the background.")
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Fall Detection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Emergency Fall Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers the full-screen emergency countdown"
            }
            manager.createNotificationChannel(alarmChannel)
        }
    }
}