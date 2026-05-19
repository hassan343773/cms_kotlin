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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cms.app.ui.components.FormLabel
import com.cms.app.ui.theme.*
import com.cms.app.viewmodel.ComplaintViewModel
import com.cms.app.viewmodel.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditComplaintScreen(
    isEdit: Boolean = false,
    canManageAssignmentAndProgress: Boolean,
    complaintViewModel: ComplaintViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val complaint = if (isEdit) complaintViewModel.selectedComplaint.collectAsState().value else null
    val extrasMap by complaintViewModel.extrasByComplaintId.collectAsState()
    val actionState by complaintViewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf(complaint?.title ?: "") }
    var description by remember { mutableStateOf(complaint?.description ?: "") }

    var titleError by remember { mutableStateOf("") }
    var descError by remember { mutableStateOf("") }

    var assignee by remember { mutableStateOf("") }
    var progressSlider by remember { mutableFloatStateOf(0f) }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(complaint?.id, extrasMap) {
        if (!isEdit) return@LaunchedEffect
        val id = complaint?.id ?: return@LaunchedEffect
        val e = extrasMap[id] ?: return@LaunchedEffect
        assignee = e.assignee
        progressSlider = e.progressPercent.toFloat().coerceIn(0f, 100f)
        imageUris = e.imageUris
    }

    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        val merged = (imageUris + uris.map { it.toString() }).distinct().take(6)
        imageUris = merged
    }

    val isLoading = actionState is LoadState.Loading

    fun validate(): Boolean {
        titleError = when {
            title.trim().isEmpty() -> "Title is required"
            title.trim().length < 5 -> "Minimum 5 characters"
            else -> ""
        }
        descError = when {
            description.trim().isEmpty() -> "Description is required"
            description.trim().length < 10 -> "Minimum 10 characters"
            else -> ""
        }
        return titleError.isEmpty() && descError.isEmpty()
    }

    fun submit() {
        if (!validate()) return
        if (isEdit) {
            val id = complaint?.id ?: return
            val preserved = if (!canManageAssignmentAndProgress) complaintViewModel.extrasFor(id) else null
            complaintViewModel.updateComplaint(
                id,
                title.trim(),
                description.trim(),
                assignee = preserved?.assignee?.trim() ?: assignee.trim(),
                imageUris = imageUris,
                progressPercent = preserved?.progressPercent ?: progressSlider.toInt()
            ) { ok, _ ->
                if (ok) onSuccess()
            }
        } else {
            complaintViewModel.createComplaint(
                title.trim(),
                description.trim(),
                if (canManageAssignmentAndProgress) assignee.trim() else "",
                imageUris,
                if (canManageAssignmentAndProgress) progressSlider.toInt() else 0
            ) { ok, _ ->
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
                Snackbar(
                    snackbarData = data,
                    containerColor = DangerRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp)
                )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isEdit) Secondary else Primary,
                    titleContentColor = Color.White
                )
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
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isEdit) "Update Complaint" else "Submit a Complaint",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (canManageAssignmentAndProgress) {
                                if (isEdit) "Assignee, images, and progress are saved with this complaint"
                                else "You can set assignee, progress, and photos — they are stored for your demo"
                            } else {
                                if (isEdit) "You can edit text and photos; assignee and progress stay with staff"
                                else "Add photos with your complaint; staff will assign and track progress"
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

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
                        if (titleError.isNotEmpty()) Text(titleError, color = DangerRed, fontSize = 11.sp)
                        else Spacer(Modifier.weight(1f))
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

            FormLabel("Description *")
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 1000) { description = it; descError = "" } },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                placeholder = { Text("Describe the issue in detail...", color = TextHint) },
                isError = descError.isNotEmpty(),
                supportingText = {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        if (descError.isNotEmpty()) Text(descError, color = DangerRed, fontSize = 11.sp)
                        else Spacer(Modifier.weight(1f))
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

            Spacer(Modifier.height(20.dp))
            if (canManageAssignmentAndProgress) {
                FormLabel("Assignee")
                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Name of person responsible", color = TextHint) },
                    leadingIcon = { Icon(Icons.Rounded.AssignmentInd, null, tint = TextHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(16.dp))
                FormLabel("Initial progress (%)")
                Slider(
                    value = progressSlider,
                    onValueChange = { progressSlider = it },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = if (isEdit) Secondary else Primary)
                )
                Text("${progressSlider.toInt()}%", fontSize = 13.sp, color = TextSecondary)

                Spacer(Modifier.height(16.dp))
            }

            FormLabel("Complaint images")
            Text(
                "Optional — up to 6 photos (stored on this device for the demo).",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (imageUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(imageUris, key = { it }) { uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUris = imageUris.filter { it != uri } },
                                modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = {
                    pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, null)
                Spacer(Modifier.width(8.dp))
                Text("Add photos")
            }

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
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(if (isEdit) Icons.Rounded.Save else Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEdit) "Save Changes" else "Submit Complaint",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
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
