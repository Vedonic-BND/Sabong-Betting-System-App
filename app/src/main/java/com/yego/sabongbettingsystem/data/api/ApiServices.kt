package com.yego.sabongbettingsystem.data.api

import com.yego.sabongbettingsystem.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<MessageResponse>

    // ── Fight ─────────────────────────────────────────────
    @GET("fight/current")
    suspend fun getCurrentFight(@Header("Authorization") token: String): Response<Fight>

    @GET("fight/history")
    suspend fun getFightHistory(@Header("Authorization") token: String): Response<List<Fight>>

    @POST("fight")
    suspend fun createFight(
        @Header("Authorization") token: String,
        @Body request: CreateFightRequest
    ): Response<Fight>

    @PUT("fight/{id}/status")
    suspend fun updateFightStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateStatusRequest
    ): Response<MessageResponse>

    @PUT("fight/{id}/side-status")
    suspend fun updateSideStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateSideStatusRequest
    ): Response<MessageResponse>

    @PUT("fight/{id}/all-side-status")
    suspend fun updateAllSideStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateAllSideStatusRequest
    ): Response<MessageResponse>

    @POST("fight/{id}/finalize")
    suspend fun finalizeBet(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    @POST("fight/{id}/winner")
    suspend fun declareWinner(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: DeclareWinnerRequest
    ): Response<MessageResponse>

    // ── Bets ─────────────────────────────────────────────
    @POST("bet")
    suspend fun placeBet(
        @Header("Authorization") token: String,
        @Body request: PlaceBetRequest
    ): Response<BetResponse>

    @POST("bet")
    suspend fun placeBetAsAdmin(
        @Header("Authorization") token: String,
        @Body request: PlaceBetRequest
    ): Response<BetResponse>

    // ── Payout ───────────────────────────────────────────
    @GET("payout/{reference}")
    suspend fun getPayout(
        @Header("Authorization") token: String,
        @Path("reference") reference: String
    ): Response<PayoutResponse>

    @POST("payout/{reference}")
    suspend fun confirmPayout(
        @Header("Authorization") token: String,
        @Path("reference") reference: String
    ): Response<MessageResponse>
}