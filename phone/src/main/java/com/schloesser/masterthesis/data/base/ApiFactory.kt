package com.schloesser.masterthesis.data.base

import com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor
import com.schloesser.masterthesis.BuildConfig
import com.schloesser.masterthesis.data.UserApi
import com.schloesser.masterthesis.data.EmotionRecordApi
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiFactory {

//    private var BASE_URL = "http://192.168.178.41:5005/"
    private var BASE_URL = "https://ai-glass-api.happimeter.org/"

    private val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()

        builder.connectTimeout(20, TimeUnit.SECONDS)
        builder.callTimeout(20, TimeUnit.SECONDS)
        builder.readTimeout(20, TimeUnit.SECONDS)

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

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val emotionRecordApi: EmotionRecordApi by lazy {
        retrofit.create(EmotionRecordApi::class.java)
    }
}