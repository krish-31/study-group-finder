package com.studygroup.finder.ui.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studygroup.finder.ui.components.EmptyStateView
import com.studygroup.finder.ui.home.components.BottomNavBar
import com.studygroup.finder.ui.home.components.GroupCard

/**
 * Screen displaying study groups that the user is currently part of.
 *
 * @param viewModel              [GroupViewModel] provided via Hilt.
 * @param currentRoute           the active navigation route (for bottom bar highlight).
 * @param onNavigateToCreateGroup navigate to the Create Group screen.
 * @param onNavigateToGroupDetail navigate to a specific group's detail screen.
 * @param onNavigateToExplore     callback to navigate back to search/discover groups.
 * @param onBottomNavClick        callback for bottom navigation item taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    viewModel: GroupViewModel,
    currentRoute: String?,
    isAdmin: Boolean = false,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupDetail: (groupId: String) -> Unit,
    onNavigateToExplore: () -> Unit,
    onBottomNavClick: (route: String) -> Unit
) {
    val userGroups by viewModel.userGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val currentUser by viewModel.currentUserProfile.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Refresh groups list when the screen starts or user ID changes
    LaunchedEffect(currentUser) {
        currentUser?.let {
            viewModel.loadUserGroups(it.userId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Groups",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                isAdmin = isAdmin,
                onItemClick = onBottomNavClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Group"
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = {
                currentUser?.let {
                    viewModel.loadUserGroups(it.userId)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (userGroups.isEmpty() && !isLoading) {
                EmptyStateView(
                    icon = Icons.Outlined.Groups,
                    title = "No Groups Joined Yet",
                    subtitle = "Join or create a study group to collaborate with friends, schedule study sessions, and share learning materials.",
                    actionButtonText = "Explore Groups",
                    onActionClick = onNavigateToExplore,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = userGroups,
                        key = { it.groupId }
                    ) { group ->
                        GroupCard(
                            group = group,
                            isMember = true,
                            isCompact = false,
                            onViewClick = { onNavigateToGroupDetail(group.groupId) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
