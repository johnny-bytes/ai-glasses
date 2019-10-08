package com.schloesser.masterthesis.data.base

import com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor
import com.schloesser.masterthesis.BuildConfig
import com.schloesser.masterthesis.data.AuthApi
import com.schloesser.masterthesis.data.EmotionRecordApi
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ApiFactory {

    companion object {
        var BASE_URL = "http://192.168.178.41:5000/"
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(OkHttpProfilerInterceptor())
        }

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val emotionRecordApi: EmotionRecordApi by lazy {
        retrofit.create(EmotionRecordApi::class.java)
    }
}