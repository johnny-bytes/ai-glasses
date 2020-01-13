package com.schloesser.masterthesis.data.repository

import android.content.Context
import com.schloesser.masterthesis.data.base.SingletonHolder

class SessionRepository private constructor(context: Context) {

    companion object : SingletonHolder<SessionRepository, Context>(::SessionRepository)

    private var preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    private var accessTokenCache: String? = null

    @Synchronized
    fun setAccessToken(accessToken: String) {
        accessTokenCache = accessToken
        preferences.edit().putString("accessToken", accessToken).apply()
    }

    @Synchronized
    fun getAccessToken(): String? {
        if (accessTokenCache == null) {
            accessTokenCache = preferences.getString("accessToken", null)
        }
        return accessTokenCache
    }

    private var refreshTokenCache: String? = null

    @Synchronized
    fun setRefreshToken(refreshToken: String) {
        refreshTokenCache = refreshToken
        preferences.edit().putString("refreshToken", refreshToken).apply()
    }

    @Synchronized
    fun getRefreshToken(): String? {
        if (refreshTokenCache == null) {
            refreshTokenCache = preferences.getString("refreshToken", null)
        }
        return refreshTokenCache
    }


    fun hasSession() = getAccessToken() != null && getRefreshToken() != null

    fun clearSession() {
        accessTokenCache = null
        refreshTokenCache = null
        preferences.edit().clear().apply()
    }
}