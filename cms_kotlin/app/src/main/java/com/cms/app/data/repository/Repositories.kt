package com.cms.app.data.repository

import com.cms.app.data.models.*
import com.cms.app.data.services.RetrofitClient
import com.cms.app.utils.SessionManager
import kotlinx.coroutines.flow.Flow

// ─── Result wrapper ──────────────────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

// ─── Auth Repository ─────────────────────────────────────────────────────────

class AuthRepository(private val session: SessionManager) {

    private val api = RetrofitClient.apiService

    fun sessionUserFlow(): Flow<UserModel?> = session.userFlow

    suspend fun login(username: String, password: String): ApiResult<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                val token = body.token ?: return ApiResult.Error("No token received")
                val user = body.user ?: UserModel(
                    username = username,
                    role = body.role ?: "USER"
                )
                session.saveSession(token, user)
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Invalid credentials", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun register(username: String, password: String, role: String): ApiResult<Unit> {
        return try {
            val response = api.register(RegisterRequest(username, password, role))
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error("Registration failed: ${response.code()}", response.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun logout() = session.clearSession()

    suspend fun getStoredUser() = session.getStoredUser()

    suspend fun isLoggedIn() = session.isLoggedIn()
}

// ─── Complaint Repository ─────────────────────────────────────────────────────

class ComplaintRepository {

    private val api = RetrofitClient.apiService

    suspend fun getPaginated(page: Int = 0, size: Int = 10): ApiResult<PaginatedResponse> =
        safeCall { api.getPaginatedComplaints(page, size) }

    suspend fun getMyComplaints(page: Int = 0, size: Int = 10): ApiResult<PaginatedResponse> =
        safeCallMyComplaints(page, size)

    /** Full list for the signed-in customer (used for background sync / fingerprints). */
    suspend fun getMyComplaintsAll(): ApiResult<List<ComplaintModel>> {
        return try {
            val response = api.getMyComplaintsAll()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load complaints", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    private suspend fun safeCallMyComplaints(page: Int, size: Int): ApiResult<PaginatedResponse> {
        return try {
            val response = api.getMyComplaints(page, size)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: PaginatedResponse())
            } else {
                // fallback: try the /all endpoint
                val fallback = api.getMyComplaintsAll()
                if (fallback.isSuccessful) {
                    val list = fallback.body() ?: emptyList()
                    ApiResult.Success(PaginatedResponse(
                        content = list, totalElements = list.size,
                        totalPages = 1, first = true, last = true
                    ))
                } else {
                    ApiResult.Error("Failed to load complaints", response.code())
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun filterByStatus(status: String, page: Int = 0, size: Int = 10): ApiResult<PaginatedResponse> =
        safeCall { api.filterByStatus(status, page, size) }

    suspend fun search(keyword: String, page: Int = 0, size: Int = 10): ApiResult<PaginatedResponse> =
        safeCall { api.searchComplaints(keyword, page, size) }

    suspend fun getById(id: Long): ApiResult<ComplaintModel> =
        safeCall { api.getComplaintById(id) }

    suspend fun getComplaintMessages(id: Long): ApiResult<List<ComplaintMessageDto>> {
        return try {
            val r = api.getComplaintMessages(id)
            if (r.isSuccessful) ApiResult.Success(r.body() ?: emptyList())
            else {
                val detail = r.errorBody()?.string()?.take(120)
                ApiResult.Error(
                    if (detail.isNullOrBlank()) "Failed to load comments (${r.code()})"
                    else "Failed to load comments: $detail",
                    r.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun postComplaintMessage(id: Long, message: String): ApiResult<Unit> {
        return try {
            val r = api.postComplaintMessage(id, PostCommentBody(message.trim()))
            if (r.isSuccessful) ApiResult.Success(Unit)
            else {
                val detail = r.errorBody()?.string()?.take(120)
                ApiResult.Error(
                    if (detail.isNullOrBlank()) "Failed to post comment (${r.code()})"
                    else "Failed to post comment: $detail",
                    r.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun create(
        title: String,
        description: String,
        assignee: String? = null,
        progressPercent: Int? = null
    ): ApiResult<Unit> {
        return try {
            val r = api.createComplaint(
                ComplaintRequest(title, description, "PENDING", assignee, progressPercent)
            )
            if (r.isSuccessful) ApiResult.Success(Unit) else ApiResult.Error("Create failed", r.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun update(
        id: Long,
        title: String,
        description: String,
        status: String = "PENDING",
        assignee: String? = null,
        progressPercent: Int? = null
    ): ApiResult<Unit> {
        return try {
            val r = api.updateComplaint(
                id,
                ComplaintRequest(title, description, status, assignee, progressPercent)
            )
            if (r.isSuccessful) ApiResult.Success(Unit) else ApiResult.Error("Update failed", r.code())
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateStatus(id: Long, status: String): ApiResult<ComplaintModel> {
        return try {
            val r = api.updateStatus(id, status)
            if (r.isSuccessful) {
                val body = r.body()
                if (body != null) ApiResult.Success(body)
                else ApiResult.Success(ComplaintModel(id = id, status = status))
            } else {
                ApiResult.Error("Status update failed", r.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun delete(id: Long): ApiResult<Unit> {
        return try {
            val r = api.deleteComplaint(id)
            if (r.isSuccessful) ApiResult.Success(Unit) else ApiResult.Error("Delete failed", r.code())
        } catch (e: Exception) { ApiResult.Error(e.localizedMessage ?: "Network error") }
    }

    private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): ApiResult<T> {
        return try {
            val r = call()
            if (r.isSuccessful) {
                ApiResult.Success(r.body()!!)
            } else {
                ApiResult.Error("Request failed: ${r.code()}", r.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
