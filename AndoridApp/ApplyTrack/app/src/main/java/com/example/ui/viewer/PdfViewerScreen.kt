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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.example.utils.AttachmentHelper
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    fileName: String,
    originalName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(fileName) { AttachmentHelper.getAttachmentFile(context, fileName) }
    
    // Save current page across configuration changes
    var currentPage by rememberSaveable(fileName) { mutableIntStateOf(0) }

    val dividerPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#BDBDBD") // Solid grey color for visual separation
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f
        }
    }

    Scaffold(
        topBar = {
            key(MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    title = { Text(originalName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close PDF Viewer")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (file.exists()) {
                                try {
                                    val sharedDir = java.io.File(context.cacheDir, "shared_files")
                                    if (!sharedDir.exists()) sharedDir.mkdirs()
                                    val sharedFile = java.io.File(sharedDir, originalName)
                                    file.copyTo(sharedFile, overwrite = true)
                                    
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
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF")
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
            AndroidView<PDFView>(
                factory = { ctx ->
                    PDFView(ctx, null).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                        fromFile(file)
                            .defaultPage(currentPage)
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .enableDoubletap(true)
                            .scrollHandle(DefaultScrollHandle(context))
                            .spacing(16)
                            .onPageChange { page, _ ->
                                currentPage = page
                            }
                            .onDraw { canvas, pageWidth, pageHeight, displayedPage ->
                                if (pageCount > 1 && displayedPage < pageCount - 1) {
                                    canvas.drawLine(0f, pageHeight - 3f, pageWidth, pageHeight - 3f, dividerPaint)
                                }
                            }
                            .load()
                    }
                },
                update = {
                    // Empty to prevent document reloads during scrolling recompositions
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
