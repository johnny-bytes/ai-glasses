package com.schloesser.masterthesis.data

import com.schloesser.masterthesis.data.request.LoginRequest
import com.schloesser.masterthesis.data.response.AuthResponse
import com.schloesser.masterthesis.data.response.GetSessionsResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface Api {

    @POST("auth/")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @Multipart
    @POST("record/")
    fun sendFrame(@Part file: MultipartBody.Part, @Part("session_id") sessionId: Int): Call<String>

    @GET("session/")
    fun getSessions(): Call<GetSessionsResponse>
}