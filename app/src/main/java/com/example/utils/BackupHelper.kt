package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.model.JobApplication
import com.example.model.StatusHistoryEntry
import com.example.model.Attachment
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupHelper {

    fun serializeApplications(apps: List<JobApplication>): String {
        val jsonArray = JSONArray()
        for (app in apps) {
            val jsonObject = JSONObject().apply {
                put("uuid", app.uuid)
                put("companyName", app.companyName)
                put("role", app.role)
                put("platform", app.platform)
                put("status", app.status)
                put("jobDescription", app.jobDescription)
                put("notes", app.notes)
                put("url", app.url)
                put("email", app.email)
                put("createdAt", app.createdAt)
                put("updatedAt", app.updatedAt)

                // Serialize statusHistory
                app.statusHistory?.let { history ->
                    val historyArray = JSONArray()
                    for (entry in history) {
                        val entryObj = JSONObject().apply {
                            put("status", entry.status)
                            put("timestamp", entry.timestamp)
                        }
                        historyArray.put(entryObj)
                    }
                    put("statusHistory", historyArray)
                }

                // Serialize attachments
                app.resume?.let { put("resume", serializeAttachment(it)) }
                app.coverLetter?.let { put("coverLetter", serializeAttachment(it)) }
                app.additionalDocument?.let { put("additionalDocument", serializeAttachment(it)) }
                app.screenshots?.let { list ->
                    val screenshotArray = JSONArray()
                    for (att in list) {
                        screenshotArray.put(serializeAttachment(att))
                    }
                    put("screenshots", screenshotArray)
                }
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString(2) // Pretty print
    }

    private fun serializeAttachment(attachment: Attachment): JSONObject {
        return JSONObject().apply {
            put("fileName", attachment.fileName)
            put("originalName", attachment.originalName)
        }
    }

    fun deserializeApplications(jsonStr: String): List<JobApplication> {
        val apps = mutableListOf<JobApplication>()
        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            
            // Deserialize statusHistory
            val historyList = mutableListOf<StatusHistoryEntry>()
            if (jsonObject.has("statusHistory")) {
                val historyArray = jsonObject.getJSONArray("statusHistory")
                for (j in 0 until historyArray.length()) {
                    val entryObj = historyArray.getJSONObject(j)
                    historyList.add(
                        StatusHistoryEntry(
                            status = entryObj.getString("status"),
                            timestamp = entryObj.getLong("timestamp")
                        )
                    )
                }
            }

            // Deserialize attachments
            val resume = if (jsonObject.has("resume")) deserializeAttachment(jsonObject.getJSONObject("resume")) else null
            val coverLetter = if (jsonObject.has("coverLetter")) deserializeAttachment(jsonObject.getJSONObject("coverLetter")) else null
            val additionalDocument = if (jsonObject.has("additionalDocument")) deserializeAttachment(jsonObject.getJSONObject("additionalDocument")) else null
            
            val screenshots = mutableListOf<Attachment>()
            if (jsonObject.has("screenshots")) {
                val screenshotArray = jsonObject.getJSONArray("screenshots")
                for (j in 0 until screenshotArray.length()) {
                    screenshots.add(deserializeAttachment(screenshotArray.getJSONObject(j)))
                }
            }

            val app = JobApplication(
                id = 0L, // Reset to let Room auto-generate
                uuid = jsonObject.getString("uuid"),
                companyName = jsonObject.optString("companyName").ifEmpty { null },
                role = jsonObject.optString("role").ifEmpty { null },
                platform = jsonObject.optString("platform").ifEmpty { null },
                status = jsonObject.getString("status"),
                jobDescription = jsonObject.optString("jobDescription").ifEmpty { null },
                notes = jsonObject.optString("notes").ifEmpty { null },
                url = jsonObject.optString("url").ifEmpty { null },
                email = jsonObject.optString("email").ifEmpty { null },
                statusHistory = historyList.ifEmpty { null },
                resume = resume,
                coverLetter = coverLetter,
                additionalDocument = additionalDocument,
                screenshots = screenshots.ifEmpty { null },
                createdAt = jsonObject.getLong("createdAt"),
                updatedAt = jsonObject.getLong("updatedAt")
            )
            apps.add(app)
        }
        return apps
    }

    private fun deserializeAttachment(obj: JSONObject): Attachment {
        return Attachment(
            fileName = obj.getString("fileName"),
            originalName = obj.getString("originalName")
        )
    }

    fun exportBackupToZip(context: Context, apps: List<JobApplication>, outputUri: Uri) {
        context.contentResolver.openOutputStream(outputUri)?.use { fos ->
            ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                // 1. Write data.json
                val jsonStr = serializeApplications(apps)
                val jsonBytes = jsonStr.toByteArray(Charsets.UTF_8)
                val jsonEntry = ZipEntry("data.json")
                zos.putNextEntry(jsonEntry)
                zos.write(jsonBytes)
                zos.closeEntry()

                // 2. Collect and write local files
                val fileNames = mutableSetOf<String>()
                for (app in apps) {
                    app.resume?.let { fileNames.add(it.fileName) }
                    app.coverLetter?.let { fileNames.add(it.fileName) }
                    app.additionalDocument?.let { fileNames.add(it.fileName) }
                    app.screenshots?.forEach { fileNames.add(it.fileName) }
                }

                val attachmentsDir = File(context.filesDir, "attachments")
                for (fileName in fileNames) {
                    val file = File(attachmentsDir, fileName)
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry(fileName))
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    fun importBackupFromZip(context: Context, inputUri: Uri): List<JobApplication> {
        var apps = emptyList<JobApplication>()
        val attachmentsDir = File(context.filesDir, "attachments")
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }

        context.contentResolver.openInputStream(inputUri)?.use { fis ->
            ZipInputStream(BufferedInputStream(fis)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "data.json") {
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        val jsonStr = baos.toString("UTF-8")
                        apps = deserializeApplications(jsonStr)
                    } else {
                        val destFile = File(attachmentsDir, name)
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return apps
    }

    fun readBackupApplicationsOnly(context: Context, uri: Uri): List<JobApplication> {
        var apps = emptyList<JobApplication>()
        context.contentResolver.openInputStream(uri)?.use { fis ->
            ZipInputStream(BufferedInputStream(fis)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "data.json") {
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        apps = deserializeApplications(baos.toString("UTF-8"))
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return apps
    }
}
