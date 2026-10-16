package com.mudasir.applytrack.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mudasir.applytrack.auth.AuthManager
import com.mudasir.applytrack.ui.JobViewModel
import com.mudasir.applytrack.ui.applications.ApplicationsScreen
import com.mudasir.applytrack.ui.dashboard.DashboardScreen
import com.mudasir.applytrack.ui.jobaddedit.AddEditScreen
import com.mudasir.applytrack.ui.jobdetail.DetailScreen
import com.mudasir.applytrack.ui.settings.SettingsScreen
import com.mudasir.applytrack.ui.viewer.ImageViewerScreen
import com.mudasir.applytrack.ui.viewer.PdfViewerScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: JobViewModel,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.DASHBOARD,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {
        // 1. Dashboard Screen
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAdd = {
                    navController.navigate(NavRoutes.addEdit())
                },
                onNavigateToApplications = {
                    navController.navigate(NavRoutes.APPLICATIONS) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // 2. Applications List Screen
        composable(NavRoutes.APPLICATIONS) {
            ApplicationsScreen(
                viewModel = viewModel,
                onNavigateToAddEdit = { id ->
                    navController.navigate(NavRoutes.addEdit(id))
                },
                onNavigateToDetail = { id ->
                    navController.navigate(NavRoutes.detail(id))
                }
            )
        }

        // 3. Add/Edit Job Application Screen
        composable(
            route = NavRoutes.ADD_EDIT,
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
                },
                onNavigateToApplications = {
                    navController.navigate(NavRoutes.APPLICATIONS) {
                        popUpTo(NavRoutes.DASHBOARD) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToPdfViewer = { fileName, originalName, fileUri ->
                    navController.navigate(NavRoutes.pdfViewer(fileName, originalName, fileUri))
                }
            )
        }

        // 4. Job Detail Inspection Screen
        composable(
            route = NavRoutes.DETAIL,
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
                    navController.navigate(NavRoutes.addEdit(id))
                },
                onNavigateToPdfViewer = { fileName, originalName ->
                    navController.navigate(NavRoutes.pdfViewer(fileName, originalName))
                },
                onNavigateToImageViewer = { id, index ->
                    navController.navigate(NavRoutes.imageViewer(id, index))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToApplications = {
                    navController.navigate(NavRoutes.APPLICATIONS) {
                        popUpTo(NavRoutes.DASHBOARD) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 5. PDF Viewer Fullscreen Screen
        composable(
            route = NavRoutes.PDF_VIEWER,
            arguments = listOf(
                navArgument("fileName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("originalName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("fileUri") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawFileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val rawOriginalName = backStackEntry.arguments?.getString("originalName") ?: ""
            val rawFileUri = backStackEntry.arguments?.getString("fileUri") ?: ""

            val fileName = NavRoutes.decodeUrl(rawFileName)
            val originalName = NavRoutes.decodeUrl(rawOriginalName)
            val fileUri = NavRoutes.decodeUrl(rawFileUri).takeIf { it.isNotBlank() }

            PdfViewerScreen(
                fileName = fileName,
                originalName = originalName,
                fileUri = fileUri,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 6. Image Viewer Fullscreen Pager Screen
        composable(
            route = NavRoutes.IMAGE_VIEWER,
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

            ImageViewerScreen(
                viewModel = viewModel,
                jobId = jobId,
                initialIndex = initialIndex,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 7. Settings Screen
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                authManager = authManager
            )
        }
    }
}
