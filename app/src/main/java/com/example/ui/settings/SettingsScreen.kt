package com.example.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JobViewModel
import com.example.ui.SyncState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.WarningAmber
import com.example.utils.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JobViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncError by viewModel.syncErrorMessage.collectAsStateWithLifecycle()
    val isFirebaseConfigured = viewModel.isFirebaseConfigured
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val hasApplications = dashboardStats.total > 0
    
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
                        isWorking = false
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

    LaunchedEffect(syncState) {
        if (syncState == SyncState.SUCCESS) {
            snackbarHostState.showSnackbar("Cloud Sync Completed Successfully!")
        } else if (syncState == SyncState.ERROR && syncError != null) {
            snackbarHostState.showSnackbar(syncError ?: "Sync Failed")
        }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Appearance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Select how ApplyTrack looks on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf(
                            AppTheme.SYSTEM to "System",
                            AppTheme.LIGHT to "Light",
                            AppTheme.DARK to "Dark"
                        )
                        themes.forEach { (theme, label) ->
                            val isSelected = appTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { viewModel.setAppTheme(theme) }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Firebase Cloud Sync Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cloud Synchronization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Configuration Banner Status
                    if (isFirebaseConfigured) {
                        Surface(
                            color = AccentGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Cloud Connected",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Cloud Sync Connected",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AccentGreen
                                    )
                                    Text(
                                        text = "Firebase Firestore configuration is active.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = WarningAmber.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Cloud Disconnected",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Local-Only Mode",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WarningAmber
                                    )
                                    Text(
                                        text = "Add 'google-services.json' to enable cloud sync.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Auto-sync Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = "Auto-Sync to Cloud",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically upload and update details in Firestore background task.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                            enabled = isFirebaseConfigured
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Active Sync Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync Status",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        val statusText = when (syncState) {
                            SyncState.IDLE -> "Idle"
                            SyncState.SYNCING -> "Syncing..."
                            SyncState.SUCCESS -> "Sync Successful"
                            SyncState.ERROR -> "Sync Failed"
                        }
                        val statusColor = when (syncState) {
                            SyncState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                            SyncState.SYNCING -> MaterialTheme.colorScheme.primary
                            SyncState.SUCCESS -> AccentGreen
                            SyncState.ERROR -> ErrorRed
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("sync_status_text")
                        )
                    }

                    // Sync action button
                    Button(
                        onClick = { viewModel.runFullSync() },
                        enabled = isFirebaseConfigured && syncState != SyncState.SYNCING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sync_now_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (syncState == SyncState.SYNCING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing Data...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Manual sync trigger"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Now")
                        }
                    }
                }
            }

            // Backup & Data Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Backup & Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Export all records along with attachments into a single ZIP archive, or restore them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                                exportLauncher.launch("applytrack_backup_$dateStr.zip")
                            },
                            enabled = hasApplications,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Export Backup")
                        }

                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/zip")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Import Backup")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Database Reset Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = "Wipe All Records",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Clears all applications and attachments locally & remotely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            enabled = hasApplications,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Wipe All")
                        }
                    }
                }
            }

            // About Application Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "About Application",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                                contentDescription = "App Logo",
                                modifier = Modifier.requiredSize(72.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "ApplyTrack",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "ApplyTrack is a streamlined, personal career records tracker designed for quick offline access and optional secure remote updates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Confirmation dialog for Reset/Wipe database
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Wipe All Saved Data") },
            text = { Text("Are you absolutely sure you want to clear all job applications and attachments? This will delete all entries locally and remotely. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.clearAllLocalData(context) {
                            Toast.makeText(context, "All local & remote data wiped successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Wipe All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Conflict confirmation dialog for backup import
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("Conflict Detected") },
            text = {
                Text(
                    if (importConflictsCount == 1) {
                        "1 of your saved applications has different details in the backup. Do you want to update it with the backup version or keep your current version?"
                    } else {
                        "$importConflictsCount of your saved applications have different details in the backup. Do you want to update them with the backup version or keep your current version?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
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
                    }
                ) {
                    Text("Overwrite")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
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
                    }
                ) {
                    Text("Ignore")
                }
            }
        )
    }

    // Progress and Result Dialogue for Backup Export/Import
    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    showProgressDialog = false
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dialogMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        when (dialogOutcome) {
                            DialogOutcome.SUCCESS -> {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFF4CAF50), shape = CircleShape), // Green
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            DialogOutcome.FAILURE -> {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFFF44336), shape = CircleShape), // Red
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Failure",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            DialogOutcome.INFO -> {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFFFFC107), shape = CircleShape), // Yellow / Amber
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Information",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dialogMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                if (!isWorking) {
                    TextButton(
                        onClick = {
                            showProgressDialog = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        )
    }
}

enum class DialogOutcome {
    SUCCESS, FAILURE, INFO
}
