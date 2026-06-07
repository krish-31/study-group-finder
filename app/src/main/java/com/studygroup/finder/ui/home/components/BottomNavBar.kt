package com.studygroup.finder.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studygroup.finder.core.navigation.Screen

/**
 * Destination model used by [BottomNavBar].
 *
 * @property route           the navigation route to navigate to.
 * @property label           the human-readable label shown below the icon.
 * @property selectedIcon    icon shown when this item is selected.
 * @property unselectedIcon  icon shown when this item is not selected.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/** The core bottom navigation destinations (always visible). */
private val coreNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = Screen.Search.route,
        label = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    BottomNavItem(
        route = Screen.GroupList.route,
        label = "My Groups",
        selectedIcon = Icons.Outlined.Groups,
        unselectedIcon = Icons.Outlined.Groups
    ),
    BottomNavItem(
        route = Screen.Notifications.route,
        label = "Alerts",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    ),
    BottomNavItem(
        route = Screen.Profile.route,
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

/** Admin-only navigation item. */
private val adminNavItem = BottomNavItem(
    route = Screen.AdminPanel.route,
    label = "Admin",
    selectedIcon = Icons.Filled.AdminPanelSettings,
    unselectedIcon = Icons.Outlined.AdminPanelSettings
)

/**
 * The list of items used externally when `isAdmin = false`.
 * Kept for backward compatibility with any code referencing [bottomNavItems].
 */
val bottomNavItems: List<BottomNavItem> = coreNavItems

/**
 * Reusable Material 3 bottom navigation bar.
 *
 * When [isAdmin] is true, an additional "Admin" tab is shown at the end.
 *
 * @param currentRoute  the currently active route (to highlight the correct tab).
 * @param isAdmin       whether the current user has admin privileges.
 * @param onItemClick   callback invoked with the target route when a tab is tapped.
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    isAdmin: Boolean = false,
    onItemClick: (String) -> Unit
) {
    val items = if (isAdmin) coreNavItems + adminNavItem else coreNavItems

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
