@file:Suppress("DEPRECATION")

package com.cms.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.data.models.ComplaintModel
import com.cms.app.ui.components.*
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.AuthViewModel
import com.cms.app.viewmodel.ComplaintViewModel
import com.cms.app.viewmodel.LoadState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    canBrowseProfileDirectory: Boolean,
    onNavigateToComplaints: () -> Unit,
    onNavigateToTeamTasks: () -> Unit,
    onNavigateToDetail: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToMyProfile: () -> Unit,
    onNavigateToProfileDirectory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser   by authViewModel.currentUser.collectAsState()
    val complaints    by complaintViewModel.complaints.collectAsState()
    val loadState     by complaintViewModel.loadState.collectAsState()
    val extrasMap     by complaintViewModel.extrasByComplaintId.collectAsState()
    val inAppNotifs   by complaintViewModel.inAppNotifications.collectAsState()
    val totalElements = complaintViewModel.totalElements
    val useGlobalQueue = authViewModel.canViewGlobalComplaintQueue

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isRefreshing     by remember { mutableStateOf(false) }
    val pullState        = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val unreadNotifs     = inAppNotifs.count { !it.read }

    LaunchedEffect(Unit) {
        complaintViewModel.inAppToastMessages.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(useGlobalQueue) {
        complaintViewModel.fetchComplaints(useGlobalComplaintQueue = useGlobalQueue)
    }

    LaunchedEffect(loadState) {
        if (loadState !is LoadState.Loading) isRefreshing = false
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title       = "Logout",
            message     = "Are you sure you want to logout?",
            confirmText = "Logout",
            onConfirm   = {
                showLogoutDialog = false
                authViewModel.logout()
                complaintViewModel.reset()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, shape = RoundedCornerShape(10.dp))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.SemiBold, color = Color.White)  },
                actions = {
                    BadgedBox(
                        badge = {
                            if (unreadNotifs > 0) {
                                Badge { Text(unreadNotifs.coerceAtMost(99).toString(), fontSize = 10.sp) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Rounded.Notifications, "Notifications", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onNavigateToMyProfile) {
                        Icon(Icons.Rounded.AccountCircle, "My profile", tint = Color.White)
                    }
                    if (canBrowseProfileDirectory) {
                        IconButton(onClick = onNavigateToProfileDirectory) {
                            Icon(Icons.Rounded.People, "People", tint = Color.White)
                        }
                    }
                    if (useGlobalQueue) {
                        IconButton(onClick = onNavigateToComplaints) {
                            Icon(Icons.Rounded.ListAlt, "All Complaints", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Rounded.Logout, "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onNavigateToCreate,
                icon           = { Icon(Icons.Rounded.Add, null, tint = Color.White) },
                text           = { Text("New Complaint", color = Color.White) },
                containerColor = Primary,
                contentColor   = Color.White
            )
        }
    ) { padding ->
        PullToRefreshBox(
            modifier     = Modifier.padding(padding),
            state        = pullState,
            isRefreshing = isRefreshing,
            onRefresh    = {
                isRefreshing = true
                complaintViewModel.fetchComplaints(useGlobalComplaintQueue = useGlobalQueue)
            }
        ) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().background(SurfaceBg),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item { WelcomeCard(username = currentUser?.username ?: "User", role = currentUser?.role ?: "USER") }

                if (useGlobalQueue) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = onNavigateToTeamTasks,
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Groups, null, tint = Primary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Team & tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Members and assignee buckets are built from complaints in your workspace (no fake users).",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = TextHint)
                            }
                        }
                    }
                }

                if (useGlobalQueue) {
                    item { StatsRow(complaints) }
                }

                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = if (useGlobalQueue) "Recent complaints" else "My complaints",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (totalElements > 5) {
                            TextButton(onClick = onNavigateToComplaints) {
                                Text("View All", color = Primary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                when {
                    loadState is LoadState.Loading && complaints.isEmpty() -> {
                        item {
                            Box(Modifier.fillMaxWidth().height(300.dp), Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                    }
                    loadState is LoadState.Error && complaints.isEmpty() -> {
                        item {
                            ErrorView(
                                message = (loadState as LoadState.Error).message,
                                onRetry = { complaintViewModel.fetchComplaints(useGlobalComplaintQueue = useGlobalQueue) }
                            )
                        }
                    }
                    complaints.isEmpty() -> {
                        item { EmptyView("No complaints yet") }
                    }
                    else -> {
                        items(complaints.take(5)) { complaint ->
                            val ex = complaint.id?.let { extrasMap[it] }
                            ComplaintCard(
                                complaint = complaint,
                                showUser = useGlobalQueue,
                                assignee = ex?.assignee?.takeIf { it.isNotBlank() }
                                    ?: complaint.assignee?.takeIf { it.isNotBlank() },
                                progressPercent = ex?.progressPercent?.takeIf { it > 0 }
                                    ?: complaint.progressPercent?.takeIf { it > 0 },
                                onClick = {
                                    complaintViewModel.setSelectedComplaint(complaint)
                                    onNavigateToDetail()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(username: String, role: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Primary, Color(0xFF2563EB))))
            .padding(20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Hello, $username 👋", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    when (role.uppercase()) {
                        "ADMIN" -> "Admin workspace"
                        "MANAGER" -> "Manager workspace"
                        "SUPPORT" -> "Support workspace"
                        "CEO" -> "Executive overview"
                        else -> "Manage your complaints"
                    },
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(role.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatsRow(complaints: List<ComplaintModel>) {
    val stats = listOf(
        Triple("Total",       complaints.size,                                 Primary),
        Triple("Pending",     complaints.count { it.status == "PENDING" },     StatusPending),
        Triple("In Progress", complaints.count { it.status == "IN_PROGRESS" }, StatusProgress),
        Triple("Resolved",    complaints.count { it.status == "RESOLVED" },    StatusResolved)
    )
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.forEach { (label, count, color) ->
            Card(
                modifier  = Modifier.weight(1f),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                    Spacer(Modifier.height(2.dp))
                    Text(label, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}
