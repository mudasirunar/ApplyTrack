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
import com.example.ui.jobaddedit.AddEditScreen
import com.example.ui.jobdetail.DetailScreen
import com.example.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.example.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    // Manual clean dependency injection container setup following recommended guidelines
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { JobRepositoryImpl(database.jobApplicationDao()) }
    
    private val viewModel: JobViewModel by viewModels {
        JobViewModelFactory(repository)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    Scaffold(
                        bottomBar = {
                            AnimatedVisibility(
                                visible = currentRoute == "dashboard" || currentRoute == "settings",
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(120)
                                ) + fadeIn(animationSpec = tween(120)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(120)
                                ) + fadeOut(animationSpec = tween(120))
                            ) {
                                val isDark = isSystemInDarkTheme()
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
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 4. Settings Screen
                        composable("settings") {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
                }
            }
        }
    }
}
