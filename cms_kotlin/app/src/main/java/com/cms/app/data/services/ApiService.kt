package com.cms.app.data.services

import com.cms.app.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Any>

    // ── Complaints ────────────────────────────────────────────────────────────

    @POST("api/complaints")
    suspend fun createComplaint(@Body request: ComplaintRequest): Response<Any>

    @GET("api/complaints/{id}")
    suspend fun getComplaintById(@Path("id") id: Long): Response<ComplaintModel>

    @PUT("api/complaints/{id}")
    suspend fun updateComplaint(
        @Path("id") id: Long,
        @Body request: ComplaintRequest
    ): Response<Any>

    @DELETE("api/complaints/{id}")
    suspend fun deleteComplaint(@Path("id") id: Long): Response<Any>

    @PUT("api/complaints/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Long,
        @Query("status") status: String
    ): Response<Any>

    @GET("api/complaints/all")
    suspend fun getAllComplaints(): Response<List<ComplaintModel>>

    @GET("api/complaints/paginated")
    suspend fun getPaginatedComplaints(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PaginatedResponse>

    @GET("api/complaints/my-complaints")
    suspend fun getMyComplaints(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PaginatedResponse>

    @GET("api/complaints/my-complaints/all")
    suspend fun getMyComplaintsAll(): Response<List<ComplaintModel>>

    @GET("api/complaints/search")
    suspend fun searchComplaints(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PaginatedResponse>

    @GET("api/complaints/filter/status")
    suspend fun filterByStatus(
        @Query("status") status: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PaginatedResponse>
}
