package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun JobSnackbarHost(
    hostState: SnackbarHostState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .padding(bottom = bottomPadding)
    )
}
