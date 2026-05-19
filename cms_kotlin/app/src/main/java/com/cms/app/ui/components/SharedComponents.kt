package com.cms.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.data.models.ComplaintModel
import com.cms.app.ui.theme.*

// ─── Status Badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status.uppercase()) {
        "PENDING"     -> Triple(PendingBg,  StatusPending,  "Pending")
        "IN_PROGRESS" -> Triple(ProgressBg, StatusProgress, "In Progress")
        "RESOLVED"    -> Triple(ResolvedBg, StatusResolved, "Resolved")
        "CLOSED"      -> Triple(ClosedBg,   StatusClosed,   "Closed")
        else          -> Triple(ClosedBg,   StatusClosed,   status)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(0.5.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

// ─── Complaint Card ───────────────────────────────────────────────────────────

@Composable
fun ComplaintCard(
    complaint: ComplaintModel,
    showUser: Boolean = false,
    assignee: String? = null,
    progressPercent: Int? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1
                )
                StatusBadge(complaint.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            val assigneeText = assignee?.takeIf { it.isNotBlank() }
            if (assigneeText != null || (progressPercent != null && progressPercent > 0)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    assigneeText?.let {
                        Icon(Icons.Rounded.AssignmentInd, null, modifier = Modifier.size(14.dp), tint = Primary)
                        Text("Assignee: $it", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Medium)
                    }
                    if (progressPercent != null && progressPercent > 0) {
                        Spacer(Modifier.weight(1f))
                        Text("${progressPercent}%", fontSize = 11.sp, color = StatusProgress, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showUser && complaint.user != null) {
                    Icon(Icons.Rounded.Person, contentDescription = null,
                        modifier = Modifier.size(13.dp), tint = TextHint)
                    Spacer(Modifier.width(3.dp))
                    Text(complaint.user.username, fontSize = 11.sp, color = TextHint)
                    Spacer(Modifier.width(10.dp))
                }
                Icon(Icons.Rounded.Schedule, contentDescription = null,
                    modifier = Modifier.size(13.dp), tint = TextHint)
                Spacer(Modifier.width(3.dp))
                Text(
                    text = complaint.createdAt?.take(10) ?: "N/A",
                    fontSize = 11.sp, color = TextHint
                )
                Spacer(Modifier.weight(1f))
                complaint.id?.let {
                    Text("#$it", fontSize = 11.sp, color = TextHint, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─── Loading ─────────────────────────────────────────────────────────────────

@Composable
fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}

// ─── Error ───────────────────────────────────────────────────────────────────

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = Color(0xFFEF4444))
            Spacer(Modifier.height(12.dp))
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (onRetry != null) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

// ─── Empty ───────────────────────────────────────────────────────────────────

@Composable
fun EmptyView(message: String = "No complaints found") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.Inbox, contentDescription = null,
                modifier = Modifier.size(72.dp), tint = Color(0xFFD1D5DB))
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
    }
}

// ─── Filter Chips ─────────────────────────────────────────────────────────────

@Composable
fun StatusFilterChips(selected: String, onSelected: (String) -> Unit) {
    val filters = listOf("ALL", "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED")
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selected
            val color = when (filter) {
                "PENDING"     -> StatusPending
                "IN_PROGRESS" -> StatusProgress
                "RESOLVED"    -> StatusResolved
                "CLOSED"      -> StatusClosed
                else          -> Primary
            }
            FilterChip(
                selected = isSelected,
                onClick  = { onSelected(filter) },
                label = {
                    Text(
                        text = filter.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor  = color,
                    selectedLabelColor      = Color.White,
                    containerColor          = color.copy(alpha = 0.08f),
                    labelColor              = color
                )
            )
        }
    }
}

// ─── Form Field ──────────────────────────────────────────────────────────────

@Composable
fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF374151),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// ─── Confirm Dialog ──────────────────────────────────────────────────────────

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    confirmColor: Color = DangerRed,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text  = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) { Text(confirmText) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
