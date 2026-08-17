package com.example.pocketplanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.pocketplanner.data.local.entity.TripEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageGenerator {
    
    suspend fun generateTripPostcard(context: Context, trip: TripEntity): android.net.Uri? {
        val width = 1080
        val height = 1080
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background color fallback
        canvas.drawColor(Color.parseColor("#121212"))
        
        // Try to load the cover photo
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(trip.photoUrl)
            .size(width, height)
            .allowHardware(false) // We need a software bitmap to draw on it
            .build()
            
        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val drawable = result.drawable
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
        }
        
        // Draw Gradient Overlay for text readability
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, height * 0.4f, 0f, height.toFloat(),
            Color.TRANSPARENT, Color.parseColor("#E6000000"), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Draw Text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        // Draw Destination (Title)
        textPaint.textSize = 120f
        val destinationY = height - 250f
        canvas.drawText(trip.destination.take(15) + if (trip.destination.length > 15) "..." else "", 80f, destinationY, textPaint)
        
        // Draw Dates
        textPaint.textSize = 50f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val dateString = "${dateFormat.format(Date(trip.startDate))} - ${dateFormat.format(Date(trip.endDate))}"
        canvas.drawText(dateString, 80f, destinationY + 80f, textPaint)
        
        // Draw App Branding
        textPaint.textSize = 40f
        textPaint.color = Color.LTGRAY
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Planned with PocketPlanner", width - 80f, height - 80f, textPaint)
        
        // Save to cache
        return saveBitmapToCache(context, bitmap, trip.id)
    }
    
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, tripId: String): android.net.Uri? {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            val file = File(cachePath, "trip_postcard_$tripId.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()
            
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
