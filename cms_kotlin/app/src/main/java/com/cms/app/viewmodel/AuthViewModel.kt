package com.cms.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cms.app.data.models.UserModel
import com.cms.app.data.repository.ApiResult
import com.cms.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle       : AuthState()
    object Loading    : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _currentUser = MutableStateFlow<UserModel?>(null)
    val currentUser: StateFlow<UserModel?> = _currentUser.asStateFlow()

    val isAdmin: Boolean get() = _currentUser.value?.isAdmin == true

    val canViewGlobalComplaintQueue: Boolean
        get() = _currentUser.value?.canViewGlobalComplaintQueue == true

    val canModerateComplaintStatus: Boolean
        get() = _currentUser.value?.canModerateComplaintStatus == true

    val canDeleteComplaints: Boolean
        get() = _currentUser.value?.canDeleteComplaints == true

    val canManageAssignmentAndProgress: Boolean
        get() = _currentUser.value?.canManageAssignmentAndProgress == true

    val canBrowseProfileDirectory: Boolean
        get() = _currentUser.value?.canBrowseProfileDirectory == true

    init {
        viewModelScope.launch {
            repository.sessionUserFlow()
                .distinctUntilChanged()
                .collect { user ->
                    _currentUser.value = user
                    when {
                        user == null -> _state.value = AuthState.Unauthenticated
                        _state.value is AuthState.Loading -> Unit
                        _state.value is AuthState.Error -> Unit
                        else -> _state.value = AuthState.Authenticated
                    }
                }
        }
    }

    /** Re-read session from storage (e.g. after returning to the app). */
    fun syncUserFromStorage() {
        viewModelScope.launch {
            val user = repository.getStoredUser()
            _currentUser.value = user
            if (user == null) _state.value = AuthState.Unauthenticated
        }
    }

    fun checkAuth() {
        viewModelScope.launch {
            val user = repository.getStoredUser()
            _currentUser.value = user
            _state.value = if (user != null) AuthState.Authenticated
                           else AuthState.Unauthenticated
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            when (val result = repository.login(username, password)) {
                is ApiResult.Success -> {
                    _currentUser.value = repository.getStoredUser()
                    _state.value = AuthState.Authenticated
                }
                is ApiResult.Error -> _state.value = AuthState.Error(result.message)
            }
        }
    }

    fun register(username: String, password: String, role: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            when (val result = repository.register(username, password, role)) {
                is ApiResult.Success -> {
                    _state.value = AuthState.Unauthenticated
                    onSuccess()
                }
                is ApiResult.Error -> _state.value = AuthState.Error(result.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
            _state.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) _state.value = AuthState.Idle
    }
}
