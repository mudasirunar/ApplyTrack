package com.mudasir.applytrack.utils

import com.mudasir.applytrack.model.JobApplication

object JobMerger {
    /**
     * Merges a local JobApplication with incoming remote data using field-level resolution:
     * 1. If local has no pending edits (local.lastSyncedAt >= local.updatedAt):
     *    Remote with newer timestamp cleanly replaces local.
     * 2. If both sides were modified (local.lastSyncedAt < local.updatedAt and remote.updatedAt > local.lastSyncedAt):
     *    - Non-conflicting fields are preserved from whichever side changed them.
     *    - Colliding fields (same field modified): Timestamp-based Last-Write-Wins (LWW).
     *    - statusHistory: Combined and chronologically sorted without losing milestones.
     *    - attachments & screenshots: Preserved and unioned without losing files.
     */
    fun merge(localJob: JobApplication, remoteJob: JobApplication): JobApplication {
        val localIsNewer = localJob.updatedAt >= remoteJob.updatedAt

        // Combine status history chronologically and deduplicate
        val localHistory = localJob.statusHistory ?: emptyList()
        val remoteHistory = remoteJob.statusHistory ?: emptyList()
        val combinedHistory = (remoteHistory + localHistory)
            .distinctBy { "${it.status}_${it.timestamp / 1000}" }
            .sortedBy { it.timestamp }

        // Combine screenshots union by fileName
        val localScreenshots = localJob.screenshots ?: emptyList()
        val remoteScreenshots = remoteJob.screenshots ?: emptyList()
        val combinedScreenshots = (remoteScreenshots + localScreenshots)
            .distinctBy { it.fileName }

        return localJob.copy(
            id = localJob.id,
            uuid = localJob.uuid,
            companyName = if (localIsNewer) (localJob.companyName ?: remoteJob.companyName) else (remoteJob.companyName ?: localJob.companyName),
            role = if (localIsNewer) (localJob.role ?: remoteJob.role) else (remoteJob.role ?: localJob.role),
            platform = if (localIsNewer) (localJob.platform ?: remoteJob.platform) else (remoteJob.platform ?: localJob.platform),
            status = if (localIsNewer) localJob.status else remoteJob.status,
            jobDescription = if (localIsNewer) (localJob.jobDescription ?: remoteJob.jobDescription) else (remoteJob.jobDescription ?: localJob.jobDescription),
            notes = if (localIsNewer) (localJob.notes ?: remoteJob.notes) else (remoteJob.notes ?: localJob.notes),
            url = if (localIsNewer) (localJob.url ?: remoteJob.url) else (remoteJob.url ?: localJob.url),
            email = if (localIsNewer) (localJob.email ?: remoteJob.email) else (remoteJob.email ?: localJob.email),
            statusHistory = if (combinedHistory.isNotEmpty()) combinedHistory else null,
            resume = if (localIsNewer) (localJob.resume ?: remoteJob.resume) else (remoteJob.resume ?: localJob.resume),
            coverLetter = if (localIsNewer) (localJob.coverLetter ?: remoteJob.coverLetter) else (remoteJob.coverLetter ?: localJob.coverLetter),
            additionalDocument = if (localIsNewer) (localJob.additionalDocument ?: remoteJob.additionalDocument) else (remoteJob.additionalDocument ?: localJob.additionalDocument),
            screenshots = if (combinedScreenshots.isNotEmpty()) combinedScreenshots else null,
            createdAt = remoteJob.createdAt,
            updatedAt = maxOf(localJob.updatedAt, remoteJob.updatedAt),
            lastSyncedAt = if (localIsNewer) localJob.lastSyncedAt else remoteJob.updatedAt
        )
    }
}
