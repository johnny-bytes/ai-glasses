package com.schloesser.masterthesis.presentation.internal

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.base.AccessTokenAuthenticator
import com.schloesser.masterthesis.data.repository.SessionRepository
import com.schloesser.masterthesis.infrastructure.ClassifierService
import com.schloesser.masterthesis.presentation.login.LoginActivity
import com.schloesser.masterthesis.presentation.preview.PreviewActivity
import com.schloesser.masterthesis.presentation.selectSession.SelectSessionDialog
import com.schloesser.masterthesis.presentation.settings.SettingsActivity
import com.schloesser.masterthesis.presentation.uploadFrame.UploadFrameActivity
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder


class InternalActivity : AppCompatActivity() {

    private val broadcastReceiver by lazy {
        Receiver()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internal)

        val filter = IntentFilter().apply {
            addAction(ClassifierService.STATUS_SERVICE_STARTED)
            addAction(ClassifierService.STATUS_SERVICE_STOPPED)
            addAction(AccessTokenAuthenticator.BROADCAST_PERFORM_LOGOUT)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)

        if (!SessionRepository.getInstance(this).hasSession()) {
            performLogout()
        } else {
            initOpenCV()
            initLayout()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun initOpenCV() {
        OpenCVLoader.initAsync(
            OpenCVLoader.OPENCV_VERSION_2_4_6,
            this,
            object : LoaderCallbackInterface {
                override fun onManagerConnected(status: Int) {
                }

                override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
                }
            })

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "openCv cannot be loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startService() {
        SelectSessionDialog { session ->
            val intent = Intent(this, ClassifierService::class.java)
            intent.putExtra("sessionId", session.id)
            intent.putExtra("sessionName", session.name)
            ContextCompat.startForegroundService(this, intent)
        }.display(supportFragmentManager)
    }

    private fun stopService() {
        stopService(Intent(this, ClassifierService::class.java))
    }

    private fun initLayout() {
        initButtons()
        updateServiceControls(false)
        txvHostIp.text = "Host IP: %s".format(wifiIpAddress())
    }

    private fun initButtons() {
        btnPreview.setOnClickListener {
            startActivity(Intent(this, PreviewActivity::class.java))
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
        }

        btnUploadFrame.setOnClickListener {
            startActivity(Intent(this, UploadFrameActivity::class.java))
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun updateServiceControls(isRunning: Boolean) {
        btnService.text = if (isRunning) "Stop Host Service" else "Start Host Service"
        btnService.setOnClickListener {
            if (isRunning) stopService() else startService()
        }
    }

    private fun performLogout() {
        SessionRepository.getInstance(this).clearSession()
        stopService(Intent(this, ClassifierService::class.java))
        triggerRestart()
    }

    inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AccessTokenAuthenticator.BROADCAST_PERFORM_LOGOUT -> performLogout()
                else -> updateServiceControls(isServiceRunning())
            }
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (ClassifierService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun wifiIpAddress(): String? {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        var ipAddress = wifiManager.connectionInfo.ipAddress

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress)
        }

        val ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()

        var ipAddressString: String?
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).hostAddress
        } catch (ex: UnknownHostException) {
            Log.e("WIFIIP", "Unable to get host address.")
            ipAddressString = null
        }

        return ipAddressString
    }

    private fun triggerRestart() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}
