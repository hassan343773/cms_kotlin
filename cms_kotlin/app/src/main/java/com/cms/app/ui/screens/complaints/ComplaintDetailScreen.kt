package com.cms.app.ui.screens.complaints

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.ui.components.*
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.ComplaintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    isAdmin: Boolean,
    complaintViewModel: ComplaintViewModel,
    onNavigateToEdit: () -> Unit,
    onBack: () -> Unit
) {
    val complaint by complaintViewModel.selectedComplaint.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember(complaint?.status) { mutableStateOf(complaint?.status ?: "PENDING") }

    if (complaint == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val c = complaint!!

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Complaint",
            message = "Are you sure you want to delete \"${c.title}\"? This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteDialog = false
                complaintViewModel.deleteComplaint(c.id!!) { success, error ->
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
                    if (isAdmin) {
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
            // ── Main Info Card ────────────────────────────────────────────────
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

            // ── Meta Card ─────────────────────────────────────────────────────
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

            // ── Status Update (admin only) ────────────────────────────────────
            if (isAdmin) {
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
                                "PENDING"     -> StatusPending
                                "IN_PROGRESS" -> StatusProgress
                                "RESOLVED"    -> StatusResolved
                                else          -> StatusClosed
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedStatus == status,
                                    onClick  = { selectedStatus = status },
                                    colors   = RadioButtonDefaults.colors(selectedColor = statusColor)
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
                        complaintViewModel.updateStatus(c.id!!, selectedStatus) { success, error ->
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
private fun MetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = TextHint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = TextPrimary)
    }
}
