@file:Suppress("DEPRECATION")

package com.cms.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.data.local.InAppNotification
import com.cms.app.ui.theme.*
import com.cms.app.utils.Constants
import com.cms.app.viewmodel.ComplaintViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    complaintViewModel: ComplaintViewModel,
    onBack: () -> Unit
) {
    val items by complaintViewModel.inAppNotifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (items.any { !it.read }) {
                        TextButton(onClick = { complaintViewModel.markAllInAppNotificationsRead() }) {
                            Text("Mark all read", color = Color.White, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceBg)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Notifications, null, tint = TextHint, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = TextSecondary, fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "When staff change a complaint on another device, updates appear here after the next sync from the server.",
                        fontSize = 12.sp,
                        color = TextHint,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceBg)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { n ->
                    NotificationRow(
                        notification = n,
                        onClick = { complaintViewModel.markInAppNotificationRead(n.id) }
                    )
                }
                item {
                    Text(
                        "Cross-device sync: complaint status and text come from your server when this app refreshes (about every ${Constants.POLL_INTERVAL_MS / 1000}s in the background, or when you return to the app). " +
                            "Comments sync from the server when you open a complaint or while the app refreshes in the background. Photos in this demo stay on each phone until your API stores them.",
                        fontSize = 11.sp,
                        color = TextHint,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: InAppNotification,
    onClick: () -> Unit
) {
    val fmt = rememberDateFormat()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) CardBg else CardBg.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(if (notification.read) 1.dp else 3.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            if (!notification.read) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Primary, shape = RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(10.dp))
            } else {
                Spacer(Modifier.width(2.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(notification.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(notification.body, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    fmt.format(Date(notification.createdAt)),
                    fontSize = 11.sp,
                    color = TextHint
                )
            }
        }
    }
}

@Composable
private fun rememberDateFormat(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }
}
