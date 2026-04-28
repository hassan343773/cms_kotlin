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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cms.app.ui.components.FormLabel
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.ComplaintViewModel
import com.cms.app.viewmodel.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditComplaintScreen(
    isEdit: Boolean = false,
    complaintViewModel: ComplaintViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val complaint    = if (isEdit) complaintViewModel.selectedComplaint.collectAsState().value else null
    val actionState  by complaintViewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title       by remember { mutableStateOf(complaint?.title ?: "") }
    var description by remember { mutableStateOf(complaint?.description ?: "") }

    var titleError  by remember { mutableStateOf("") }
    var descError   by remember { mutableStateOf("") }

    val isLoading = actionState is LoadState.Loading

    fun validate(): Boolean {
        titleError = when {
            title.trim().isEmpty()  -> "Title is required"
            title.trim().length < 5 -> "Minimum 5 characters"
            else -> ""
        }
        descError = when {
            description.trim().isEmpty()   -> "Description is required"
            description.trim().length < 10 -> "Minimum 10 characters"
            else -> ""
        }
        return titleError.isEmpty() && descError.isEmpty()
    }

    fun submit() {
        if (!validate()) return
        if (isEdit && complaint?.id != null) {
            complaintViewModel.updateComplaint(complaint.id, title.trim(), description.trim()) { ok, err ->
                if (ok) onSuccess() else { /* error handled below */ }
            }
        } else {
            complaintViewModel.createComplaint(title.trim(), description.trim()) { ok, err ->
                if (ok) onSuccess()
            }
        }
    }

    LaunchedEffect(actionState) {
        if (actionState is LoadState.Error) {
            snackbarHostState.showSnackbar((actionState as LoadState.Error).message)
            complaintViewModel.clearActionState()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = DangerRed, contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Complaint" else "New Complaint", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isEdit) Secondary else Primary, titleContentColor = Color.White)
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
            // Header card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            if (isEdit) listOf(Secondary, Color(0xFF9061F9))
                            else listOf(Primary, Color(0xFF2563EB))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isEdit) Icons.Rounded.Edit else Icons.Rounded.AddCircleOutline,
                        null, tint = Color.White, modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isEdit) "Update Complaint" else "Submit a Complaint",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        Text(
                            if (isEdit) "Modify the details below" else "Describe your issue in detail",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Title field
            FormLabel("Title *")
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) { title = it; titleError = "" } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Brief summary of your complaint", color = TextHint) },
                leadingIcon = { Icon(Icons.Rounded.Title, null, tint = TextHint) },
                isError = titleError.isNotEmpty(),
                supportingText = {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        if (titleError.isNotEmpty()) Text(titleError, color = DangerRed, fontSize = 11.sp) else Spacer(Modifier.weight(1f))
                        Text("${title.length}/100", fontSize = 11.sp, color = TextHint)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isEdit) Secondary else Primary,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(Modifier.height(16.dp))

            // Description field
            FormLabel("Description *")
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 1000) { description = it; descError = "" } },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                placeholder = { Text("Describe the issue in detail...", color = TextHint) },
                isError = descError.isNotEmpty(),
                supportingText = {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        if (descError.isNotEmpty()) Text(descError, color = DangerRed, fontSize = 11.sp) else Spacer(Modifier.weight(1f))
                        Text("${description.length}/1000", fontSize = 11.sp, color = TextHint)
                    }
                },
                maxLines = 8,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isEdit) Secondary else Primary,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = ::submit,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEdit) Secondary else Primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(if (isEdit) Icons.Rounded.Save else Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEdit) "Save Changes" else "Submit Complaint", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Cancel") }
        }
    }
}
