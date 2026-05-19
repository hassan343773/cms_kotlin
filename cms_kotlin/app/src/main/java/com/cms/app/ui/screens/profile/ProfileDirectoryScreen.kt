@file:Suppress("DEPRECATION")

package com.cms.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.data.local.ComplaintExtras
import com.cms.app.data.models.ComplaintModel
import com.cms.app.data.models.UserModel
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDirectoryScreen(
    authViewModel: AuthViewModel,
    complaints: List<ComplaintModel>,
    extrasByComplaintId: Map<Long, ComplaintExtras>,
    onBack: () -> Unit,
    onOpenProfile: (username: String, role: String) -> Unit
) {
    val viewer by authViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val entries = remember(complaints, extrasByComplaintId) {
        buildDirectoryEntries(complaints, extrasByComplaintId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("People", fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Built from people on complaints in your workspace (demo until a users API exists).",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No profiles yet.", color = TextSecondary)
                    }
                }
            } else {
                items(entries, key = { "${it.username}|${it.role}" }) { row ->
                    val target = UserModel(username = row.username, role = row.role)
                    val canOpen = viewer?.let { v ->
                        v.username.equals(row.username, ignoreCase = true) ||
                            v.canViewProfileOf(target)
                    } == true
                    Card(
                        onClick = {
                            if (canOpen) {
                                onOpenProfile(row.username, row.role)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("You can't open this profile with your role.")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.People, null, tint = Primary, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(row.username, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(row.role.uppercase(), fontSize = 12.sp, color = TextSecondary)
                            }
                            if (canOpen) {
                                Icon(Icons.Rounded.ChevronRight, null, tint = TextHint)
                            } else {
                                Icon(Icons.Rounded.Lock, null, tint = TextHint.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Row shown in the directory list. */
data class ProfileDirectoryRow(val username: String, val role: String)

internal fun buildDirectoryEntries(
    complaints: List<ComplaintModel>,
    extras: Map<Long, ComplaintExtras>
): List<ProfileDirectoryRow> {
    val map = linkedMapOf<String, String>()
    complaints.forEach { c ->
        c.user?.let { u ->
            val name = u.username.trim()
            if (name.isNotEmpty()) map[name] = u.role.ifBlank { "USER" }
        }
        val id = c.id ?: return@forEach
        val assignee = extras[id]?.assignee?.trim().orEmpty()
        if (assignee.isNotEmpty() && !map.containsKey(assignee)) {
            map[assignee] = "ASSIGNEE"
        }
    }
    return map.map { ProfileDirectoryRow(it.key, it.value) }
        .sortedBy { it.username.lowercase() }
}
