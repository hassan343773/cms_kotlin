@file:Suppress("DEPRECATION")

package com.cms.app.ui.screens.complaints

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cms.app.data.local.ComplaintComment
import com.cms.app.ui.components.*
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.ComplaintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    canModerateStatus: Boolean,
    canDeleteComplaint: Boolean,
    canManageAssignmentAndProgress: Boolean,
    currentUserName: String,
    complaintViewModel: ComplaintViewModel,
    onNavigateToEdit: () -> Unit,
    onBack: () -> Unit
) {
    val complaint by complaintViewModel.selectedComplaint.collectAsState()
    val extrasMap by complaintViewModel.extrasByComplaintId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember(complaint?.status) { mutableStateOf(complaint?.status ?: "PENDING") }

    val cid = complaint?.id
    val extras = remember(cid, extrasMap) {
        cid?.let { extrasMap[it] } ?: com.cms.app.data.local.ComplaintExtras()
    }

    var assigneeDraft by remember(cid, extras.assignee) { mutableStateOf(extras.assignee) }
    LaunchedEffect(cid, extras.assignee) { assigneeDraft = extras.assignee }

    var progressDraft by remember(cid, extras.progressPercent) {
        mutableFloatStateOf(extras.progressPercent.toFloat().coerceIn(0f, 100f))
    }
    LaunchedEffect(cid, extras.progressPercent) {
        progressDraft = extras.progressPercent.toFloat().coerceIn(0f, 100f)
    }

    var commentText by remember { mutableStateOf("") }

    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        if (cid == null) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            complaintViewModel.addComplaintImage(cid, uri.toString())
        }
    }

    if (complaint == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val c = complaint!!

    LaunchedEffect(c.id) {
        c.id?.let { complaintViewModel.loadCommentsForComplaint(it, notifyOnError = true) }
    }

    LaunchedEffect(Unit) {
        complaintViewModel.inAppToastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Complaint",
            message = "Are you sure you want to delete \"${c.title}\"? This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteDialog = false
                complaintViewModel.deleteComplaint(c.id!!) { success, _ ->
                    if (success) onBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
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
                title = { Text("Complaint #${c.id ?: ""}", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Rounded.Edit, null, tint = Color.White)
                    }
                    if (canDeleteComplaint) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(alpha = 0.85f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = c.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        StatusBadge(c.status)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = c.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Assignee
            Text("Assignee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (canManageAssignmentAndProgress) {
                        OutlinedTextField(
                            value = assigneeDraft,
                            onValueChange = { assigneeDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Who is handling this complaint?", color = TextHint) },
                            leadingIcon = { Icon(Icons.Rounded.AssignmentInd, null, tint = TextHint) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { complaintViewModel.updateAssignee(c.id!!, assigneeDraft) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save assignee")
                        }
                    } else {
                        Text(
                            assigneeDraft.trim().ifBlank { "Unassigned (staff will assign)" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress
            Text("Progress tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    LinearProgressIndicator(
                        progress = { progressDraft / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = StatusProgress,
                        trackColor = BorderColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${progressDraft.toInt()}%", fontWeight = FontWeight.SemiBold, color = Primary)
                    if (canManageAssignmentAndProgress) {
                        Slider(
                            value = progressDraft,
                            onValueChange = { progressDraft = it },
                            valueRange = 0f..100f,
                            steps = 19
                        )
                        Button(
                            onClick = {
                                complaintViewModel.setProgressPercent(c.id!!, progressDraft.toInt())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                        ) {
                            Text("Update progress")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Images
            Text("Complaint images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (extras.imageUris.isEmpty()) {
                        Text("No images yet.", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(extras.imageUris, key = { it }) { uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (canManageAssignmentAndProgress) {
                                        IconButton(
                                            onClick = { complaintViewModel.removeComplaintImage(c.id!!, uri) },
                                            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (canManageAssignmentAndProgress) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                pickImages.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add images")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Comments / conversation
            Text("Comments & conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (extras.comments.isEmpty()) {
                        Text("No messages yet. Start the thread below.", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            extras.comments.forEach { msg ->
                                CommentBubble(msg)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Write a comment…") },
                            maxLines = 3
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                val draft = commentText
                                complaintViewModel.addComment(c.id!!, currentUserName, draft) { ok ->
                                    if (ok) commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.Rounded.Send, null)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Meta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    c.user?.let {
                        MetaRow(Icons.Rounded.Person, "Submitted by", it.username)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = BorderColor)
                    }
                    c.createdAt?.let {
                        MetaRow(Icons.Rounded.CalendarToday, "Created", it.take(10))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = BorderColor)
                    }
                    c.updatedAt?.let {
                        MetaRow(Icons.Rounded.Update, "Last Updated", it.take(10))
                    }
                }
            }

            if (canModerateStatus) {
                Spacer(Modifier.height(16.dp))
                Text("Update Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        listOf("PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED").forEach { status ->
                            val statusColor = when (status) {
                                "PENDING" -> StatusPending
                                "IN_PROGRESS" -> StatusProgress
                                "RESOLVED" -> StatusResolved
                                else -> StatusClosed
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = status },
                                    colors = RadioButtonDefaults.colors(selectedColor = statusColor)
                                )
                                Text(
                                    text = status.replace("_", " "),
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedStatus == status) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedStatus == status) statusColor else TextPrimary
                                )
                            }
                            if (status != "CLOSED") HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        complaintViewModel.updateStatus(c.id!!, selectedStatus) { success, _ ->
                            if (success) onBack()
                        }
                    },
                    enabled = selectedStatus != c.status,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Status", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CommentBubble(comment: ComplaintComment) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(comment.author, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Primary)
                Text(
                    java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(comment.timestamp)),
                    fontSize = 11.sp,
                    color = TextHint
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(comment.message, fontSize = 14.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun MetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = TextHint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = TextPrimary)
    }
}
