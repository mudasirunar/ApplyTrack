package com.example.data.sync

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SupabaseStorageHelper {
    private val client = OkHttpClient()

    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_KEY

    fun checkFileExists(userId: String, type: String, fileName: String): Boolean {
        if (anonKey.isBlank()) return false
        val url = "$supabaseUrl/storage/v1/object/ApplyTrack/users/$userId/$type/$fileName"
        val request = Request.Builder()
            .url(url)
            .head()
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("apikey", anonKey)
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun uploadFile(userId: String, type: String, fileName: String, file: File): Boolean {
        if (anonKey.isBlank()) return false
        val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val url = "$supabaseUrl/storage/v1/object/ApplyTrack/users/$userId/$type/$fileName"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/octet-stream")
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun downloadFile(userId: String, type: String, fileName: String, destFile: File): Boolean {
        if (anonKey.isBlank()) return false
        // Since the bucket is public, we can fetch via GET to the public url
        val url = "$supabaseUrl/storage/v1/object/public/ApplyTrack/users/$userId/$type/$fileName"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("apikey", anonKey)
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().use { output ->
                            body.byteStream().copyTo(output)
                        }
                        true
                    } else false
                } else false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteFile(userId: String, type: String, fileName: String): Boolean {
        if (anonKey.isBlank()) return false
        val url = "$supabaseUrl/storage/v1/object/ApplyTrack/users/$userId/$type/$fileName"
        
        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("apikey", anonKey)
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
