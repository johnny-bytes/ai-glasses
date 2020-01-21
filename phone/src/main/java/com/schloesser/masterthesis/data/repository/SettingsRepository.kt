package com.schloesser.masterthesis.data.repository

import android.content.Context
import androidx.preference.PreferenceManager
import com.schloesser.masterthesis.data.base.SingletonHolder

class SettingsRepository private constructor(context: Context) {

    companion object : SingletonHolder<SettingsRepository, Context>(::SettingsRepository)

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val sendFrameIntervalSeconds: Int
        @Synchronized get() = sharedPreferences.getString("sendFrameIntervalSeconds", "15")!!.toInt()

    val offlineModeEnabled: Boolean
        @Synchronized get() = sharedPreferences.getBoolean("offlineModeEnabled", false)
}