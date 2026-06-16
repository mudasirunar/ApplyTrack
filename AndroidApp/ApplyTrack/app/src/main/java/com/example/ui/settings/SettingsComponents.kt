package com.example.ui.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.auth.AuthState
import com.example.ui.SyncState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.WarningAmber
import com.example.utils.AppTheme

enum class DialogOutcome {
    SUCCESS, FAILURE, INFO
}

@Composable
fun AccountCard(
    authState: AuthState,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    onSignOutClick: () -> Unit,
    onLinkGoogleClick: () -> Unit
) {
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
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (authState == AuthState.AUTHENTICATED && currentUser != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val photoUrl = currentUser.photoUrl?.toString()?.replace("http://", "https://")
                    if (!photoUrl.isNullOrEmpty()) {
                        coil.compose.SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUrl)
                                .crossfade(true)
                                .build(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Default Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.displayName ?: "Google User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentUser.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onSignOutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Guest Avatar",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guest Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Log in with Google to back up your data to the cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onLinkGoogleClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = com.example.R.drawable.ic_google),
                        contentDescription = "Google Sign-In",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Link Google Account")
                }
            }
        }
    }
}

@Composable
fun AppearanceCard(
    appTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit
) {
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
                            .clickable { onThemeSelect(theme) }
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
}

@Composable
fun BackupManagementCard(
    hasApplications: Boolean,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onWipeAllClick: () -> Unit
) {
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
                    onClick = onExportBackupClick,
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
                    onClick = onImportBackupClick,
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
                        text = "Clears all job applications and attachments locally & remotely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onWipeAllClick,
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
}

@Composable
fun AboutCard() {
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
                    Image(
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

@Composable
fun ResetConfirmDialog(
    isWiping: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isWiping) {
                onDismiss()
            }
        },
        title = { Text(if (isWiping) "Wiping Data" else "Wipe All Saved Data") },
        text = {
            if (isWiping) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Wiping local database...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text("Are you absolutely sure you want to clear all job applications and attachments? This will delete all entries locally and remotely. This action cannot be undone.")
            }
        },
        confirmButton = {
            if (!isWiping) {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text("Wipe All", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            if (!isWiping) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun SignOutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign Out") },
        text = { Text("Are you sure you want to sign out?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Sign Out", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConflictDialog(
    importConflictsCount: Int,
    onOverwriteClick: () -> Unit,
    onKeepClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = onOverwriteClick
            ) {
                Text("Overwrite")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onKeepClick
            ) {
                Text("Keep")
            }
        }
    )
}

@Composable
fun BackupProgressDialog(
    dialogTitle: String,
    dialogMessage: String,
    isWorking: Boolean,
    dialogOutcome: DialogOutcome,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isWorking) {
                onDismiss()
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
                    onClick = onDismiss
                ) {
                    Text("OK")
                }
            }
        }
    )
}

@Composable
fun AuthProgressDialog(
    authProgressMessage: String
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.width(200.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = authProgressMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
