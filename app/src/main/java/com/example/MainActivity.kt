package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Manual clean dependency injection container setup following recommended guidelines
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { JobRepositoryImpl(database.jobApplicationDao()) }
    
    private val viewModel: JobViewModel by viewModels {
        JobViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "dashboard"
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
                    }
                }
            }
        }
    }
}
