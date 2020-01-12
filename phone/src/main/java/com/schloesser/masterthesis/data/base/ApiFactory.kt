package com.schloesser.masterthesis.data.base

import com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor
import com.schloesser.masterthesis.BuildConfig
import com.schloesser.masterthesis.data.Api
import com.schloesser.masterthesis.data.repository.SessionRepository
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
        builder.addInterceptor(HeaderInterceptor())

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

    val api: Api by lazy {
        retrofit.create(Api::class.java)
    }

    private class HeaderInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val builder: Request.Builder = request.newBuilder()
            val accessToken = SessionRepository.token

            if (accessToken != null) {
                builder.header("Authorization", "Bearer %s".format(accessToken));
            }

            return chain.proceed(builder.build())
        }
    }
}