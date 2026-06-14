package com.example.data.sync

import com.example.ui.SyncState
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
}
