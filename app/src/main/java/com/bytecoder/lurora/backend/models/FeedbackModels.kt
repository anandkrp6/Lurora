package com.bytecoder.lurora.backend.models

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Feedback category types
 */
enum class FeedbackCategory(val displayName: String) {
    BUG_REPORT("Bug Report"),
    FEATURE_REQUEST("Feature Request"),
    GENERAL_FEEDBACK("General Feedback")
}

/**
 * Feedback submission state
 */
sealed class FeedbackSubmissionState {
    object Idle : FeedbackSubmissionState()
    object Submitting : FeedbackSubmissionState()
    data class Success(val feedbackId: String) : FeedbackSubmissionState()
    data class Error(val message: String, val canRetry: Boolean = true) : FeedbackSubmissionState()
}

/**
 * Image attachment data
 */
@Immutable
data class FeedbackImage(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String
) {
    val sizeMB: Float get() = sizeBytes / (1024f * 1024f)
    val isValidSize: Boolean get() = sizeBytes <= MAX_FILE_SIZE_BYTES
    val isValidType: Boolean get() = mimeType in SUPPORTED_MIME_TYPES
    
    companion object {
        const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024L // 25MB per file (Discord free limit)
        const val MAX_TOTAL_SIZE_BYTES = 25 * 1024 * 1024L // 25MB total for all attachments (Discord free limit)
        val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/webp"
        )
    }
}

/**
 * Complete feedback data for submission
 */
@Immutable
data class FeedbackSubmission(
    val id: String,
    val category: FeedbackCategory,
    val message: String,
    val images: List<FeedbackImage> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val deviceInfo: DeviceInfo
)

/**
 * Device information for feedback context
 */
@Immutable
data class DeviceInfo(
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
    val deviceManufacturer: String
)

/**
 * Validation result for feedback
 */
data class FeedbackValidation(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    companion object {
        const val MIN_WORDS = 10
        const val MAX_WORDS = 500
        
        fun validate(
            message: String,
            images: List<FeedbackImage>
        ): FeedbackValidation {
            val errors = mutableListOf<String>()
            
            // Validate message
            val words = message.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            when {
                message.isBlank() -> errors.add("Please describe your feedback before submitting")
                words.size < MIN_WORDS -> errors.add("Please provide at least $MIN_WORDS words in your feedback")
                words.size > MAX_WORDS -> errors.add("Please keep your feedback under $MAX_WORDS words (currently: ${words.size} words)")
            }
            
            // Validate images if provided (size-based limit)
            val totalSizeBytes = images.sumOf { it.sizeBytes }
            val totalSizeMB = totalSizeBytes / (1024f * 1024f)
            
            if (totalSizeBytes > FeedbackImage.MAX_TOTAL_SIZE_BYTES) {
                errors.add("Total attachment size must be under 25MB (current: ${String.format("%.1f", totalSizeMB)}MB)")
            }
            
            images.forEach { img ->
                when {
                    !img.isValidSize -> errors.add("Image ${img.fileName} must be under 25MB (current: ${String.format("%.1f", img.sizeMB)}MB)")
                    !img.isValidType -> errors.add("${img.fileName} must be JPG, PNG, or WebP format")
                }
            }
            
            return FeedbackValidation(
                isValid = errors.isEmpty(),
                errors = errors
            )
        }
    }
}

/**
 * Draft feedback for auto-save
 */
@Immutable
data class FeedbackDraft(
    val category: FeedbackCategory = FeedbackCategory.GENERAL_FEEDBACK,
    val message: String = "",
    val imageUris: List<String> = emptyList(),
    val lastSaved: Long = System.currentTimeMillis()
)