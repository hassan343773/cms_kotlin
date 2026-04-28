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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    onNavigateToComplaints: () -> Unit,
    onNavigateToDetail: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser   by authViewModel.currentUser.collectAsState()
    val complaints    by complaintViewModel.complaints.collectAsState()
    val loadState     by complaintViewModel.loadState.collectAsState()
    val totalElements = complaintViewModel.totalElements
    val isAdmin       = authViewModel.isAdmin

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isRefreshing     by remember { mutableStateOf(false) }
    val pullState        = rememberPullToRefreshState()

    LaunchedEffect(isAdmin) {
        complaintViewModel.fetchComplaints(isAdmin = isAdmin)
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
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.SemiBold)  },
                actions = {
                    if (isAdmin) {
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
                icon           = { Icon(Icons.Rounded.Add, null) },
                text           = { Text("New Complaint") },
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
                complaintViewModel.fetchComplaints(isAdmin = isAdmin)
            }
        ) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().background(SurfaceBg),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item { WelcomeCard(username = currentUser?.username ?: "User", isAdmin = isAdmin) }

                if (isAdmin) {
                    item { StatsRow(complaints) }
                }

                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = if (isAdmin) "Recent Complaints" else "My Complaints",
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
                                onRetry = { complaintViewModel.fetchComplaints(isAdmin = isAdmin) }
                            )
                        }
                    }
                    complaints.isEmpty() -> {
                        item { EmptyView("No complaints yet") }
                    }
                    else -> {
                        items(complaints.take(5)) { complaint ->
                            ComplaintCard(
                                complaint = complaint,
                                showUser  = isAdmin,
                                onClick   = {
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
private fun WelcomeCard(username: String, isAdmin: Boolean) {
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
                    if (isAdmin) "Admin Dashboard" else "Manage your complaints",
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
                Text(if (isAdmin) "ADMIN" else "USER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    Text(label, fontSize = 9.sp, color = TextSecondary)
                }
            }
        }
    }
}
