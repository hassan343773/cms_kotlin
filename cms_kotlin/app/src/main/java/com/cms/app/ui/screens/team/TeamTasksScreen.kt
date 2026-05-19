@file:Suppress("DEPRECATION")

package com.cms.app.ui.screens.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.data.local.ComplaintExtras
import com.cms.app.data.models.ComplaintModel
import com.cms.app.ui.components.StatusBadge
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.ComplaintViewModel
import com.cms.app.viewmodel.LoadState

/**
 * Workload grouped by **assignee** (from on-device extras) plus reporters from the API.
 * No hard-coded team: reflects complaints you already loaded from the server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamTasksScreen(
    complaintViewModel: ComplaintViewModel,
    useGlobalComplaintQueue: Boolean,
    onBack: () -> Unit,
    onOpenComplaint: (ComplaintModel) -> Unit
) {
    val complaints by complaintViewModel.complaints.collectAsState()
    val extras by complaintViewModel.extrasByComplaintId.collectAsState()
    val loadState by complaintViewModel.loadState.collectAsState()

    LaunchedEffect(useGlobalComplaintQueue) {
        if (!useGlobalComplaintQueue) {
            onBack()
            return@LaunchedEffect
        }
        complaintViewModel.fetchComplaints(useGlobalComplaintQueue = true)
    }

    val grouped = remember(complaints, extras) {
        buildAssigneeWorkload(complaints, extras)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team workload", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Each bucket is an assignee name you set on a complaint (or “Unassigned”). " +
                        "Reporter names come from the server. Open the complaints list first if this screen is empty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            if (loadState is LoadState.Loading && complaints.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (grouped.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("No complaints in this workspace yet", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Open the complaints list (staff) or submit one as a customer, then set assignees on details.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(grouped, key = { it.assigneeKey }) { bucket ->
                    AssigneeBucketCard(bucket = bucket, onOpenComplaint = onOpenComplaint)
                }
            }
        }
    }
}

data class AssigneeBucket(
    val assigneeKey: String,
    val displayTitle: String,
    val subtitle: String,
    val complaints: List<ComplaintModel>
)

private fun buildAssigneeWorkload(
    complaints: List<ComplaintModel>,
    extras: Map<Long, ComplaintExtras>
): List<AssigneeBucket> {
    val byAssignee = linkedMapOf<String, MutableList<ComplaintModel>>()
    for (c in complaints) {
        val id = c.id ?: continue
        val a = extras[id]?.assignee?.trim().orEmpty()
        val key = if (a.isEmpty()) "__unassigned__" else a.lowercase()
        byAssignee.getOrPut(key) { mutableListOf() }.add(c)
    }
    val sortedKeys = byAssignee.keys.sortedWith(compareBy({ it != "__unassigned__" }, { it }))
    return sortedKeys.map { key ->
        val list = byAssignee[key].orEmpty()
        val display = if (key == "__unassigned__") "Unassigned" else list.firstOrNull()?.let { c ->
            extras[c.id!!]?.assignee?.trim().orEmpty()
        }.orEmpty().ifEmpty { key }
        val reporters = list.mapNotNull { it.user?.username }.distinct().take(4).joinToString(", ")
        AssigneeBucket(
            assigneeKey = key,
            displayTitle = display,
            subtitle = if (reporters.isEmpty()) "No reporter metadata" else "Reporters: $reporters",
            complaints = list.sortedByDescending { it.id ?: 0L }
        )
    }
}

@Composable
private fun AssigneeBucketCard(
    bucket: AssigneeBucket,
    onOpenComplaint: (ComplaintModel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AssignmentInd, null, tint = Primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(bucket.displayTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(bucket.subtitle, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("${bucket.complaints.size}", fontWeight = FontWeight.Bold, color = Primary, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            bucket.complaints.forEach { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenComplaint(c) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "#${c.id} · ${c.user?.username ?: "—"}",
                            fontSize = 11.sp,
                            color = TextHint
                        )
                    }
                    StatusBadge(c.status)
                    Icon(Icons.Rounded.ChevronRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
                }
                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
            }
        }
    }
}
