package com.schloesser.masterthesis

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    companion object {
        private const val DEFAULT_SERVER_ADDRESS = "192.168.178.36"
    }

    private val preferences: SharedPreferences by lazy { context.defaultSharedPreferences }
    private var serverAddressCache: String? = null


    fun getServerAddress(): String {
        if (serverAddressCache == null) {
            serverAddressCache = preferences.getString("server_address", DEFAULT_SERVER_ADDRESS)
                ?: DEFAULT_SERVER_ADDRESS
        }
        return serverAddressCache ?: DEFAULT_SERVER_ADDRESS
    }

    fun setServerAddress(address: String) {
        serverAddressCache = address
        preferences.edit().putString("server_address", address).apply()
    }
}