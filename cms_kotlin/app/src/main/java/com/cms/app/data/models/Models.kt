package com.cms.app.data.models

import com.google.gson.annotations.SerializedName

// ─── User ────────────────────────────────────────────────────────────────────

data class UserModel(
    val id: Long? = null,
    val username: String = "",
    val password: String? = null,
    val role: String = "USER"
) {
    val isAdmin: Boolean get() = role == "ADMIN"
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val role: String
)

data class AuthResponse(
    val token: String? = null,
    val user: UserModel? = null,
    val role: String? = null,
    val username: String? = null
)

// ─── Complaint ───────────────────────────────────────────────────────────────

data class ComplaintModel(
    val id: Long? = null,
    val title: String = "",
    val description: String = "",
    val status: String = "PENDING",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val user: UserModel? = null
)

data class ComplaintRequest(
    val title: String,
    val description: String,
    val status: String = "PENDING"
)

data class PaginatedResponse(
    val content: List<ComplaintModel> = emptyList(),
    val page: Int = 0,
    val size: Int = 10,
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

// ─── Status ──────────────────────────────────────────────────────────────────

enum class ComplaintStatus(val display: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed");
}

// ADD this new enum below it
enum class UserRole(val display: String) {
    ADMIN("Admin"),
    CUSTOMER("Customer"),
    SUPPORT("Support"),
    MANAGER("Manager"),
    CEO("CEO");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.name == value } ?: CUSTOMER
    }
}
