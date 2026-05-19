package com.cms.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cms.app.data.local.ComplaintComment
import com.cms.app.data.local.ComplaintExtras
import com.cms.app.data.local.ComplaintExtrasRepository
import com.cms.app.data.local.ComplaintRemoteFingerprintStore
import com.cms.app.data.local.InAppNotification
import com.cms.app.data.local.InAppNotificationRepository
import com.cms.app.data.models.ComplaintModel
import com.cms.app.data.models.PaginatedResponse
import com.cms.app.data.repository.ApiResult
import com.cms.app.data.repository.ComplaintRepository
import com.cms.app.utils.Constants
import com.cms.app.utils.SessionManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed class LoadState {
    object Idle : LoadState()
    object Loading : LoadState()
    object Success : LoadState()
    data class Error(val message: String) : LoadState()
}

class ComplaintViewModel(application: Application) : ViewModel() {

    private val repository = ComplaintRepository()
    private val extrasRepository = ComplaintExtrasRepository(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)
    private val notificationRepository = InAppNotificationRepository(application.applicationContext)
    private val remoteFingerprintStore = ComplaintRemoteFingerprintStore(application.applicationContext)

    /** In-app inbox for the signed-in username (from session). */
    private val _inAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val inAppNotifications: StateFlow<List<InAppNotification>> = _inAppNotifications.asStateFlow()

    private val _inAppToastMessages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val inAppToastMessages: SharedFlow<String> = _inAppToastMessages.asSharedFlow()

    val unreadInAppNotificationCount: Int
        get() = _inAppNotifications.value.count { !it.read }

    /** Mirrors last successful list fetch: staff queue vs "my submissions". */
    private var lastFetchUseGlobalQueue: Boolean = false

    private val _complaints = MutableStateFlow<List<ComplaintModel>>(emptyList())
    val complaints: StateFlow<List<ComplaintModel>> = _complaints.asStateFlow()

    private val _selectedComplaint = MutableStateFlow<ComplaintModel?>(null)
    val selectedComplaint: StateFlow<ComplaintModel?> = _selectedComplaint.asStateFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private val _actionState = MutableStateFlow<LoadState>(LoadState.Idle)
    val actionState: StateFlow<LoadState> = _actionState.asStateFlow()

    private val _pagination = MutableStateFlow(PaginatedResponse())
    val pagination: StateFlow<PaginatedResponse> = _pagination.asStateFlow()

    private val _activeFilter = MutableStateFlow("ALL")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    private val _extrasByComplaintId = MutableStateFlow<Map<Long, ComplaintExtras>>(emptyMap())
    val extrasByComplaintId: StateFlow<Map<Long, ComplaintExtras>> = _extrasByComplaintId.asStateFlow()

    val isLoading: Boolean get() = _loadState.value is LoadState.Loading

    init {
        viewModelScope.launch {
            extrasRepository.extrasFlow.collect { _extrasByComplaintId.value = it }
        }
        viewModelScope.launch {
            combine(sessionManager.usernameFlow, notificationRepository.inboxFlow) { userRaw, inbox ->
                val u = userRaw?.trim()?.lowercase().orEmpty()
                if (u.isEmpty()) emptyList()
                else inbox[u].orEmpty().sortedByDescending { it.createdAt }
            }.collect { _inAppNotifications.value = it }
        }
        viewModelScope.launch {
            while (true) {
                silentPollRemoteComplaints()
                delay(Constants.POLL_INTERVAL_MS)
            }
        }
    }

    fun extrasFor(complaintId: Long?): ComplaintExtras {
        if (complaintId == null) return ComplaintExtras()
        return _extrasByComplaintId.value[complaintId] ?: ComplaintExtras()
    }

    fun markInAppNotificationRead(id: String) {
        viewModelScope.launch {
            val u = sessionManager.getStoredUser()?.username?.trim()?.lowercase().orEmpty()
            if (u.isEmpty()) return@launch
            notificationRepository.markRead(u, id)
        }
    }

    fun markAllInAppNotificationsRead() {
        viewModelScope.launch {
            val u = sessionManager.getStoredUser()?.username?.trim()?.lowercase().orEmpty()
            if (u.isEmpty()) return@launch
            notificationRepository.markAllRead(u)
        }
    }

