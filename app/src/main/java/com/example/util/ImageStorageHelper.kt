package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {
    suspend fun saveImageLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        // Attempt to persist URI permission if supported by the provider
        try {
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Not all content providers support persistable permissions
        }

        try {
            val logoDir = File(context.filesDir, "channel_logos").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(logoDir, "channel_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            // Fallback to source URI string
            sourceUri.toString()
        }
    }
}
