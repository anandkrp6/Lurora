package com.bytecoder.lurora.backend.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bytecoder.lurora.backend.models.FeedbackSubmission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for sending feedback via Discord webhook
 */
@Singleton
class FeedbackService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // TODO: Replace with your actual Discord webhook URL
    private val webhookUrl = "YOUR_DISCORD_WEBHOOK_URL_HERE"

    /**
     * Submit feedback to Discord webhook
     */
    suspend fun submitFeedback(feedback: FeedbackSubmission): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("FeedbackService", "Submitting feedback: ${feedback.id}")

            val request = if (feedback.images.isNotEmpty()) {
                createMultipartRequest(feedback)
            } else {
                createJsonRequest(feedback)
            }

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d("FeedbackService", "Feedback submitted successfully: ${feedback.id}")
                Result.success(feedback.id)
            } else {
                val error = "Server error: ${response.code} ${response.message}"
                Log.e("FeedbackService", error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("FeedbackService", "Failed to submit feedback", e)
            Result.failure(e)
        }
    }

    private fun createJsonRequest(feedback: FeedbackSubmission): Request {
        val embedJson = JSONObject().apply {
            put("title", "📝 New Feedback: ${feedback.category.displayName}")
            put("description", feedback.message)
            put("color", when (feedback.category) {
                com.bytecoder.lurora.backend.models.FeedbackCategory.BUG_REPORT -> 15158332 // Red
                com.bytecoder.lurora.backend.models.FeedbackCategory.FEATURE_REQUEST -> 3447003 // Blue
                com.bytecoder.lurora.backend.models.FeedbackCategory.GENERAL_FEEDBACK -> 15105570 // Orange
            })
            put("timestamp", java.time.Instant.ofEpochMilli(feedback.timestamp).toString())
            put("fields", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "📱 Device Info")
                    put("value", "**Model:** ${feedback.deviceInfo.deviceManufacturer} ${feedback.deviceInfo.deviceModel}\\n" +
                              "**Android:** ${feedback.deviceInfo.androidVersion}\\n" +
                              "**App Version:** ${feedback.deviceInfo.appVersion}")
                    put("inline", true)
                })
                put(JSONObject().apply {
                    put("name", "🆔 Reference ID")
                    put("value", feedback.id)
                    put("inline", true)
                })
            })
            put("footer", JSONObject().apply {
                put("text", "Lurora Feedback System")
            })
        }

        val payload = JSONObject().apply {
            put("embeds", org.json.JSONArray().apply {
                put(embedJson)
            })
        }

        return Request.Builder()
            .url(webhookUrl)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun createMultipartRequest(feedback: FeedbackSubmission): Request {
        val tempFiles = mutableListOf<File>()
        
        // Copy images to temp files for upload
        feedback.images.forEachIndexed { index, image ->
            val inputStream = context.contentResolver.openInputStream(image.uri)
            val tempFile = File(context.cacheDir, "feedback_${feedback.id}_${index}_${image.fileName}")
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFiles.add(tempFile)
        }

        val embedJson = JSONObject().apply {
            put("title", "📝 New Feedback: ${feedback.category.displayName}")
            put("description", feedback.message)
            put("color", when (feedback.category) {
                com.bytecoder.lurora.backend.models.FeedbackCategory.BUG_REPORT -> 15158332
                com.bytecoder.lurora.backend.models.FeedbackCategory.FEATURE_REQUEST -> 3447003
                com.bytecoder.lurora.backend.models.FeedbackCategory.GENERAL_FEEDBACK -> 15105570
            })
            
            // Set first image as main embed image if available
            if (feedback.images.isNotEmpty()) {
                put("image", JSONObject().apply {
                    put("url", "attachment://0_${feedback.images[0].fileName}")
                })
            }
            
            put("timestamp", java.time.Instant.ofEpochMilli(feedback.timestamp).toString())
            put("fields", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "📱 Device Info")
                    put("value", "**Model:** ${feedback.deviceInfo.deviceManufacturer} ${feedback.deviceInfo.deviceModel}\\n" +
                              "**Android:** ${feedback.deviceInfo.androidVersion}\\n" +
                              "**App Version:** ${feedback.deviceInfo.appVersion}")
                    put("inline", true)
                })
                put(JSONObject().apply {
                    put("name", "🆔 Reference ID") 
                    put("value", feedback.id)
                    put("inline", true)
                })
                if (feedback.images.isNotEmpty()) {
                    put(JSONObject().apply {
                        put("name", "🖼️ Attachments")
                        put("value", feedback.images.mapIndexed { index, image -> 
                            "${index + 1}. ${image.fileName} (${String.format("%.1f", image.sizeMB)} MB)"
                        }.joinToString("\\n"))
                        put("inline", false)
                    })
                }
            })
            put("footer", JSONObject().apply {
                put("text", "Lurora Feedback System")
            })
        }

        val payload = JSONObject().apply {
            put("embeds", org.json.JSONArray().apply {
                put(embedJson)
            })
        }

        val requestBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", payload.toString())
        
        // Add all image files to the request
        tempFiles.forEachIndexed { index, tempFile ->
            val image = feedback.images[index]
            requestBodyBuilder.addFormDataPart(
                "file$index",
                "${index}_${image.fileName}",
                tempFile.asRequestBody(image.mimeType.toMediaType())
            )
        }

        return Request.Builder()
            .url(webhookUrl)
            .post(requestBodyBuilder.build())
            .build()
    }

    /**
     * Generate unique feedback ID
     */
    fun generateFeedbackId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val randomPart = (1..5)
            .map { chars.random() }
            .joinToString("")
        return "FB-$randomPart"
    }
}