package com.example.ui.applications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformSubFilterPanel(
    statusFilter: String,
    selectedResume: String,
    onResumeChange: (String) -> Unit,
    selectedPlatform: String,
    onPlatformChange: (String) -> Unit,
    resumeSearchQuery: String,
    onResumeSearchQueryChange: (String) -> Unit,
    resumeNames: List<String>
) {
    // Resume Sub-filter controls
    AnimatedVisibility(
        visible = statusFilter == "Resume",
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        var isResumeDropdownExpanded by remember { mutableStateOf(false) }
        val filteredResumes = resumeNames.filter { it.contains(resumeSearchQuery, ignoreCase = true) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (selectedResume == "Select---") "Select---" else {
                        selectedResume.let { n ->
                            when {
                                n.endsWith(".pdf", ignoreCase = true) -> n.dropLast(4)
                                n.endsWith(".docx", ignoreCase = true) -> n.dropLast(5)
                                n.endsWith(".doc", ignoreCase = true) -> n.dropLast(4)
                                else -> n
                            }
                        }
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Resume / CV") },
                    trailingIcon = {
                        IconButton(onClick = { isResumeDropdownExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Open Resume Dropdown")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Clickable overlay to open dropdown
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isResumeDropdownExpanded = true }
                )
            }

            DropdownMenu(
                expanded = isResumeDropdownExpanded,
                onDismissRequest = { 
                    isResumeDropdownExpanded = false
                    onResumeSearchQueryChange("")
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 280.dp)
            ) {
                // Search Input inside Dropdown
                OutlinedTextField(
                    value = resumeSearchQuery,
                    onValueChange = onResumeSearchQueryChange,
                    placeholder = { Text("Search resumes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (resumeSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onResumeSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                DropdownMenuItem(
                    text = { Text("Select---") },
                    onClick = {
                        onResumeChange("Select---")
                        isResumeDropdownExpanded = false
                        onResumeSearchQueryChange("")
                    }
                )

                if (filteredResumes.isEmpty()) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = "No resumes found", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        onClick = {},
                        enabled = false
                    )
                } else {
                    filteredResumes.forEach { name ->
                        val displayName = name.let { n ->
                            when {
                                n.endsWith(".pdf", ignoreCase = true) -> n.dropLast(4)
                                n.endsWith(".docx", ignoreCase = true) -> n.dropLast(5)
                                n.endsWith(".doc", ignoreCase = true) -> n.dropLast(4)
                                else -> n
                            }
                        }
                        DropdownMenuItem(
                            text = { Text(displayName) },
                            onClick = {
                                onResumeChange(name)
                                isResumeDropdownExpanded = false
                                onResumeSearchQueryChange("")
                            }
                        )
                    }
                }
            }
        }
    }

    // Platform Sub-filter controls
    AnimatedVisibility(
        visible = statusFilter == "Platform",
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        var isPlatformDropdownExpanded by remember { mutableStateOf(false) }
        val platformOptions = listOf("LinkedIn", "Indeed", "Email", "Website", "Other")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = selectedPlatform,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Platform") },
                trailingIcon = {
                    IconButton(onClick = { isPlatformDropdownExpanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Open Platform Dropdown")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Clickable overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isPlatformDropdownExpanded = true }
            )

            DropdownMenu(
                expanded = isPlatformDropdownExpanded,
                onDismissRequest = { isPlatformDropdownExpanded = false }
            ) {
                platformOptions.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onPlatformChange(name)
                            isPlatformDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}
