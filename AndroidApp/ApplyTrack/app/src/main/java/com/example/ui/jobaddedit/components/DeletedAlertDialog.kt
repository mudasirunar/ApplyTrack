package com.example.ui.jobaddedit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties

@Composable
fun DeletedAlertDialog(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Application Deleted", fontWeight = FontWeight.ExtraBold) },
        text = { Text("This job application has been deleted from another device.") },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Return to Dashboard", fontWeight = FontWeight.Bold)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        modifier = modifier
    )
}
