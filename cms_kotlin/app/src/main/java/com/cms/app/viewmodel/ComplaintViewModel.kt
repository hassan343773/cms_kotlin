package com.cms.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cms.app.data.models.ComplaintModel
import com.cms.app.data.models.PaginatedResponse
import com.cms.app.data.repository.ApiResult
import com.cms.app.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoadState {
    object Idle    : LoadState()
    object Loading : LoadState()
    object Success : LoadState()
    data class Error(val message: String) : LoadState()
}

class ComplaintViewModel : ViewModel() {

    private val repository = ComplaintRepository()

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

    val isLoading: Boolean get() = _loadState.value is LoadState.Loading

    // ── Fetch ────────────────────────────────────────────────────────────────

    fun fetchComplaints(page: Int = 0, size: Int = 10, isAdmin: Boolean = false, append: Boolean = false) {
        viewModelScope.launch {
            _loadState.value = LoadState.Loading
            val result = when {
                isAdmin && _activeFilter.value != "ALL" ->
                    repository.filterByStatus(_activeFilter.value, page, size)
                isAdmin ->
                    repository.getPaginated(page, size)
                else ->
                    repository.getMyComplaints(page, size)
            }
            when (result) {
                is ApiResult.Success -> {
                    _pagination.value = result.data
                    _complaints.value = if (append) _complaints.value + result.data.content
                                        else result.data.content
                    _loadState.value = LoadState.Success
                }
                is ApiResult.Error -> _loadState.value = LoadState.Error(result.message)
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
                    _loadState.value = LoadState.Success
                }
                is ApiResult.Error -> _loadState.value = LoadState.Error(result.message)
            }
        }
    }

    // ── Create / Update ──────────────────────────────────────────────────────

    fun createComplaint(title: String, description: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _actionState.value = LoadState.Loading
            when (val r = repository.create(title, description)) {
                is ApiResult.Success -> {
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

    fun updateComplaint(id: Long, title: String, description: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _actionState.value = LoadState.Loading
            when (val r = repository.update(id, title, description)) {
                is ApiResult.Success -> {
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
                    // update locally
                    _complaints.value = _complaints.value.map {
                        if (it.id == id) it.copy(status = status) else it
                    }
                    _selectedComplaint.value = _selectedComplaint.value?.let {
                        if (it.id == id) it.copy(status = status) else it
                    }
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun setFilter(filter: String) { _activeFilter.value = filter }

    fun setSelectedComplaint(complaint: ComplaintModel) { _selectedComplaint.value = complaint }

    fun reset() {
        _complaints.value = emptyList()
        _pagination.value = PaginatedResponse()
        _activeFilter.value = "ALL"
        _loadState.value = LoadState.Idle
    }

    fun clearActionState() { _actionState.value = LoadState.Idle }

    val hasNext: Boolean get() = _pagination.value.hasNext
    val currentPage: Int get() = _pagination.value.page
    val totalElements: Int get() = _pagination.value.totalElements
}
