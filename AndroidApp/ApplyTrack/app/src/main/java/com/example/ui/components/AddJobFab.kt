package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun AddJobFab(
    visible: Boolean,
    bottomPadding: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
            .padding(end = 16.dp, bottom = bottomPadding)
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("add_job_fab"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New Job Application")
        }
    }
}
