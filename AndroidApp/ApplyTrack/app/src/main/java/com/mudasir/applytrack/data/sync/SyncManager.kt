package com.mudasir.applytrack.data.sync

import com.mudasir.applytrack.ui.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncManager {
    val syncState: StateFlow<SyncState>
    val syncErrorMessage: StateFlow<String?>
    val downloadingFiles: StateFlow<Set<String>>

    fun startSync()
    fun stopSync()
    fun triggerUpload()
    suspend fun runFullSync(): Result<Unit>
    suspend fun migrateLocalDataToCloud()
    fun downloadJobAttachments(job: com.mudasir.applytrack.model.JobApplication)
}
