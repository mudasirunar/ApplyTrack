package com.example.ui.viewer

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.example.utils.AttachmentHelper
import com.ahmer.pdfviewer.PDFView
import com.ahmer.pdfviewer.listener.OnDrawListener
import com.ahmer.pdfviewer.listener.OnErrorListener
import com.ahmer.pdfviewer.listener.OnLoadCompleteListener
import com.ahmer.pdfviewer.listener.OnPageChangeListener
import com.ahmer.pdfviewer.scroll.DefaultScrollHandle

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    fileName: String = "",
    originalName: String = "",
    fileUri: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(fileName) { AttachmentHelper.getAttachmentFile(context, fileName) }
    val hasValidSource = remember(file, fileUri) {
        file.exists() || (!fileUri.isNullOrBlank())
    }
    
    // Save current page across configuration changes
    var currentPage by rememberSaveable(fileName, fileUri) { mutableIntStateOf(0) }
    var pagesCount by remember { mutableIntStateOf(0) }
    var isPdfLoading by remember(fileName, fileUri) { mutableStateOf(true) }

    val dividerPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#BDBDBD") // Solid grey color for visual separation
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f
        }
    }

    val downloadPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = if (file.exists()) {
                AttachmentHelper.saveToUri(context, file, uri)
            } else if (!fileUri.isNullOrBlank()) {
                AttachmentHelper.saveUriToUri(context, Uri.parse(fileUri), uri)
            } else false

            if (success) {
                Toast.makeText(context, "Saved '$originalName' successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save '$originalName'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            key(MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    title = { Text(originalName.ifBlank { "Document Viewer" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close PDF Viewer")
                        }
                    },
                    actions = {
                        if (hasValidSource) {
                            IconButton(onClick = {
                                downloadPdfLauncher.launch(originalName.ifBlank { "document.pdf" })
                            }) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Download PDF")
                            }

                            IconButton(onClick = {
                                try {
                                    val sharedDir = java.io.File(context.cacheDir, "shared_files")
                                    if (!sharedDir.exists()) sharedDir.mkdirs()
                                    val targetSharedName = originalName.ifBlank { "document.pdf" }
                                    val sharedFile = java.io.File(sharedDir, targetSharedName)
                                    
                                    if (file.exists()) {
                                        file.copyTo(sharedFile, overwrite = true)
                                    } else if (!fileUri.isNullOrBlank()) {
                                        context.contentResolver.openInputStream(Uri.parse(fileUri))?.use { input ->
                                            sharedFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }
                                    
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        sharedFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            if (!hasValidSource) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Document not found locally or still syncing",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Please check your network connection or wait a moment for the document to finish downloading.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                AndroidView<PDFView>(
                    factory = { ctx ->
                        PDFView(ctx, null).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                            val configurator = if (!fileUri.isNullOrBlank()) {
                                fromUri(Uri.parse(fileUri))
                            } else {
                                fromFile(file)
                            }
                            configurator
                                .defaultPage(currentPage)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubleTap(true)
                                .scrollHandle(DefaultScrollHandle(context))
                                .spacing(16)
                                .onPageChange(object : OnPageChangeListener {
                                    override fun onPageChanged(page: Int, totalPages: Int) {
                                        currentPage = page
                                    }
                                })
                                .onLoad(object : OnLoadCompleteListener {
                                    override fun loadComplete(totalPages: Int) {
                                        pagesCount = totalPages
                                        isPdfLoading = false
                                    }
                                })
                                .onError(object : OnErrorListener {
                                    override fun onError(t: Throwable?) {
                                        isPdfLoading = false
                                    }
                                })
                                .onDraw(object : OnDrawListener {
                                    override fun onLayerDrawn(
                                        canvas: android.graphics.Canvas?,
                                        pageWidth: Float,
                                        pageHeight: Float,
                                        currentPage: Int
                                    ) {
                                        if (pagesCount > 1 && currentPage < pagesCount - 1) {
                                            canvas?.drawLine(0f, pageHeight - 3f, pageWidth, pageHeight - 3f, dividerPaint)
                                        }
                                    }
                                })
                                .load()
                        }
                    },
                    update = {
                        // Empty to prevent document reloads during scrolling recompositions
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isPdfLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Loading document...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
