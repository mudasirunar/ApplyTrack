package com.example.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object AttachmentHelper {

    /**
     * Resolves the display name of a Uri using ContentResolver.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "unnamed_file"
    }

    /**
     * Extracts the extension of a filename.
     */
    fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot != -1 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * Copies a Uri stream into the app's local internal storage directory 'attachments'.
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri, destFileName: String): File? {
        val attachmentsDir = File(context.filesDir, "attachments")
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }
        val destFile = File(attachmentsDir, destFileName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a local attachment file by its unique filename.
     */
    fun deleteFile(context: Context, fileName: String): Boolean {
        val file = File(File(context.filesDir, "attachments"), fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Returns the File pointer to the attachment path.
     */
    fun getAttachmentFile(context: Context, fileName: String): File {
        return File(File(context.filesDir, "attachments"), fileName)
    }

    /**
     * Checks if a file exists locally in the attachments folder.
     */
    fun fileExists(context: Context, fileName: String): Boolean {
        return File(File(context.filesDir, "attachments"), fileName).exists()
    }

    /**
     * Clears all files in the attachments directory.
     */
    fun clearAllAttachments(context: Context) {
        val attachmentsDir = File(context.filesDir, "attachments")
        if (attachmentsDir.exists()) {
            attachmentsDir.deleteRecursively()
        }
    }
}
