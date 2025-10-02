package com.bytecoder.lurora.backend.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.bytecoder.lurora.backend.models.FileSystemItem
import java.io.File

/**
 * Utility class for sharing files
 */
object SharingUtils {
    
    /**
     * Share a single file
     */
    fun shareFile(context: Context, fileItem: FileSystemItem) {
        try {
            val file = File(fileItem.path)
            if (!file.exists()) {
                // File doesn't exist
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(fileItem.name)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val shareIntent = Intent.createChooser(intent, "Share ${fileItem.name}")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Handle error - could show a toast or log
        }
    }
    
    /**
     * Share multiple files
     */
    fun shareFiles(context: Context, fileItems: List<FileSystemItem>) {
        try {
            val uris = mutableListOf<Uri>()
            
            fileItems.forEach { fileItem ->
                val file = File(fileItem.path)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    uris.add(uri)
                }
            }
            
            if (uris.isEmpty()) {
                return
            }
            
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*" // Mixed content type for multiple files
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val shareIntent = Intent.createChooser(intent, "Share ${fileItems.size} files")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Get MIME type for a file
     */
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"
            "mp4", "avi", "mkv", "mov", "wmv", "flv" -> "video/*"
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> "audio/*"
            "txt", "log", "md" -> "text/*"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }
}