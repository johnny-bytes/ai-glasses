package com.schloesser.masterthesis.data.repository

import android.content.Context
import com.schloesser.masterthesis.data.base.SingletonHolder

class SettingsRepository private constructor(context: Context) {

    companion object : SingletonHolder<SettingsRepository, Context>(::SettingsRepository)

    private var preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var sendFrameIntervalSeconds: Int
        @Synchronized set(value) = preferences.edit().putInt("sendFrameIntervalSeconds", value).apply()
        @Synchronized get() = preferences.getInt("sendFrameIntervalSeconds", 10)
}