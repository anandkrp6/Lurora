package com.bytecoder.lurora.frontend.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Feedback screen with Discord webhook integration
 */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val context: Context = application
    
    // Discord webhook URL (in production, this should be securely stored)
    private val discordWebhookUrl = "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN"
    
    // UI State
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()
    
    // Temporary storage for attachments
    private val attachmentsList = mutableListOf<String>()
    
    /**
     * Submit feedback to Discord webhook
     */
    fun submitFeedback(
        feedback: String,
        email: String? = null,
        category: String,
        priority: String,
        includeSystemInfo: Boolean,
        includeLogs: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            try {
                val systemInfo = if (includeSystemInfo) getSystemInfo() else null
                val logData = if (includeLogs) getDebugLogs() else null
                
                val discordPayload = createDiscordPayload(
                    feedback = feedback,
                    email = email,
                    category = category,
                    priority = priority,
                    systemInfo = systemInfo,
                    logData = logData,
                    attachments = attachmentsList
                )
                
                // Send to Discord webhook
                val success = sendToDiscordWebhook(discordPayload)
                
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submissionResult = if (success) {
                        Result.success("Feedback sent successfully! Thank you for helping us improve Lurora.")
                    } else {
                        Result.failure(Exception("Failed to send feedback. Please try again or contact support directly."))
                    }
                )
                
                if (success) {
                    // Clear attachments on success
                    attachmentsList.clear()
                    updateAttachmentsList()
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submissionResult = Result.failure(e)
                )
            }
        }
    }
    
    /**
     * Add file attachments
     */
    fun addAttachments(uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                val fileName = getFileNameFromUri(uri) ?: "Unknown file"
                if (!attachmentsList.contains(fileName)) {
                    attachmentsList.add(fileName)
                }
            }
            updateAttachmentsList()
        }
    }
    
    /**
     * Remove attachment
     */
    fun removeAttachment(filename: String) {
        attachmentsList.remove(filename)
        updateAttachmentsList()
    }
    
    /**
     * Clear all attachments
     */
    fun clearAttachments() {
        attachmentsList.clear()
        updateAttachmentsList()
    }
    
    /**
     * Clear submission result
     */
    fun clearSubmissionResult() {
        _uiState.value = _uiState.value.copy(submissionResult = null)
    }
    
    /**
     * Update UI with current attachments list
     */
    private fun updateAttachmentsList() {
        _uiState.value = _uiState.value.copy(attachments = attachmentsList.toList())
    }
    
    /**
     * Get system information
     */
    private fun getSystemInfo(): SystemInfo {
        return SystemInfo(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = getAppVersion(),
            deviceId = Build.ID,
            architecture = Build.SUPPORTED_ABIS.joinToString(", "),
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get debug logs (simplified implementation)
     */
    private fun getDebugLogs(): String {
        return try {
            // In a real implementation, you would read from your log files
            // For now, return a placeholder
            "Debug logs would be attached here in production version"
        } catch (e: Exception) {
            "Failed to retrieve debug logs"
        }
    }
    
    /**
     * Get filename from URI
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
            "attachment_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "attachment_${System.currentTimeMillis()}"
        }
    }
    
    /**
     * Create Discord webhook payload as JSON string
     */
    private fun createDiscordPayload(
        feedback: String,
        email: String?,
        category: String,
        priority: String,
        systemInfo: SystemInfo?,
        logData: String?,
        attachments: List<String>
    ): String {
        val priorityColor = when (priority) {
            "High" -> 0xFF0000L // Red
            "Medium" -> 0xFFA500L // Orange
            "Low" -> 0x00FF00L // Green
            else -> 0x0099FFL // Blue
        }
        
        // Build JSON manually (simplified approach without kotlinx.serialization)
        val fieldsJson = buildString {
            append("[")
            append("""{"name":"📂 Category","value":"$category","inline":true},""")
            append("""{"name":"⚡ Priority","value":"$priority","inline":true}""")
            
            email?.let { 
                append(""",{"name":"📧 Email","value":"$it","inline":true}""") 
            }
            
            systemInfo?.let { info ->
                append(""",{"name":"📱 Device","value":"${info.deviceModel}","inline":true}""")
                append(""",{"name":"🤖 Android","value":"${info.androidVersion}","inline":true}""")
                append(""",{"name":"📦 App Version","value":"${info.appVersion}","inline":true}""")
                append(""",{"name":"🏗️ Architecture","value":"${info.architecture}","inline":false}""")
            }
            
            if (attachments.isNotEmpty()) {
                val attachmentList = attachments.joinToString("\\n• ", "• ")
                append(""",{"name":"📎 Attachments","value":"$attachmentList","inline":false}""")
            }
            
            logData?.let {
                val escapedLogs = it.replace("\"", "\\\"").replace("\n", "\\n")
                append(""",{"name":"🐛 Debug Info","value":"```\\n$escapedLogs\\n```","inline":false}""")
            }
            
            append("]")
        }
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        val escapedFeedback = feedback.replace("\"", "\\\"").replace("\n", "\\n")
        
        return """
            {
                "content": "🎯 **New Feedback Received**",
                "embeds": [
                    {
                        "title": "📱 New Lurora Feedback - $category",
                        "description": "$escapedFeedback",
                        "color": $priorityColor,
                        "fields": $fieldsJson,
                        "footer": {
                            "text": "Lurora Feedback System"
                        },
                        "timestamp": "$timestamp"
                    }
                ]
            }
        """.trimIndent()
    }
    
    /**
     * Send payload to Discord webhook (mock implementation)
     */
    private suspend fun sendToDiscordWebhook(payload: String): Boolean {
        return try {
            // In a real implementation, you would use a HTTP client like OkHttp or Retrofit
            // to send the JSON payload to the Discord webhook URL
            
            // Mock success for demonstration
            // In production, replace this with actual HTTP request:
            /*
            val client = OkHttpClient()
            val body = payload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(discordWebhookUrl)
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            return response.isSuccessful
            */
            
            // Simulate network delay
            kotlinx.coroutines.delay(2000)
            
            // Return true for success (in production, check actual HTTP response)
            true
            
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * UI state for the feedback screen
 */
data class FeedbackUiState(
    val isSubmitting: Boolean = false,
    val submissionResult: Result<String>? = null,
    val attachments: List<String> = emptyList()
)

/**
 * System information data class
 */
data class SystemInfo(
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val deviceId: String,
    val architecture: String,
    val timestamp: String
)