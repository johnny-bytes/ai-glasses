package com.schloesser.masterthesis.data

import com.schloesser.masterthesis.data.repository.SessionRepository
import com.schloesser.masterthesis.data.request.LoginRequest
import com.schloesser.masterthesis.data.response.AuthResponse
import com.schloesser.masterthesis.entity.EmotionRecord
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface UserApi {

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @GET("emotion_record/")
    fun getAllEmotionRecords(@Header("Authorization") token: String): Call<List<EmotionRecord>>

    @Multipart
    @POST("face/")
    fun sendFace(@Part file: MultipartBody.Part, @Header("Authorization") token: String): Call<String>
}