    private fun complaintSnapshot(id: Long): ComplaintModel? =
        _complaints.value.firstOrNull { it.id == id }
            ?: _selectedComplaint.value?.takeIf { it.id == id }

    private fun parseCommentCreatedAt(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        val core = iso.trim().take(19)
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getDefault()
            fmt.parse(core)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun loadCommentsForComplaint(complaintId: Long, notifyOnError: Boolean = false) {
        viewModelScope.launch {
            when (val r = repository.getComplaintMessages(complaintId)) {
                is ApiResult.Success -> {
                    val mapped = r.data.map { dto ->
                        ComplaintComment(
                            author = dto.author.orEmpty(),
                            message = dto.message.orEmpty(),
                            timestamp = parseCommentCreatedAt(dto.createdAt)
                        )
                    }
                    extrasRepository.update(complaintId) { prev ->
                        prev.copy(comments = mapped)
                    }
                }
                is ApiResult.Error -> {
                    if (notifyOnError) emitToastForCurrentUser(r.message)
                }
            }
        }
    }

    private suspend fun emitToastForCurrentUser(message: String) {
        if (message.isNotBlank()) _inAppToastMessages.emit(message)
    }

    private fun fingerprintOf(c: ComplaintModel): String =
        "${c.status}|${c.updatedAt ?: ""}|${c.title.trim()}|${c.description.trim().take(200)}|" +
            "${c.assignee?.trim().orEmpty()}|${c.progressPercent ?: 0}"

    /**
     * When **my complaints** are refreshed from the API, compare each row to the last snapshot
     * stored on this device. If the server changed (e.g. staff on another phone), notify **this**
     * signed-in user — true cross-device sync for fields the backend returns.
     */
    private suspend fun applyRemoteComplaintFingerprints(content: List<ComplaintModel>) {
        val me = sessionManager.getStoredUser()?.username?.trim()?.lowercase().orEmpty()
        if (me.isEmpty()) return
        for (c in content) {
            val id = c.id ?: continue
            val fp = fingerprintOf(c)
            val prev = remoteFingerprintStore.get(id)
            if (prev != null && prev != fp) {
                notificationRepository.addForUsername(
                    me,
                    InAppNotification(
                        title = "Complaint updated (server)",
                        body = "“${c.title.take(48)}” (#$id) — status: ${c.status.replace('_', ' ')}.",
                        complaintId = id
                    )
                )
                emitToastForCurrentUser("Complaint #$id was updated on the server.")
            }
            remoteFingerprintStore.put(id, fp)
        }
    }

    private suspend fun recordServerFingerprint(model: ComplaintModel) {
        val id = model.id ?: return
        remoteFingerprintStore.put(id, fingerprintOf(model))
    }

    private suspend fun maybeNotifyExecutivesAboutNewComplaints(content: List<ComplaintModel>) {
        val actor = sessionManager.getStoredUser() ?: return
        val role = actor.role.trim().uppercase()
        if (role !in setOf("ADMIN", "MANAGER", "CEO", "SUPPORT")) return
        val username = actor.username.trim().lowercase()
        if (username.isEmpty()) return
        val seen = notificationRepository.getSeenComplaintIds(username)
        val idsOnScreen = content.mapNotNull { it.id }.toSet()
        if (seen.isEmpty()) {
            notificationRepository.replaceSeenComplaintIds(username, idsOnScreen)
            return
        }
        val fresh = content.filter { c -> c.id != null && c.id !in seen }
        for (c in fresh) {
            notificationRepository.addForUsername(
                username,
                InAppNotification(
                    title = "New complaint",
                    body = "“${c.title.take(72)}” (#${c.id})",
                    complaintId = c.id
                )
            )
        }
        if (fresh.isNotEmpty()) {
            emitToastForCurrentUser("New complaint(s) — open Notifications.")
        }
        notificationRepository.replaceSeenComplaintIds(username, seen + idsOnScreen)
    }

    // ── Fetch ────────────────────────────────────────────────────────────────

    /**
     * @param useGlobalComplaintQueue true → paginated / status-filter APIs (staff).
     * false → `my-complaints` (customers / end users).
     */
    fun fetchComplaints(
        page: Int = 0,
        size: Int = 10,
        useGlobalComplaintQueue: Boolean = false,
        append: Boolean = false
    ) {
        lastFetchUseGlobalQueue = useGlobalComplaintQueue
        viewModelScope.launch {
            applyComplaintsPage(page, size, useGlobalComplaintQueue, append, showLoading = true)
        }
    }

    private suspend fun refreshComplaintsFirstPage() {
        applyComplaintsPage(page = 0, size = 10, useGlobalComplaintQueue = lastFetchUseGlobalQueue, append = false, showLoading = true)
    }

    /** Refresh using the same staff vs customer mode; customers load all rows for fingerprints + merge. */
    fun refreshComplaintsFromLastMode() {
        viewModelScope.launch {
            if (!sessionManager.isLoggedIn()) return@launch
            val user = sessionManager.getStoredUser() ?: return@launch
            if (!user.canViewGlobalComplaintQueue) {
                if (pullAndMergeMyComplaintsFromServer()) {
                    _loadState.value = LoadState.Success
                } else {
                    applyComplaintsPage(0, 10, false, false, showLoading = false)
                }
            } else {
                applyComplaintsPage(
                    page = 0,
                    size = 10,
                    useGlobalComplaintQueue = lastFetchUseGlobalQueue,
                    append = false,
                    showLoading = false
                )
            }
        }
    }

    /** Customer: full list from API → fingerprints, merge into UI, update total count. */
    private suspend fun pullAndMergeMyComplaintsFromServer(): Boolean {
        return when (val all = repository.getMyComplaintsAll()) {
            is ApiResult.Success -> {
                applyRemoteComplaintFingerprints(all.data)
                syncAssigneeProgressFromServer(all.data)
                mergeMyComplaintsFromServer(all.data)
                _pagination.value = _pagination.value.copy(totalElements = all.data.size)
                true
            }
            is ApiResult.Error -> false
        }
    }

    private fun silentPollRemoteComplaints() {
        viewModelScope.launch {
            if (!sessionManager.isLoggedIn()) return@launch
            val user = sessionManager.getStoredUser() ?: return@launch
            // Staff must poll the global queue for new IDs — do not rely on "last screen" mode
            // (defaults to customer before the first fetch completes).
            val useGlobalQueue = user.canViewGlobalComplaintQueue
            if (!useGlobalQueue) {
                if (!pullAndMergeMyComplaintsFromServer()) { /* silent: keep UI */ }
            } else {
                applyComplaintsPage(
                    page = 0,
                    size = 10,
                    useGlobalComplaintQueue = true,
                    append = false,
                    showLoading = false
                )
            }
            _selectedComplaint.value?.id?.let { loadCommentsForComplaint(it) }
        }
    }

    /**
     * Apply server rows to the in-memory list without dropping items the user paged in
     * (paginated "my complaints" only returns one page).
     */
    private fun mergeMyComplaintsFromServer(server: List<ComplaintModel>) {
        if (server.isEmpty()) return
        val byId = server.mapNotNull { c -> c.id?.let { id -> id to c } }.toMap()
        val current = _complaints.value
        val updated = current.map { c ->
            val id = c.id ?: return@map c
            byId[id] ?: c
        }
        val currentIds = current.mapNotNull { it.id }.toSet()
        val newFromServer = server.filter { it.id != null && it.id !in currentIds }
        if (newFromServer.isEmpty() && updated == current) return
        _complaints.value = newFromServer + updated
    }

    /** Copy assignee / progress from API models into local extras so all screens stay in sync. */
    private suspend fun syncAssigneeProgressFromServer(rows: Iterable<ComplaintModel>) {
        for (c in rows) {
            val id = c.id ?: continue
            val a = c.assignee
            val p = c.progressPercent
            if (a == null && p == null) continue
            extrasRepository.update(id) { prev ->
                prev.copy(
                    assignee = a?.trim() ?: prev.assignee,
                    progressPercent = p ?: prev.progressPercent
                )
            }
        }
    }

    private suspend fun applyComplaintsPage(
        page: Int,
        size: Int,
        useGlobalComplaintQueue: Boolean,
        append: Boolean,
        showLoading: Boolean = true
    ) {
        if (showLoading) _loadState.value = LoadState.Loading
        val result = when {
            useGlobalComplaintQueue && _activeFilter.value != "ALL" ->
                repository.filterByStatus(_activeFilter.value, page, size)
            useGlobalComplaintQueue ->
                repository.getPaginated(page, size)
            else ->
                repository.getMyComplaints(page, size)
        }
        when (result) {
            is ApiResult.Success -> {
                val newList = if (append) _complaints.value + result.data.content else result.data.content
                if (useGlobalComplaintQueue && page == 0 && !append) {
                    maybeNotifyExecutivesAboutNewComplaints(newList)
                }
                if (!useGlobalComplaintQueue && page == 0 && !append) {
                    applyRemoteComplaintFingerprints(newList)
                }
                syncAssigneeProgressFromServer(result.data.content)
                _pagination.value = result.data
                _complaints.value = newList
                if (showLoading) _loadState.value = LoadState.Success
            }
            is ApiResult.Error -> {
                if (showLoading) _loadState.value = LoadState.Error(result.message)
            }
        }
    }

    fun search(keyword: String, page: Int = 0) {
        viewModelScope.launch {
            _loadState.value = LoadState.Loading
            when (val result = repository.search(keyword, page)) {
                is ApiResult.Success -> {
                    _pagination.value = result.data
                    _complaints.value = result.data.content
                    if (!lastFetchUseGlobalQueue) {
                        applyRemoteComplaintFingerprints(result.data.content)
                    }
                    syncAssigneeProgressFromServer(result.data.content)
                    _loadState.value = LoadState.Success
                }
                is ApiResult.Error -> _loadState.value = LoadState.Error(result.message)
            }
        }
    }

    fun fetchById(id: Long) {
        viewModelScope.launch {
            _loadState.value = LoadState.Loading
            when (val result = repository.getById(id)) {
                is ApiResult.Success -> {
                    _selectedComplaint.value = result.data
                    syncAssigneeProgressFromServer(listOf(result.data))
                    recordServerFingerprint(result.data)
                    loadCommentsForComplaint(id)
                    _loadState.value = LoadState.Success
                }
                is ApiResult.Error -> _loadState.value = LoadState.Error(result.message)
            }
        }
    }

    // ── Create / Update ──────────────────────────────────────────────────────

    fun createComplaint(
        title: String,
        description: String,
        assignee: String,
        imageUris: List<String>,
        progressPercent: Int,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _actionState.value = LoadState.Loading
            val staff = sessionManager.getStoredUser()?.canManageAssignmentAndProgress == true
            val assigneeParam = if (staff) assignee.trim().ifBlank { null } else null
            val progressParam = if (staff) progressPercent.coerceIn(0, 100) else null
            when (val r = repository.create(title, description, assigneeParam, progressParam)) {
                is ApiResult.Success -> {
                    refreshComplaintsFirstPage()
                    val newId = _complaints.value
                        .filter { it.title.trim() == title.trim() }
                        .maxByOrNull { it.id ?: 0L }
                        ?.id
                    if (newId != null) {
                        extrasRepository.put(
                            newId,
                            ComplaintExtras(
                                assignee = assignee.trim(),
                                imageUris = imageUris,
                                comments = emptyList(),
                                progressPercent = progressPercent.coerceIn(0, 100)
                            )
                        )
                    }
                    _actionState.value = LoadState.Success
                    onResult(true, null)
                }
                is ApiResult.Error -> {
                    _actionState.value = LoadState.Error(r.message)
                    onResult(false, r.message)
                }
            }
        }
    }

