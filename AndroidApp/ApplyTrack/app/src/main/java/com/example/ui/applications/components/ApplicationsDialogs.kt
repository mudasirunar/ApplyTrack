package com.example.ui.applications

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.model.JobApplication

@Composable
fun ApplicationsDialogs(
    jobToDelete: JobApplication?,
    onDismissDeleteSingle: () -> Unit,
    onConfirmDeleteSingle: (JobApplication) -> Unit,
    jobsToDeleteList: List<JobApplication>,
    onDismissDeleteList: () -> Unit,
    onConfirmDeleteList: (List<JobApplication>) -> Unit
) {
    if (jobToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteSingle,
            title = { Text(text = "Delete Application") },
            text = { Text(text = "Are you sure you want to delete this job application?") },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteSingle(jobToDelete) },
                    modifier = Modifier.testTag("delete_dialog_confirm")
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteSingle,
                    modifier = Modifier.testTag("delete_dialog_cancel")
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (jobsToDeleteList.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissDeleteList,
            title = { Text(text = "Delete Applications") },
            text = { 
                val textMsg = if (jobsToDeleteList.size == 1) {
                    "Are you sure you want to delete this job application?"
                } else {
                    "Are you sure you want to delete these ${jobsToDeleteList.size} job applications?"
                }
                Text(text = textMsg) 
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteList(jobsToDeleteList) },
                    modifier = Modifier.testTag("delete_dialog_confirm")
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteList,
                    modifier = Modifier.testTag("delete_dialog_cancel")
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
