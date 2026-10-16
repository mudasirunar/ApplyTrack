package com.mudasir.applytrack.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mudasir.applytrack.ui.theme.DarkNavContainer
import com.mudasir.applytrack.ui.theme.DarkNavSelectedIcon
import com.mudasir.applytrack.ui.theme.DarkNavSelectedIndicator
import com.mudasir.applytrack.ui.theme.DarkNavSelectedText
import com.mudasir.applytrack.ui.theme.DarkNavUnselected
import com.mudasir.applytrack.ui.theme.LightNavContainer
import com.mudasir.applytrack.ui.theme.LightNavSelectedIcon
import com.mudasir.applytrack.ui.theme.LightNavSelectedIndicator
import com.mudasir.applytrack.ui.theme.LightNavSelectedText
import com.mudasir.applytrack.ui.theme.LightNavUnselected

@Composable
fun AppBottomBar(
    visible: Boolean,
    currentRoute: String?,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onReselect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(120)
        ) + fadeIn(animationSpec = tween(120)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(120)
        ) + fadeOut(animationSpec = tween(120))
    ) {
        val navContainer = if (isDarkTheme) DarkNavContainer else LightNavContainer
        val navSelectedIcon = if (isDarkTheme) DarkNavSelectedIcon else LightNavSelectedIcon
        val navSelectedText = if (isDarkTheme) DarkNavSelectedText else LightNavSelectedText
        val navSelectedIndicator = if (isDarkTheme) DarkNavSelectedIndicator else LightNavSelectedIndicator
        val navUnselected = if (isDarkTheme) DarkNavUnselected else LightNavUnselected

        NavigationBar(
            containerColor = navContainer,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Dashboard") },
                selected = currentRoute == NavRoutes.DASHBOARD,
                onClick = {
                    if (currentRoute != NavRoutes.DASHBOARD) {
                        onNavigate(NavRoutes.DASHBOARD)
                    } else {
                        onReselect(NavRoutes.DASHBOARD)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = navSelectedIcon,
                    selectedTextColor = navSelectedText,
                    unselectedIconColor = navUnselected,
                    unselectedTextColor = navUnselected,
                    indicatorColor = navSelectedIndicator
                ),
                modifier = Modifier.testTag("nav_home")
            )

            NavigationBarItem(
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Applications") },
                label = { Text("Applications") },
                selected = currentRoute == NavRoutes.APPLICATIONS,
                onClick = {
                    if (currentRoute != NavRoutes.APPLICATIONS) {
                        onNavigate(NavRoutes.APPLICATIONS)
                    } else {
                        onReselect(NavRoutes.APPLICATIONS)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = navSelectedIcon,
                    selectedTextColor = navSelectedText,
                    unselectedIconColor = navUnselected,
                    unselectedTextColor = navUnselected,
                    indicatorColor = navSelectedIndicator
                ),
                modifier = Modifier.testTag("nav_applications")
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = currentRoute == NavRoutes.SETTINGS,
                onClick = {
                    if (currentRoute != NavRoutes.SETTINGS) {
                        onNavigate(NavRoutes.SETTINGS)
                    } else {
                        onReselect(NavRoutes.SETTINGS)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = navSelectedIcon,
                    selectedTextColor = navSelectedText,
                    unselectedIconColor = navUnselected,
                    unselectedTextColor = navUnselected,
                    indicatorColor = navSelectedIndicator
                ),
                modifier = Modifier.testTag("nav_settings")
            )
        }
    }
}
