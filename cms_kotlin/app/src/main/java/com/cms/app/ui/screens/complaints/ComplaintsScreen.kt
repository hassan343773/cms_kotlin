package com.cms.app.ui.screens.complaints

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.cms.app.viewmodel.LoadState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ComplaintsScreen(
    isAdmin: Boolean,
    complaintViewModel: ComplaintViewModel,
    onNavigateToDetail: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onBack: () -> Unit
) {
    val complaints   by complaintViewModel.complaints.collectAsState()
    val loadState    by complaintViewModel.loadState.collectAsState()
    val activeFilter by complaintViewModel.activeFilter.collectAsState()
    val pagination   by complaintViewModel.pagination.collectAsState()

    var searchQuery  by remember { mutableStateOf("") }
    var isSearching  by remember { mutableStateOf(false) }
    val listState    = rememberLazyListState()

    // Infinite scroll trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3 && pagination.hasNext && loadState !is LoadState.Loading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            complaintViewModel.fetchComplaints(
                page = complaintViewModel.currentPage + 1,
                isAdmin = isAdmin,
                append = true
            )
        }
    }

    // Load on first entry
    LaunchedEffect(Unit) {
        complaintViewModel.fetchComplaints(isAdmin = isAdmin)
    }

    // Debounced search
    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery }
            .debounce(400)
            .distinctUntilChanged()
            .collect { query ->
                if (query.isBlank()) {
                    isSearching = false
                    complaintViewModel.fetchComplaints(isAdmin = isAdmin)
                } else {
                    isSearching = true
                    complaintViewModel.search(query)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAdmin) "All Complaints" else "My Complaints", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = Primary,
                contentColor = Color.White
            ) { Icon(Icons.Rounded.Add, null) }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("Search complaints...", color = TextHint) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextHint, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, null, tint = TextHint, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    unfocusedContainerColor = CardBg,
                    focusedContainerColor = CardBg
                )
            )

            // Filter chips (admin only, not while searching)
            if (isAdmin && !isSearching) {
                ScrollableStatusFilterChips(
                    selected = activeFilter,
                    onSelected = { filter ->
                        complaintViewModel.setFilter(filter)
                        complaintViewModel.fetchComplaints(isAdmin = true)
                    }
                )
                Spacer(Modifier.height(4.dp))
            }

            // Result count
            Text(
                text = "${pagination.totalElements} complaint${if (pagination.totalElements != 1) "s" else ""}",
                fontSize = 12.sp,
                color = TextHint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // List
            when {
                loadState is LoadState.Loading && complaints.isEmpty() -> FullScreenLoading()
                loadState is LoadState.Error && complaints.isEmpty() -> ErrorView(
                    message = (loadState as LoadState.Error).message,
                    onRetry = { complaintViewModel.fetchComplaints(isAdmin = isAdmin) }
                )
                complaints.isEmpty() -> EmptyView(
                    if (isSearching) "No results for \"$searchQuery\"" else "No complaints found"
                )
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(complaints, key = { it.id ?: it.hashCode() }) { complaint ->
                            ComplaintCard(
                                complaint = complaint,
                                showUser = isAdmin,
                                onClick = {
                                    complaintViewModel.setSelectedComplaint(complaint)
                                    onNavigateToDetail()
                                }
                            )
                        }
                        if (pagination.hasNext) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollableStatusFilterChips(selected: String, onSelected: (String) -> Unit) {
    val filters = listOf("ALL", "PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED")
    ScrollableTabRow(
        selectedTabIndex = filters.indexOf(selected).coerceAtLeast(0),
        edgePadding = 16.dp,
        containerColor = SurfaceBg,
        contentColor = Primary,
        indicator = {},
        divider = {}
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
            Tab(selected = isSelected, onClick = { onSelected(filter) }) {
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(filter) },
                    label = {
                        Text(filter.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    },
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color,
                        selectedLabelColor     = Color.White,
                        containerColor         = color.copy(alpha = 0.08f),
                        labelColor             = color
                    )
                )
            }
        }
    }
}