    fun updateComplaint(
        id: Long,
        title: String,
        description: String,
        assignee: String,
        imageUris: List<String>,
        progressPercent: Int,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _actionState.value = LoadState.Loading
            val status = complaintSnapshot(id)?.status ?: "PENDING"
            val staff = sessionManager.getStoredUser()?.canManageAssignmentAndProgress == true
            val assigneeParam = if (staff) assignee.trim().ifBlank { null } else null
            val progressParam = if (staff) progressPercent.coerceIn(0, 100) else null
            when (val r = repository.update(id, title, description, status, assigneeParam, progressParam)) {
                is ApiResult.Success -> {
                    extrasRepository.update(id) { prev ->
                        prev.copy(
                            assignee = assignee.trim(),
                            imageUris = imageUris,
                            progressPercent = progressPercent.coerceIn(0, 100)
                        )
                    }
                    _selectedComplaint.value = _selectedComplaint.value?.let {
                        if (it.id == id) {
                            it.copy(
                                title = title,
                                description = description,
                                assignee = assignee.trim().ifBlank { null },
                                progressPercent = progressPercent.coerceIn(0, 100)
                            )
                        } else it
                    }
                    _complaints.value = _complaints.value.map {
                        if (it.id == id) {
                            it.copy(
                                title = title,
                                description = description,
                                assignee = assignee.trim().ifBlank { null },
                                progressPercent = progressPercent.coerceIn(0, 100)
                            )
                        } else it
                    }
                    complaintSnapshot(id)?.let { recordServerFingerprint(it) }
                    _actionState.value = LoadState.Success
                    onResult(true, null)
                }
                is ApiResult.Error -> {
                    _actionState.value = LoadState.Error(r.message)
                    onResult(false, r.message)
                }
            }
        }
    }

    fun updateStatus(id: Long, status: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val r = repository.updateStatus(id, status)) {
                is ApiResult.Success -> {
                    val merged = r.data
                    _complaints.value = _complaints.value.map {
                        if (it.id == id) merged else it
                    }
                    _selectedComplaint.value = _selectedComplaint.value?.let {
                        if (it.id == id) merged else it
                    }
                    recordServerFingerprint(merged)
                    syncAssigneeProgressFromServer(listOf(merged))
                    onResult(true, null)
                }
                is ApiResult.Error -> onResult(false, r.message)
            }
        }
    }

    fun deleteComplaint(id: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val r = repository.delete(id)) {
                is ApiResult.Success -> {
                    _complaints.value = _complaints.value.filter { it.id != id }
                    onResult(true, null)
                }
                is ApiResult.Error -> onResult(false, r.message)
            }
        }
    }

    fun updateAssignee(complaintId: Long, assignee: String) {
        viewModelScope.launch {
            extrasRepository.update(complaintId) { it.copy(assignee = assignee.trim()) }
            val snap = complaintSnapshot(complaintId) ?: return@launch
            if (sessionManager.getStoredUser()?.canManageAssignmentAndProgress != true) return@launch
            val ex = extrasFor(complaintId)
            repository.update(
                complaintId,
                snap.title,
                snap.description,
                snap.status,
                assignee.trim().ifBlank { "" },
                ex.progressPercent
            )
        }
    }

    fun setProgressPercent(complaintId: Long, percent: Int) {
        viewModelScope.launch {
            val p = percent.coerceIn(0, 100)
            extrasRepository.update(complaintId) {
                it.copy(progressPercent = p)
            }
            val snap = complaintSnapshot(complaintId) ?: return@launch
            if (sessionManager.getStoredUser()?.canManageAssignmentAndProgress != true) return@launch
            val ex = extrasFor(complaintId)
            repository.update(
                complaintId,
                snap.title,
                snap.description,
                snap.status,
                ex.assignee.trim().ifBlank { null },
                p
            )
        }
    }

    fun addComplaintImage(complaintId: Long, uri: String) {
        viewModelScope.launch {
            extrasRepository.update(complaintId) {
                if (it.imageUris.size >= 6) it else it.copy(imageUris = it.imageUris + uri)
            }
        }
    }

    fun removeComplaintImage(complaintId: Long, uri: String) {
        viewModelScope.launch {
            extrasRepository.update(complaintId) {
                it.copy(imageUris = it.imageUris.filter { u -> u != uri })
            }
        }
    }

    fun addComment(complaintId: Long, author: String, message: String, onResult: ((Boolean) -> Unit)? = null) {
        val text = message.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            when (val r = repository.postComplaintMessage(complaintId, text)) {
                is ApiResult.Success -> {
                    loadCommentsForComplaint(complaintId)
                    onResult?.invoke(true)
                }
                is ApiResult.Error -> {
                    emitToastForCurrentUser(r.message)
                    onResult?.invoke(false)
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun setSelectedComplaint(complaint: ComplaintModel) {
        _selectedComplaint.value = complaint
        complaint.id?.let { loadCommentsForComplaint(it, notifyOnError = true) }
    }

    fun reset() {
        _complaints.value = emptyList()
        _pagination.value = PaginatedResponse()
        _activeFilter.value = "ALL"
        _loadState.value = LoadState.Idle
        viewModelScope.launch {
            remoteFingerprintStore.clear()
        }
    }

    fun clearActionState() {
        _actionState.value = LoadState.Idle
    }

    val hasNext: Boolean get() = _pagination.value.hasNext
    val currentPage: Int get() = _pagination.value.page
    val totalElements: Int get() = _pagination.value.totalElements
}
