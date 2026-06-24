package com.example.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ConflictDialog(
    importConflictsCount: Int,
    onOverwriteClick: () -> Unit,
    onKeepClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
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
        },
        modifier = modifier
    )
}
