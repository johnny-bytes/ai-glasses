package com.schloesser.masterthesis.data.repository

import android.content.Context
import androidx.preference.PreferenceManager
import com.schloesser.masterthesis.data.base.SingletonHolder

class SessionRepository private constructor(context: Context) {

    companion object : SingletonHolder<SessionRepository, Context>(::SessionRepository)

    private val preferences by lazy {
        context.getSharedPreferences("session", Context.MODE_PRIVATE)
    }

    var accessToken: String?
        @Synchronized set(value) = preferences.edit().putString("accessToken", value).apply()
        @Synchronized get() = preferences.getString("accessToken", null)

    var refreshToken: String?
        @Synchronized set(value) = preferences.edit().putString("refreshToken", value).apply()
        @Synchronized get() = preferences.getString("refreshToken", null)


    fun hasSession() = accessToken != null && refreshToken != null

    fun clearSession() {
        preferences.edit().clear().apply()
    }
}