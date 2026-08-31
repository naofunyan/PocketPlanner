package com.example.pocketplanner.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.location.Location
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import com.example.pocketplanner.R

class EmergencyActionManager(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Master SOS Sequence
     */
    fun executeSosSequence(
        savedContacts: Set<String>,
        emergencyNumber: String,
        attachAudio: Boolean,
        attachPicture: Boolean,
        shareMedicalInfo: Boolean = false, // <-- NEW PARAMETER
        medicalSummary: String = ""        // <-- NEW PARAMETER
    ) {
        scope.launch {
            try {
                // 1. Gather Data Concurrently
                val locationDeferred = async { fetchLocation() }
                val audioDeferred = async { if (attachAudio) recordAudio() else null }
                val pictureDeferred = async { if (attachPicture) captureBackgroundPhoto() else null }

                val locationUrl = locationDeferred.await()
                val audioFile = audioDeferred.await()
                val pictureFile = pictureDeferred.await()

                // 2. Upload Media Concurrently
                val uploadTasks = listOfNotNull(
                    audioFile?.let { async { uploadToFirebase(it, "audio") } },
                    pictureFile?.let { async { uploadToFirebase(it, "images") } }
                )

                val mediaLinks = uploadTasks.awaitAll().filterNotNull()
                val mediaString = if (mediaLinks.isNotEmpty()) context.getString(R.string.sos_sms_media, mediaLinks.joinToString(" | ")) else ""

                // 3. Construct Final Message
                val message = buildString {
                    append(context.getString(R.string.sos_sms_message, emergencyNumber))
                    append(context.getString(R.string.sos_sms_location, locationUrl))
                    if (mediaString.isNotEmpty()) append(mediaString)

                    // NEW: Append medical data if the user toggled it ON
                    if (shareMedicalInfo && medicalSummary.isNotEmpty()) {
                        append(context.getString(R.string.sos_sms_medical, medicalSummary))
                    }
                }

                // 4. Dispatch SMS
                val phoneNumbers = savedContacts.mapNotNull { it.split("|").getOrNull(1) }
                if (phoneNumbers.isNotEmpty()) {
                    sendSmsToContacts(phoneNumbers, message)
                }

                // 5. Initiate emergency phone call
                makeEmergencyCall(emergencyNumber)

            } catch (e: Exception) {
                Log.e("EmergencyActionManager", "SOS Sequence Failed", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocation(): String {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return context.getString(R.string.sos_loc_no_perm)
        return try {
            val location: Location? = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (location != null) "https://maps.google.com/?q=${location.latitude},${location.longitude}" else context.getString(R.string.sos_loc_unavail)
        } catch (e: Exception) {
            context.getString(R.string.sos_loc_error)
        }
    }

    private suspend fun recordAudio(): File? = suspendCancellableCoroutine { continuation ->
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            continuation.resume(null, null)
            return@suspendCancellableCoroutine
        }

        val audioFile = File(context.cacheDir, "sos_audio_${UUID.randomUUID()}.3gp")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            // Record for 5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    recorder.stop()
                    recorder.release()
                    continuation.resume(audioFile, null)
                } catch (e: Exception) {
                    continuation.resume(null, null)
                }
            }, 5000)
        } catch (e: Exception) {
            continuation.resume(null, null)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun captureBackgroundPhoto(): File? = suspendCancellableCoroutine { continuation ->
        if (!hasPermission(Manifest.permission.CAMERA)) {
            continuation.resume(null, null)
            return@suspendCancellableCoroutine
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.first()

            val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
            val photoFile = File(context.cacheDir, "sos_photo_${UUID.randomUUID()}.jpg")

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                FileOutputStream(photoFile).use { it.write(bytes) }
                image.close()
                reader.close()
                if (continuation.isActive) continuation.resume(photoFile, null)
            }, null)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    captureRequestBuilder.addTarget(imageReader.surface)

                    camera.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            session.capture(captureRequestBuilder.build(), null, null)
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            camera.close()
                            if (continuation.isActive) continuation.resume(null, null)
                        }
                    }, null)
                }
                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    if (continuation.isActive) continuation.resume(null, null)
                }
            }, null)

        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(null, null)
        }
    }

    private suspend fun uploadToFirebase(file: File, folder: String): String? {
        return try {
            val userId = auth.currentUser?.uid ?: "unknown_user"
            val ref = storage.reference.child("sos_media/$userId/$folder/${file.name}")
            ref.putFile(android.net.Uri.fromFile(file)).await()
            val downloadUrl = ref.downloadUrl.await()

            // Clean up cache to save space
            file.delete()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("EmergencyActionManager", "Firebase Upload Failed", e)
            null
        }
    }

    private fun sendSmsToContacts(phoneNumbers: List<String>, message: String) {
        if (!hasPermission(Manifest.permission.SEND_SMS)) return

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            for (number in phoneNumbers) {
                val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                if (cleanNumber.isNotEmpty()) {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e("EmergencyActionManager", "SMS Failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun makeEmergencyCall(number: String) {
        if (!hasPermission(Manifest.permission.CALL_PHONE)) return
        try {
            val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:$number")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
        } catch (e: Exception) {
            android.util.Log.e("EmergencyActionManager", "Call Failed", e)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}