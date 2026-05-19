package com.cms.app.navigation

import android.net.Uri

object Routes {
    const val SPLASH   = "splash"
    const val LOGIN    = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val COMPLAINTS = "complaints"
    const val COMPLAINT_DETAIL = "complaint_detail"
    const val CREATE_COMPLAINT = "create_complaint"
    const val EDIT_COMPLAINT   = "edit_complaint"
    const val TEAM_TASKS     = "team_tasks"
    const val MY_PROFILE = "my_profile"
    const val PROFILE_DIRECTORY = "profile_directory"
    const val PROFILE_DETAIL = "profile_detail/{username}/{role}"
    const val NOTIFICATIONS = "notifications"

    fun profileDetailRoute(username: String, role: String): String =
        "profile_detail/${Uri.encode(username)}/${Uri.encode(role)}"
}
