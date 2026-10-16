package com.mudasir.applytrack.ui.viewer

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
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
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudasir.applytrack.ui.JobViewModel
import com.mudasir.applytrack.utils.AttachmentHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    viewModel: JobViewModel,
    jobId: Long,
    initialIndex: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedApp by viewModel.selectedApplication.collectAsStateWithLifecycle()

    LaunchedEffect(jobId) {
        viewModel.loadApplicationById(jobId)
    }

    val screenshots = selectedApp?.screenshots ?: emptyList()

    if (selectedApp == null || selectedApp?.id != jobId) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (screenshots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No screenshots found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Persist current page index across configuration changes
    var currentPage by rememberSaveable(jobId) { mutableIntStateOf(initialIndex) }
    val pagerState = rememberPagerState(initialPage = currentPage, pageCount = { screenshots.size })
    var isZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
        currentPage = pagerState.currentPage
    }

    var pendingDownloadImage by remember { mutableStateOf<Pair<File, String>?>(null) }
    val downloadImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        val pending = pendingDownloadImage
        if (uri != null && pending != null) {
            val (file, name) = pending
            val success = AttachmentHelper.saveToUri(context, file, uri)
            if (success) {
                Toast.makeText(context, "Saved '$name' successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save '$name'", Toast.LENGTH_SHORT).show()
            }
        }
        pendingDownloadImage = null
    }

    Scaffold(
        topBar = {
            key(MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    title = { Text("Screenshot ${pagerState.currentPage + 1} of ${screenshots.size}") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Viewer")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val currentScreenshot = screenshots.getOrNull(pagerState.currentPage)
                            if (currentScreenshot != null) {
                                val file = AttachmentHelper.getAttachmentFile(context, currentScreenshot.fileName)
                                if (file.exists()) {
                                    val targetName = currentScreenshot.originalName.ifBlank { "screenshot_${pagerState.currentPage + 1}.jpg" }
                                    pendingDownloadImage = Pair(file, targetName)
                                    downloadImageLauncher.launch(targetName)
                                } else {
                                    Toast.makeText(context, "Image file not found locally", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Download Image")
                        }

                        IconButton(onClick = {
                            val currentScreenshot = screenshots.getOrNull(pagerState.currentPage)
                            if (currentScreenshot != null) {
                                val file = AttachmentHelper.getAttachmentFile(context, currentScreenshot.fileName)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                                }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share Image")
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
                .background(Color.Black)
        ) {
            @OptIn(ExperimentalFoundationApi::class)
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isZoomed,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val screenshot = screenshots[page]
                    val file = AttachmentHelper.getAttachmentFile(context, screenshot.fileName)
                    
                    if (file.exists()) {
                        ZoomableImage(
                            file = file,
                            isZoomed = (page == pagerState.currentPage) && isZoomed,
                            onZoomChanged = { zoomed ->
                                if (page == pagerState.currentPage) {
                                    isZoomed = zoomed
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Loading image...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
