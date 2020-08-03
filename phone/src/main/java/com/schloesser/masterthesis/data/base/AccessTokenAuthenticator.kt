package com.schloesser.masterthesis.data.base

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor
import com.schloesser.masterthesis.data.repository.SessionRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class AccessTokenAuthenticator(private val context: Context) : Authenticator {

    companion object {

        const val BROADCAST_PERFORM_LOGOUT = "logout"

        const val AUTHORIZATION_HEADER_NAME = "Authorization"
        const val AUTHORIZATION_PREFIX = "Bearer"

        fun getAuthorizationheader(accessToken: String) = "$AUTHORIZATION_PREFIX $accessToken"
    }

    private val TAG = "Authenticator"

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null

        Log.d(TAG, "Access Token expired.")

        val accessToken = SessionRepository.getInstance(context).accessToken

        if (!hasBearerAuthorizationToken(response) || accessToken == null) {
            Log.d(TAG, "Request does not need authorization or AT was not previously set.")
            return null
        }

        synchronized(this) {

            // Get possible new access token, that was requested in another thread
            val newAccessToken = SessionRepository.getInstance(context).accessToken

            // Check if access token already changed
            if (accessToken != newAccessToken && newAccessToken != null) {
                Log.d(TAG, "Access token has been refreshed in another thread.")
                return newRequestWithAccessToken(response.request, newAccessToken)
            }

            try {
                // Need to refresh the access token
                val updatedAccessToken = refreshAccessToken()

                if (updatedAccessToken != null) {
                    Log.d(TAG, "Access Token has been refreshed.")
                    SessionRepository.getInstance(context).accessToken = updatedAccessToken
                    return newRequestWithAccessToken(response.request, updatedAccessToken)
                } else {
                    Log.d(TAG, "Could not refresh access token.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            Log.d(TAG, "Could not refresh access token.")
            logout()
            return null
        }
    }

    private fun hasBearerAuthorizationToken(response: Response): Boolean {
        val header = response.request.header(AUTHORIZATION_HEADER_NAME)
        return header != null && header.startsWith(AUTHORIZATION_PREFIX)
    }

    private fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
        return request.newBuilder()
            .header(AUTHORIZATION_HEADER_NAME, getAuthorizationheader(accessToken))
            .build()
    }

    private val okHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(OkHttpProfilerInterceptor())
        builder.build()
    }

    private fun refreshAccessToken(): String? {

        val body = JSONObject().apply {
            put("refresh", SessionRepository.getInstance(context).refreshToken)
        }

        val jsonMediaType = "application/json".toMediaTypeOrNull()

        val request = Request.Builder()
            .url(ApiFactory.BASE_URL + "auth/refresh/")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (response.isSuccessful) {
            val jsonResponse = JSONObject(response.body.string() ?: "{}")

            if (jsonResponse.has("access")) {
                return jsonResponse.getString("access")
            }
        }

        return null
    }

    private fun logout() {
        SessionRepository.getInstance(context).clearSession()
        val logoutIntent = Intent(BROADCAST_PERFORM_LOGOUT)
        LocalBroadcastManager.getInstance(context).sendBroadcast(logoutIntent)
    }
}