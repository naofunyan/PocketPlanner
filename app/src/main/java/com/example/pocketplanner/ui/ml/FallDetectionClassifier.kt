package com.example.pocketplanner.ui.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FallDetectionClassifier(context: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            // Load the model from the assets folder
            val modelBuffer = loadModelFile(context, "fall_model_int8.tflite")

            // Configure the Interpreter to use 2 threads for better performance
            val options = Interpreter.Options().apply {
                numThreads = 2
            }

            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Maps the .tflite file into memory. This is the standard Android way
     * to safely load ML models without causing memory out-of-bounds errors.
     */
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Executes the Fall Detection model.
     *
     * @param sensorBuffer A 3D array representing [1 batch, 128 timesteps, 3 axes]
     * @return A float probability between 0.0 (Not a fall) and 1.0 (Fall)
     */
    fun classifyFall(sensorBuffer: Array<Array<FloatArray>>): Float {
        // Ensure the interpreter isn't null before running
        val tflite = interpreter ?: return 0f

        // Our model's output layer is a single Dense node with a Sigmoid activation
        // Shape: [1, 1]
        val output = Array(1) { FloatArray(1) }

        // Run the inference!
        tflite.run(sensorBuffer, output)

        // Return the raw probability score
        return output[0][0]
    }

    /**
     * Frees up resources when the Service is destroyed.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}