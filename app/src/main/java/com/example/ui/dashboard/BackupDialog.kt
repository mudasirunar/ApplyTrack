package com.example.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.JobViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupDialog(
    viewModel: JobViewModel,
    onDismiss: () -> Unit
) {
    var backupTabSelected by remember { mutableStateOf(0) } // 0: Export, 1: Import
    val clipboardManager = LocalClipboardManager.current
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(false) }
    var importSuccess by remember { mutableStateOf(false) }

    val exportedJson = remember { viewModel.exportBackupJson() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 500.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                TabRow(selectedTabIndex = backupTabSelected) {
                    Tab(
                        selected = backupTabSelected == 0,
                        onClick = { backupTabSelected = 0 },
                        text = { Text("Export Data") }
                    )
                    Tab(
                        selected = backupTabSelected == 1,
                        onClick = { backupTabSelected = 1 },
                        text = { Text("Restore") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (backupTabSelected == 0) {
                        Column {
                            Text(
                                "Copy this JSON block to back up all your applications locally:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = exportedJson,
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(exportedJson))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Copy JSON Backup")
                            }
                        }
                    } else {
                        Column {
                            Text(
                                "Paste your JSON backup data below to restore applications:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = importJsonText,
                                onValueChange = {
                                    importJsonText = it
                                    importError = false
                                    importSuccess = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                placeholder = { Text("[{...}]") },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (importError) {
                                Text(
                                    "Error: Invalid JSON format. Restore failed.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (importSuccess) {
                                Text(
                                    "Data restored successfully!",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val ok = viewModel.restoreBackupJson(importJsonText)
                                    if (ok) {
                                        importSuccess = true
                                        importJsonText = ""
                                    } else {
                                        importError = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Restore Backup")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
