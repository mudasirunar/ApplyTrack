package com.mudasir.applytrack.data.sync

import com.mudasir.applytrack.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SupabaseStorageHelper {
    private val client = OkHttpClient()

    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_KEY

    fun checkFileExists(userId: String, type: String, fileName: String): Boolean {
        if (anonKey.isBlank() || userId.isBlank() || fileName.isBlank()) return false
        val cleanBase = supabaseUrl.trimEnd('/')
        val encodedName = android.net.Uri.encode(fileName)
        val url = "$cleanBase/storage/v1/object/ApplyTrack/users/$userId/$type/$encodedName"
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
        if (anonKey.isBlank() || userId.isBlank() || fileName.isBlank()) return false
        val cleanBase = supabaseUrl.trimEnd('/')
        val encodedName = android.net.Uri.encode(fileName)
        val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val url = "$cleanBase/storage/v1/object/ApplyTrack/users/$userId/$type/$encodedName"
        
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
        if (anonKey.isBlank() || userId.isBlank() || fileName.isBlank()) return false
        val cleanBase = supabaseUrl.trimEnd('/')
        val encodedName = android.net.Uri.encode(fileName)
        val url = "$cleanBase/storage/v1/object/public/ApplyTrack/users/$userId/$type/$encodedName"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("apikey", anonKey)
            .build()
            
        val parent = destFile.parentFile ?: File(".")
        parent.mkdirs()
        val tempFile = File(parent, "${destFile.name}.tmp")
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        tempFile.outputStream().use { output ->
                            body.byteStream().copyTo(output)
                        }
                        if (tempFile.exists() && tempFile.length() > 0L) {
                            if (destFile.exists()) destFile.delete()
                            tempFile.renameTo(destFile)
                            true
                        } else {
                            if (tempFile.exists()) tempFile.delete()
                            false
                        }
                    } else false
                } else false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }

    fun deleteFile(userId: String, type: String, fileName: String): Boolean {
        if (anonKey.isBlank()) return false
        val fullPath = "users/$userId/$type/$fileName"
        val batchUrl = "$supabaseUrl/storage/v1/object/ApplyTrack"
        
        return try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val jsonBody = """{"prefixes":["$fullPath"]}""".toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url(batchUrl)
                .delete(jsonBody)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .build()
                
            val response = client.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()
            
            if (!successful) {
                // Fallback: direct path DELETE
                val directUrl = "$supabaseUrl/storage/v1/object/ApplyTrack/$fullPath"
                val directRequest = Request.Builder()
                    .url(directUrl)
                    .delete()
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("apikey", anonKey)
                    .build()
                client.newCall(directRequest).execute().use { it.isSuccessful }
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun wipeAllUserData(userId: String): Boolean {
        if (anonKey.isBlank() || userId.isBlank()) return false
        val folders = listOf("resumes", "cover_letters", "additional_documents", "screenshots", "documents")
        val allFilesToDelete = mutableListOf<String>()

        for (folder in folders) {
            try {
                val listUrl = "$supabaseUrl/storage/v1/object/list/ApplyTrack"
                val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val listBody = """{"prefix":"users/$userId/$folder","limit":500}""".toRequestBody(jsonMediaType)
                val listRequest = Request.Builder()
                    .url(listUrl)
                    .post(listBody)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("apikey", anonKey)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(listRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val jsonArray = org.json.JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val name = obj.optString("name")
                            val id = obj.optString("id")
                            if (name.isNotEmpty() && id.isNotEmpty()) {
                                allFilesToDelete.add("users/$userId/$folder/$name")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (allFilesToDelete.isNotEmpty()) {
            return try {
                val batchUrl = "$supabaseUrl/storage/v1/object/ApplyTrack"
                val prefixesJson = org.json.JSONArray(allFilesToDelete).toString()
                val jsonBody = """{"prefixes":$prefixesJson}""".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(batchUrl)
                    .delete(jsonBody)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("apikey", anonKey)
                    .addHeader("Content-Type", "application/json")
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return true
    }
}
