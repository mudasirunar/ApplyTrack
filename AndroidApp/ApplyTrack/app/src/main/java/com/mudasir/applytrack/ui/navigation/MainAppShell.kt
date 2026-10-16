package com.mudasir.applytrack.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mudasir.applytrack.auth.AuthManager
import com.mudasir.applytrack.auth.AuthState
import com.mudasir.applytrack.ui.JobViewModel
import com.mudasir.applytrack.ui.SyncState
import com.mudasir.applytrack.ui.components.JobSnackbarHost
import com.mudasir.applytrack.ui.components.SyncToast
import kotlinx.coroutines.delay

@Composable
fun MainAppShell(
    viewModel: JobViewModel,
    authManager: AuthManager,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authState by authManager.authState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isSearchFocused by viewModel.isSearchFocused.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val isFabVisible by viewModel.isFabVisible.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val pendingDeleteJobs by viewModel.pendingDeleteJobs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showToast by remember { mutableStateOf(false) }
    var toastState by remember { mutableStateOf(SyncState.IDLE) }

    // Sync Toast auto-dismiss handling
    LaunchedEffect(syncState, authState) {
        if (syncState != SyncState.IDLE && authState != AuthState.GUEST) {
            toastState = syncState
            showToast = true
            if (syncState == SyncState.SYNCING) {
                delay(3500)
                showToast = false
            } else if (syncState == SyncState.SUCCESS || syncState == SyncState.ERROR) {
                delay(2000)
                showToast = false
            }
        } else {
            showToast = false
        }
    }

    val isFabVisibleOnScreen = currentRoute == NavRoutes.APPLICATIONS && isFabVisible && !isSearchFocused && !isInitialLoading && !isSelectionModeActive
    val isBottomBarVisible = (currentRoute == NavRoutes.DASHBOARD || currentRoute == NavRoutes.APPLICATIONS || currentRoute == NavRoutes.SETTINGS) && !isSearchFocused && !isSelectionModeActive

    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val snackbarSpacing = 2.dp
    val fabSpacing = 8.dp
    val fabHeight = 56.dp
    val bottomBarHeight = 80.dp

    val snackbarBottomPadding = when {
        isFabVisibleOnScreen -> navigationBarsPadding + bottomBarHeight + fabSpacing + fabHeight + snackbarSpacing
        isBottomBarVisible -> navigationBarsPadding + bottomBarHeight + snackbarSpacing
        else -> navigationBarsPadding + snackbarSpacing
    }

    val animatedPadding by animateDpAsState(
        targetValue = snackbarBottomPadding,
        animationSpec = tween(durationMillis = 300),
        label = "SnackbarPaddingAnimation"
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe pending delete for Snackbar with Undo
    LaunchedEffect(pendingDeleteJobs) {
        if (pendingDeleteJobs.isNotEmpty()) {
            val snackbarMessage = if (pendingDeleteJobs.size == 1) {
                val roleName = pendingDeleteJobs.first().role.takeUnless { it.isNullOrBlank() } ?: "Application"
                "'$roleName' deleted"
            } else {
                "${pendingDeleteJobs.size} applications deleted"
            }
            val result = snackbarHostState.showSnackbar(
                message = snackbarMessage,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else if (result == SnackbarResult.Dismissed) {
                viewModel.commitPendingDelete()
            }
        }
    }

    // Commit delete if navigating to edit or detail screens
    LaunchedEffect(currentRoute) {
        if (currentRoute?.startsWith("add_edit") == true || currentRoute?.startsWith("detail") == true) {
            viewModel.commitPendingDelete()
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    // Commit delete if app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.commitPendingDeleteBlocking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                AppBottomBar(
                    visible = isBottomBarVisible,
                    currentRoute = currentRoute,
                    isDarkTheme = isDarkTheme,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onReselect = { route ->
                        viewModel.triggerScrollToTop(route)
                    }
                )
            }
        ) { innerPadding ->
            AppNavGraph(
                navController = navController,
                viewModel = viewModel,
                authManager = authManager
            )
        }

        JobSnackbarHost(
            hostState = snackbarHostState,
            bottomPadding = animatedPadding,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        SyncToast(
            visible = showToast,
            toastState = toastState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        )
    }
}
