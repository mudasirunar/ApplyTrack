package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.JobRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.example.utils.AttachmentHelper

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = JobRepositoryImpl(applicationContext, database.jobApplicationDao())
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null || currentUser.isAnonymous || !repository.isFirebaseConfigured()) {
            return Result.success()
        }

        val userId = currentUser.uid

        return try {
            val uploadResult = repository.uploadLocalChanges()
            val fetchResult = repository.fetchRemoteUpdates()

            if (fetchResult.isSuccess) {
                val supabaseStorageHelper = SupabaseStorageHelper()
                val apps = database.jobApplicationDao().getAllApplicationsList()
                for (job in apps) {
                    val downloads = listOfNotNull(
                        job.resume?.let { "resumes" to it.fileName },
                        job.coverLetter?.let { "cover_letters" to it.fileName },
                        job.additionalDocument?.let { "additional_documents" to it.fileName }
                    ) + (job.screenshots?.map { "screenshots" to it.fileName } ?: emptyList())

                    for ((type, fileName) in downloads) {
                        val destFile = AttachmentHelper.getAttachmentFile(applicationContext, fileName)
                        if (!destFile.exists()) {
                            try {
                                supabaseStorageHelper.downloadFile(userId, type, fileName, destFile)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            if (uploadResult.isSuccess && fetchResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
