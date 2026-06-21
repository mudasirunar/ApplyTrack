package com.example.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import com.example.auth.AuthState
import com.example.auth.toUserFriendlyMessage
import com.example.ui.JobViewModel
import com.example.utils.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JobViewModel,
    authManager: AuthManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val dashboardAnalytics by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()
    val hasApplications = dashboardAnalytics.total > 0

    val scrollState = rememberScrollState()
    val scrollToTopEvent by viewModel.settingsScrollToTop.collectAsStateWithLifecycle()
    var lastScrollTrigger by remember { mutableStateOf(scrollToTopEvent) }

    LaunchedEffect(scrollToTopEvent) {
        if (scrollToTopEvent > lastScrollTrigger) {
            scrollState.animateScrollTo(0)
        }
        lastScrollTrigger = scrollToTopEvent
    }

    val authState by authManager.authState.collectAsStateWithLifecycle()
    val currentUser by authManager.currentUserFlow.collectAsStateWithLifecycle()

    var showAuthProgress by remember { mutableStateOf(false) }
    var authProgressMessage by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }

    val formatImportMessage: (Int, Int, Int) -> String = { importedCount, updatedCount, ignoredCount ->
        when {
            importedCount > 0 || updatedCount > 0 -> {
                val mainParts = mutableListOf<String>()
                if (importedCount > 0) {
                    mainParts.add(if (importedCount == 1) "1 new record" else "$importedCount new records")
                }
                if (updatedCount > 0) {
                    mainParts.add(if (updatedCount == 1) "1 updated record" else "$updatedCount updated records")
                }
                val importText = "Successfully imported " + mainParts.joinToString(" and ")
                if (ignoredCount > 0) {
                    val ignoreText = if (ignoredCount == 1) "1 duplicate record was ignored" else "$ignoredCount duplicate records were ignored"
                    "$importText. $ignoreText."
                } else {
                    "$importText."
                }
            }
            ignoredCount > 0 -> {
                val ignoreText = if (ignoredCount == 1) "1 duplicate record was ignored" else "$ignoredCount duplicate records were ignored"
                "No new records were imported. $ignoreText."
            }
            else -> {
                "All records in the backup file are already up to date. No new records were imported."
            }
        }
    }

    // Dialog states for reset confirmation
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var isWipingData by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    // Dialog states for import conflicts
    var showConflictDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var importConflictsCount by remember { mutableStateOf(0) }

    // Dialog states for backup progress
    var showProgressDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var operationSuccess by remember { mutableStateOf(true) }
    var dialogOutcome by remember { mutableStateOf(DialogOutcome.INFO) }

    // SAF CreateDocument Launcher for ZIP export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri: Uri? ->
            uri?.let {
                dialogTitle = "Exporting Backup"
                dialogMessage = "Creating backup file..."
                isWorking = true
                showProgressDialog = true
                dialogOutcome = DialogOutcome.INFO
                viewModel.exportBackup(
                    context = context,
                    uri = it,
                    onSuccess = {
                        isWorking = false
                        operationSuccess = true
                        dialogOutcome = DialogOutcome.SUCCESS
                        dialogMessage = "Successfully exported records and attachments to backup file!"
                    },
                    onError = { error ->
                        isWorking = false
                        operationSuccess = false
                        dialogOutcome = DialogOutcome.FAILURE
                        dialogMessage = "Failed to export backup: $error"
                    }
                )
            }
        }
    )

    // SAF OpenDocument Launcher for ZIP import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                dialogTitle = "Analyzing Backup"
                dialogMessage = "Scanning for changes..."
                isWorking = true
                showProgressDialog = true
                dialogOutcome = DialogOutcome.INFO
                viewModel.checkBackupConflicts(
                    context = context,
                    uri = it,
                    onResult = { conflictsCount ->
                        showProgressDialog = false
                        if (conflictsCount > 0) {
                            selectedImportUri = it
                            importConflictsCount = conflictsCount
                            showConflictDialog = true
                        } else {
                            // No conflicts, import with overwrite = false directly
                            dialogTitle = "Importing Backup"
                            isWorking = true
                            showProgressDialog = true
                            dialogOutcome = DialogOutcome.INFO
                            viewModel.importBackup(
                                context = context,
                                uri = it,
                                overwrite = false,
                                onProgress = { msg -> dialogMessage = msg },
                                onSuccess = { imported, updated, ignored ->
                                    isWorking = false
                                    operationSuccess = true
                                    dialogOutcome = DialogOutcome.SUCCESS
                                    dialogMessage = formatImportMessage(imported, updated, ignored)
                                },
                                onError = { error ->
                                    isWorking = false
                                    operationSuccess = false
                                    dialogOutcome = DialogOutcome.FAILURE
                                    dialogMessage = "Failed to import backup: $error"
                                }
                            )
                        }
                    },
                    onError = { error ->
                        isWorking = false
                        operationSuccess = false
                        dialogOutcome = DialogOutcome.FAILURE
                        dialogMessage = "Failed to analyze backup: $error"
                    }
                )
            }
        }
    )



    Scaffold(
        topBar = {
            key(MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountCard(
                authState = authState,
                currentUser = currentUser,
                onSignOutClick = {
                    showSignOutConfirmDialog = true
                },
                onLinkGoogleClick = {
                    showAuthProgress = true
                    authProgressMessage = "Signing in with Google..."
                    scope.launch {
                        val result = authManager.signInWithGoogle(context)
                        showAuthProgress = false
                        if (result.isFailure) {
                            val exception = result.exceptionOrNull()
                            val errorMsg = exception?.toUserFriendlyMessage()
                            if (errorMsg != null) {
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Sign-in successful!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            AppearanceCard(
                appTheme = appTheme,
                onThemeSelect = { theme -> viewModel.setAppTheme(theme) }
            )



            BackupManagementCard(
                hasApplications = hasApplications,
                onExportBackupClick = {
                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                    exportLauncher.launch("applytrack_backup_$dateStr.zip")
                },
                onImportBackupClick = {
                    importLauncher.launch(arrayOf("application/zip"))
                },
                onWipeAllClick = {
                    showResetConfirmDialog = true
                }
            )

            AboutCard()
        }
    }

    if (showResetConfirmDialog) {
        ResetConfirmDialog(
            isWiping = isWipingData,
            onDismiss = { showResetConfirmDialog = false },
            onConfirm = {
                isWipingData = true
                viewModel.clearAllLocalData(context) {
                    isWipingData = false
                    showResetConfirmDialog = false
                    Toast.makeText(context, "All local data wiped successfully. Remote cleanup started in background.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showSignOutConfirmDialog) {
        SignOutConfirmDialog(
            onDismiss = { showSignOutConfirmDialog = false },
            onConfirm = {
                showSignOutConfirmDialog = false
                showAuthProgress = true
                authProgressMessage = "Signing out..."
                scope.launch {
                    delay(500) 
                    val result = authManager.signOut()
                    showAuthProgress = false
                    if (result.isFailure) {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.localizedMessage ?: "Sign out failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    if (showConflictDialog) {
        ConflictDialog(
            importConflictsCount = importConflictsCount,
            onOverwriteClick = {
                showConflictDialog = false
                selectedImportUri?.let { uri ->
                    dialogTitle = "Importing Backup"
                    dialogMessage = "Restoring records..."
                    isWorking = true
                    showProgressDialog = true
                    dialogOutcome = DialogOutcome.INFO
                    viewModel.importBackup(
                        context = context,
                        uri = uri,
                        overwrite = true,
                        onProgress = { msg -> dialogMessage = msg },
                        onSuccess = { imported, updated, ignored ->
                            isWorking = false
                            operationSuccess = true
                            dialogOutcome = DialogOutcome.SUCCESS
                            dialogMessage = formatImportMessage(imported, updated, ignored)
                        },
                        onError = { error ->
                            isWorking = false
                            operationSuccess = false
                            dialogOutcome = DialogOutcome.FAILURE
                            dialogMessage = "Failed to import backup: $error"
                        }
                    )
                }
            },
            onKeepClick = {
                showConflictDialog = false
                selectedImportUri?.let { uri ->
                    dialogTitle = "Importing Backup"
                    dialogMessage = "Restoring records..."
                    isWorking = true
                    showProgressDialog = true
                    dialogOutcome = DialogOutcome.INFO
                    viewModel.importBackup(
                        context = context,
                        uri = uri,
                        overwrite = false,
                        onProgress = { msg -> dialogMessage = msg },
                        onSuccess = { imported, updated, ignored ->
                            isWorking = false
                            operationSuccess = true
                            dialogOutcome = DialogOutcome.SUCCESS
                            dialogMessage = formatImportMessage(imported, updated, ignored)
                        },
                        onError = { error ->
                            isWorking = false
                            operationSuccess = false
                            dialogOutcome = DialogOutcome.FAILURE
                            dialogMessage = "Failed to import backup: $error"
                        }
                    )
                }
            },
            onDismiss = { showConflictDialog = false }
        )
    }

    if (showProgressDialog) {
        BackupProgressDialog(
            dialogTitle = dialogTitle,
            dialogMessage = dialogMessage,
            isWorking = isWorking,
            dialogOutcome = dialogOutcome,
            onDismiss = { showProgressDialog = false }
        )
    }

    if (showAuthProgress) {
        AuthProgressDialog(authProgressMessage = authProgressMessage)
    }
}
