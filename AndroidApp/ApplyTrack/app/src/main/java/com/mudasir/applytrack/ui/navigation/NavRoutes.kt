package com.mudasir.applytrack.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Navigation destination routes and type-safe URL builders for ApplyTrack.
 */
object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val APPLICATIONS = "applications"
    const val SETTINGS = "settings"

    const val ADD_EDIT = "add_edit?jobId={jobId}"
    const val DETAIL = "detail/{jobId}"
    const val PDF_VIEWER = "pdf_viewer?fileName={fileName}&originalName={originalName}&fileUri={fileUri}"
    const val IMAGE_VIEWER = "image_viewer?jobId={jobId}&initialIndex={initialIndex}"

    fun addEdit(jobId: Long? = null): String {
        return if (jobId != null) "add_edit?jobId=$jobId" else "add_edit"
    }

    fun detail(jobId: Long): String {
        return "detail/$jobId"
    }

    fun pdfViewer(fileName: String?, originalName: String?, fileUri: String? = null): String {
        val encFileName = URLEncoder.encode(fileName ?: "", "UTF-8")
        val encOriginalName = URLEncoder.encode(originalName ?: "", "UTF-8")
        val encFileUri = URLEncoder.encode(fileUri ?: "", "UTF-8")
        return "pdf_viewer?fileName=$encFileName&originalName=$encOriginalName&fileUri=$encFileUri"
    }

    fun imageViewer(jobId: Long, initialIndex: Int): String {
        return "image_viewer?jobId=$jobId&initialIndex=$initialIndex"
    }

    fun decodeUrl(param: String?): String {
        return if (param.isNullOrBlank()) "" else URLDecoder.decode(param, "UTF-8")
    }
}
