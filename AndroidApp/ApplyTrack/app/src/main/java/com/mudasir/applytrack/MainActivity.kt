package com.mudasir.applytrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudasir.applytrack.auth.AuthManager
import com.mudasir.applytrack.auth.AuthState
import com.mudasir.applytrack.data.local.AppDatabase
import com.mudasir.applytrack.data.repository.JobRepositoryImpl
import com.mudasir.applytrack.data.sync.SyncManagerImpl
import com.mudasir.applytrack.ui.JobViewModel
import com.mudasir.applytrack.ui.JobViewModelFactory
import com.mudasir.applytrack.ui.login.LoginScreen
import com.mudasir.applytrack.ui.navigation.MainAppShell
import com.mudasir.applytrack.ui.theme.MyApplicationTheme
import com.mudasir.applytrack.utils.AppTheme
import com.mudasir.applytrack.utils.PreferencesHelper

/**
 * Main Activity entrypoint for ApplyTrack.
 * Responsibilities:
 * - Bootstraps core application dependencies and lifecycle observers.
 * - Handles top-level AuthState routing (Loading, Login, or MainAppShell).
 * - Delegates UI navigation and screen composition to the modular navigation layer.
 */
class MainActivity : ComponentActivity() {

    // Clean dependency container setup
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { JobRepositoryImpl(applicationContext, database.jobApplicationDao(), authManager) }
    private val preferencesHelper by lazy { PreferencesHelper(applicationContext) }
    private val authManager by lazy { AuthManager(applicationContext, preferencesHelper) }
    private val syncManager by lazy {
        SyncManagerImpl(
            context = applicationContext,
            repository = repository,
            dao = database.jobApplicationDao(),
            preferencesHelper = preferencesHelper,
            authManager = authManager
        )
    }

    private val viewModel: JobViewModel by viewModels {
        JobViewModelFactory(repository, preferencesHelper, syncManager, authManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start live sync manager
        syncManager.startSync()

        // Setup guest data upgrade callback
        authManager.onUserUpgradeCallback = {
            syncManager.migrateLocalDataToCloud()
        }

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
                    val authState by authManager.authState.collectAsStateWithLifecycle()
                    when (authState) {
                        AuthState.LOADING -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        AuthState.UNAUTHENTICATED -> {
                            LoginScreen(
                                onGoogleSignIn = { authManager.signInWithGoogle(this@MainActivity) },
                                onGuestSignIn = { authManager.signInAnonymously() }
                            )
                        }
                        AuthState.GUEST, AuthState.AUTHENTICATED -> {
                            MainAppShell(
                                viewModel = viewModel,
                                authManager = authManager,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.commitPendingDeleteBlocking()
        syncManager.stopSync()
    }
}
