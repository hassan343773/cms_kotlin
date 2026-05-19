package com.cms.app.data.models

import com.google.gson.annotations.SerializedName

// ─── User ────────────────────────────────────────────────────────────────────

data class UserModel(
    val id: Long? = null,
    val username: String = "",
    val password: String? = null,
    val role: String = "USER"
) {
    private fun r(): String = role.trim().uppercase()

    /** Full platform admin (dangerous actions). */
    val isAdmin: Boolean get() = r() == "ADMIN"

    /** Sees org-wide complaint queues (paginated / filtered APIs), not only "my submissions". */
    val canViewGlobalComplaintQueue: Boolean get() = r() in setOf("ADMIN", "MANAGER", "CEO", "SUPPORT")

    /** Can update workflow status on a complaint. */
    val canModerateComplaintStatus: Boolean get() = r() in setOf("ADMIN", "MANAGER", "SUPPORT", "CEO")

    /** Can remove complaints (usually admin-only). */
    val canDeleteComplaints: Boolean get() = r() == "ADMIN"

    val isSupport: Boolean get() = r() == "SUPPORT"
    val isManager: Boolean get() = r() == "MANAGER"

    /** Assignee, progress %, and evidence images — staff only (not end customers). */
    val canManageAssignmentAndProgress: Boolean get() = r() in setOf("ADMIN", "MANAGER", "SUPPORT", "CEO")

    /** Can open the org profile directory (derived from complaints until a users API exists). */
    val canBrowseProfileDirectory: Boolean get() = canViewGlobalComplaintQueue

    /** Whether [viewer] may open a profile card for [target] (not including self; self is always allowed). */
    fun canViewProfileOf(target: UserModel): Boolean {
        if (!canBrowseProfileDirectory) return false
        val t = target.r()
        return when {
            isAdmin -> true
            isManager || r() == "CEO" -> t != "ADMIN"
            isSupport -> t in setOf("USER", "CUSTOMER", "SUPPORT", "MANAGER", "ASSIGNEE")
            else -> false
        }
    }
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
    val user: UserModel? = null,
    /** From server when backend persists staff assignment (syncs to customer). */
    val assignee: String? = null,
    val progressPercent: Int? = null
)

data class ComplaintRequest(
    val title: String,
    val description: String,
    val status: String = "PENDING",
    val assignee: String? = null,
    val progressPercent: Int? = null
)

/** Server thread message (GET/POST `/complaints/{id}/messages`). */
data class ComplaintMessageDto(
    val id: Long? = null,
    val author: String? = null,
    val message: String? = null,
    val createdAt: String? = null
)

data class PostCommentBody(
    val message: String
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
