package com.bytecoder.lurora.frontend.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.backend.services.FeedbackService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Comprehensive ViewModel for Feedback screen with full functionality
 */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackService: FeedbackService
) : ViewModel() {

    // UI State
    private val _selectedCategory = MutableStateFlow(FeedbackCategory.GENERAL_FEEDBACK)
    val selectedCategory: StateFlow<FeedbackCategory> = _selectedCategory.asStateFlow()

    private val _feedbackMessage = MutableStateFlow("")
    val feedbackMessage: StateFlow<String> = _feedbackMessage.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<FeedbackImage>>(emptyList())
    val selectedImages: StateFlow<List<FeedbackImage>> = _selectedImages.asStateFlow()

    private val _submissionState = MutableStateFlow<FeedbackSubmissionState>(FeedbackSubmissionState.Idle)
    val submissionState: StateFlow<FeedbackSubmissionState> = _submissionState.asStateFlow()

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    // Word counting
    val wordCount: StateFlow<Int> = MutableStateFlow(0).apply {
        viewModelScope.launch {
            feedbackMessage.collect { message ->
                val words = message.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                value = if (message.isBlank()) 0 else words.size
            }
        }
    }.asStateFlow()

    // Draft auto-save
    private var lastDraftSave = 0L
    private val draftSaveDelay = 2000L // 2 seconds

    init {
        // Auto-save draft while typing
        viewModelScope.launch {
            feedbackMessage.collect { message ->
                if (message.isNotBlank() && System.currentTimeMillis() - lastDraftSave > draftSaveDelay) {
                    saveDraft()
                    lastDraftSave = System.currentTimeMillis()
                }
            }
        }
    }

    // Computed properties for attachment size tracking
    val currentTotalSizeBytes: StateFlow<Long> = MutableStateFlow(0L).apply {
        viewModelScope.launch {
            selectedImages.collect { images ->
                value = images.sumOf { it.sizeBytes }
            }
        }
    }.asStateFlow()

    val currentTotalSizeMB: StateFlow<Float> = MutableStateFlow(0f).apply {
        viewModelScope.launch {
            currentTotalSizeBytes.collect { bytes ->
                value = bytes / (1024f * 1024f)
            }
        }
    }.asStateFlow()

    val remainingSizeBytes: StateFlow<Long> = MutableStateFlow(FeedbackImage.MAX_TOTAL_SIZE_BYTES).apply {
        viewModelScope.launch {
            currentTotalSizeBytes.collect { usedBytes ->
                value = FeedbackImage.MAX_TOTAL_SIZE_BYTES - usedBytes
            }
        }
    }.asStateFlow()

    val canAddMoreImages: StateFlow<Boolean> = MutableStateFlow(true).apply {
        viewModelScope.launch {
            remainingSizeBytes.collect { remainingBytes ->
                value = remainingBytes > 0
            }
        }
    }.asStateFlow()

    /**
     * Update selected feedback category
     */
    fun selectCategory(category: FeedbackCategory) {
        _selectedCategory.value = category
        saveDraft()
    }

    /**
     * Update feedback message
     */
    fun updateMessage(message: String) {
        _feedbackMessage.value = message
        
        // Real-time validation
        validateInput()
        
        // Trigger auto-save after delay
        viewModelScope.launch {
            delay(draftSaveDelay)
            if (_feedbackMessage.value == message) {
                saveDraft()
            }
        }
    }

    /**
     * Add image for feedback (size-based limit)
     */
    fun addImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(uri, null, null, null, null)
                
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        
                        val fileName = if (displayNameIndex >= 0) {
                            it.getString(displayNameIndex) ?: "image.jpg"
                        } else {
                            "image.jpg"
                        }
                        
                        val size = if (sizeIndex >= 0) {
                            it.getLong(sizeIndex)
                        } else {
                            0L
                        }
                        
                        // Check if adding this image would exceed total size limit
                        val currentTotalSize = _selectedImages.value.sumOf { img -> img.sizeBytes }
                        val newTotalSize = currentTotalSize + size
                        
                        if (newTotalSize > FeedbackImage.MAX_TOTAL_SIZE_BYTES) {
                            val currentTotalMB = currentTotalSize / (1024f * 1024f)
                            val newImageMB = size / (1024f * 1024f)
                            val maxTotalMB = FeedbackImage.MAX_TOTAL_SIZE_BYTES / (1024f * 1024f)
                            _validationErrors.value = listOf(
                                "Cannot add ${fileName} (${String.format("%.1f", newImageMB)}MB). " +
                                "Total size would be ${String.format("%.1f", newTotalSize / (1024f * 1024f))}MB. " +
                                "Maximum total is ${String.format("%.1f", maxTotalMB)}MB. " +
                                "Current total: ${String.format("%.1f", currentTotalMB)}MB"
                            )
                            return@launch
                        }
                        
                        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                        
                        val feedbackImage = FeedbackImage(
                            uri = uri,
                            fileName = fileName,
                            sizeBytes = size,
                            mimeType = mimeType
                        )
                        
                        _selectedImages.value = _selectedImages.value + feedbackImage
                        validateInput()
                        saveDraft()
                    }
                }
            } catch (e: Exception) {
                _validationErrors.value = listOf("Failed to process selected image")
            }
        }
    }

    /**
     * Remove specific image
     */
    fun removeImage(image: FeedbackImage) {
        _selectedImages.value = _selectedImages.value - image
        validateInput()
        saveDraft()
    }

    /**
     * Validate current input
     */
    private fun validateInput() {
        val validation = FeedbackValidation.validate(
            message = _feedbackMessage.value,
            images = _selectedImages.value
        )
        _validationErrors.value = validation.errors
    }

    /**
     * Submit feedback
     */
    fun submitFeedback(context: Context) {
        viewModelScope.launch {
            // Final validation
            val validation = FeedbackValidation.validate(
                message = _feedbackMessage.value,
                images = _selectedImages.value
            )
            
            if (!validation.isValid) {
                _validationErrors.value = validation.errors
                return@launch
            }

            _submissionState.value = FeedbackSubmissionState.Submitting
            _validationErrors.value = emptyList()

            try {
                val feedbackId = feedbackService.generateFeedbackId()
                
                val submission = FeedbackSubmission(
                    id = feedbackId,
                    category = _selectedCategory.value,
                    message = _feedbackMessage.value,
                    images = _selectedImages.value,
                    deviceInfo = getDeviceInfo(context)
                )

                val result = feedbackService.submitFeedback(submission)
                
                if (result.isSuccess) {
                    _submissionState.value = FeedbackSubmissionState.Success(feedbackId)
                    clearForm()
                    clearDraft()
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when {
                        error?.message?.contains("network", ignoreCase = true) == true ||
                        error?.message?.contains("connection", ignoreCase = true) == true ->
                            "Unable to send feedback. Check your internet connection."
                        error?.message?.contains("server", ignoreCase = true) == true ||
                        error?.message?.contains("5", ignoreCase = true) == true ->
                            "Something went wrong. Please try again."
                        else -> "Something went wrong. Please try again."
                    }
                    _submissionState.value = FeedbackSubmissionState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _submissionState.value = FeedbackSubmissionState.Error(
                    "Unable to send feedback. Check your internet connection."
                )
            }
        }
    }

    /**
     * Reset submission state (for retry)
     */
    fun resetSubmissionState() {
        _submissionState.value = FeedbackSubmissionState.Idle
    }

    /**
     * Clear the feedback form
     */
    private fun clearForm() {
        _selectedCategory.value = FeedbackCategory.GENERAL_FEEDBACK
        _feedbackMessage.value = ""
        _selectedImages.value = emptyList()
        _validationErrors.value = emptyList()
    }

    /**
     * Save draft to preferences (simplified implementation)
     */
    private fun saveDraft() {
        // TODO: Implement actual draft saving to SharedPreferences or Room
        // For now, this is a placeholder
    }

    /**
     * Clear saved draft
     */
    private fun clearDraft() {
        // TODO: Implement actual draft clearing
        // For now, this is a placeholder
    }

    /**
     * Get device information for feedback context
     */
    private fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            appVersion = try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            },
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER
        )
    }
}