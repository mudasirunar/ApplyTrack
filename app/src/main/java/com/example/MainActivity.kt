package com.example

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.JobRepositoryImpl
import com.example.ui.JobViewModel
import com.example.ui.JobViewModelFactory
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.applications.ApplicationsScreen
import com.example.ui.jobaddedit.AddEditScreen
import com.example.ui.jobdetail.DetailScreen
import com.example.ui.theme.*
import com.example.utils.PreferencesHelper
import com.example.utils.AppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.settings.SettingsScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateDpAsState

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // Manual clean dependency injection container setup following recommended guidelines
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { JobRepositoryImpl(database.jobApplicationDao()) }
    private val preferencesHelper by lazy { PreferencesHelper(applicationContext) }
    
    private val viewModel: JobViewModel by viewModels {
        JobViewModelFactory(repository, preferencesHelper)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val isDarkTheme = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isSearchFocused by viewModel.isSearchFocused.collectAsStateWithLifecycle()
                    val snackbarHostState = remember { SnackbarHostState() }
                    val isFabVisible by viewModel.isFabVisible.collectAsStateWithLifecycle()
                    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
                    val isFabVisibleOnScreen = currentRoute == "applications" && isFabVisible && !isSearchFocused && !isInitialLoading
                    val isBottomBarVisible = (currentRoute == "dashboard" || currentRoute == "applications" || currentRoute == "settings") && !isSearchFocused
                    
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
                    
                    val pendingDeleteJob by viewModel.pendingDeleteJob.collectAsStateWithLifecycle()
                    val lifecycleOwner = LocalLifecycleOwner.current

                    // Observe pending delete for Snackbar
                    LaunchedEffect(pendingDeleteJob) {
                        pendingDeleteJob?.let { job ->
                            val roleName = job.role.takeUnless { it.isNullOrBlank() } ?: "Application"
                            val result = snackbarHostState.showSnackbar(
                                message = "'$roleName' deleted",
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
                                viewModel.commitPendingDelete()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            bottomBar = {
                                AnimatedVisibility(
                                    visible = isBottomBarVisible,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(120)
                                ) + fadeIn(animationSpec = tween(120)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(120)
                                ) + fadeOut(animationSpec = tween(120))
                            ) {
                                val isDark = isDarkTheme
                                val navContainer = if (isDark) DarkNavContainer else LightNavContainer
                                val navSelectedIcon = if (isDark) DarkNavSelectedIcon else LightNavSelectedIcon
                                val navSelectedText = if (isDark) DarkNavSelectedText else LightNavSelectedText
                                val navSelectedIndicator = if (isDark) DarkNavSelectedIndicator else LightNavSelectedIndicator
                                val navUnselected = if (isDark) DarkNavUnselected else LightNavUnselected

                                NavigationBar(
                                    containerColor = navContainer,
                                    tonalElevation = 0.dp
                                ) {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                        label = { Text("Dashboard") },
                                        selected = currentRoute == "dashboard",
                                        onClick = {
                                            if (currentRoute != "dashboard") {
                                                navController.navigate("dashboard") {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = navSelectedIcon,
                                            selectedTextColor = navSelectedText,
                                            unselectedIconColor = navUnselected,
                                            unselectedTextColor = navUnselected,
                                            indicatorColor = navSelectedIndicator
                                        ),
                                        modifier = Modifier.testTag("nav_home")
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Applications") },
                                        label = { Text("Applications") },
                                        selected = currentRoute == "applications",
                                        onClick = {
                                            if (currentRoute != "applications") {
                                                navController.navigate("applications") {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = navSelectedIcon,
                                            selectedTextColor = navSelectedText,
                                            unselectedIconColor = navUnselected,
                                            unselectedTextColor = navUnselected,
                                            indicatorColor = navSelectedIndicator
                                        ),
                                        modifier = Modifier.testTag("nav_applications")
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") },
                                        selected = currentRoute == "settings",
                                        onClick = {
                                            if (currentRoute != "settings") {
                                                navController.navigate("settings") {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = navSelectedIcon,
                                            selectedTextColor = navSelectedText,
                                            unselectedIconColor = navUnselected,
                                            unselectedTextColor = navUnselected,
                                            indicatorColor = navSelectedIndicator
                                        ),
                                        modifier = Modifier.testTag("nav_settings")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            enterTransition = { fadeIn(animationSpec = tween(100)) },
                            exitTransition = { fadeOut(animationSpec = tween(100)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(100)) },
                            popExitTransition = { fadeOut(animationSpec = tween(100)) }
                        ) {
                        // 1. Dashboard Landing screen
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = {
                                    navController.navigate("add_edit")
                                }
                            )
                        }

                        // Applications List Screen
                        composable("applications") {
                            ApplicationsScreen(
                                viewModel = viewModel,
                                onNavigateToAddEdit = { id ->
                                    val route = if (id != null) "add_edit?jobId=$id" else "add_edit"
                                    navController.navigate(route)
                                },
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                }
                            )
                        }

                        // 2. Add/Edit Job details form screen (with optional argument jobId)
                        composable(
                            route = "add_edit?jobId={jobId}",
                            arguments = listOf(
                                navArgument("jobId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val jobIdString = backStackEntry.arguments?.getString("jobId")
                            val jobId = jobIdString?.toLongOrNull()

                            AddEditScreen(
                                viewModel = viewModel,
                                jobId = jobId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 3. Job Detail full inspection screen
                        composable(
                            route = "detail/{jobId}",
                            arguments = listOf(
                                navArgument("jobId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->
                            val jobId = backStackEntry.arguments?.getLong("jobId") ?: 0L

                            DetailScreen(
                                viewModel = viewModel,
                                jobId = jobId,
                                onNavigateToEdit = { id ->
                                    navController.navigate("add_edit?jobId=$id")
                                },
                                onNavigateToPdfViewer = { fileName, originalName ->
                                    navController.navigate("pdf_viewer?fileName=$fileName&originalName=$originalName")
                                },
                                onNavigateToImageViewer = { id, index ->
                                    navController.navigate("image_viewer?jobId=$id&initialIndex=$index")
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 4. PDF Viewer fullscreen screen
                        composable(
                            route = "pdf_viewer?fileName={fileName}&originalName={originalName}",
                            arguments = listOf(
                                navArgument("fileName") {
                                    type = NavType.StringType
                                },
                                navArgument("originalName") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                            val originalName = backStackEntry.arguments?.getString("originalName") ?: ""
                            com.example.ui.viewer.PdfViewerScreen(
                                fileName = fileName,
                                originalName = originalName,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 5. Image Viewer fullscreen pager screen
                        composable(
                            route = "image_viewer?jobId={jobId}&initialIndex={initialIndex}",
                            arguments = listOf(
                                navArgument("jobId") {
                                    type = NavType.LongType
                                },
                                navArgument("initialIndex") {
                                    type = NavType.IntType
                                }
                            )
                        ) { backStackEntry ->
                            val jobId = backStackEntry.arguments?.getLong("jobId") ?: 0L
                            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
                            com.example.ui.viewer.ImageViewerScreen(
                                viewModel = viewModel,
                                jobId = jobId,
                                initialIndex = initialIndex,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 6. Settings Screen
                        composable("settings") {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = animatedPadding)
                )
            }
                }
            }
        }
    }
}
