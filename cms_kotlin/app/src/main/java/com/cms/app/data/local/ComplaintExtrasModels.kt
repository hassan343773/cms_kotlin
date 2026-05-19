package com.cms.app.data.local

import com.google.gson.annotations.SerializedName

data class ComplaintComment(
    @SerializedName("author") val author: String,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class ComplaintExtras(
    @SerializedName("assignee") val assignee: String = "",
    @SerializedName("imageUris") val imageUris: List<String> = emptyList(),
    @SerializedName("comments") val comments: List<ComplaintComment> = emptyList(),
    /** 0–100 manual progress (demo; can align with status separately) */
    @SerializedName("progressPercent") val progressPercent: Int = 0
)
