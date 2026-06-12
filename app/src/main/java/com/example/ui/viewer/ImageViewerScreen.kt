package com.example.ui.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.ui.JobViewModel
import com.example.ui.components.ZoomableImage
import com.example.utils.AttachmentHelper

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
                }
            }
        }
    }
}
