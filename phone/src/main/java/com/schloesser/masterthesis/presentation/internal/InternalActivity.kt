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
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.repository.SessionRepository
import com.schloesser.masterthesis.infrastructure.ClassifierService
import com.schloesser.masterthesis.presentation.login.LoginActivity
import com.schloesser.masterthesis.presentation.preview.PreviewActivity
import com.schloesser.masterthesis.presentation.uploadFrame.UploadFrameActivity
import kotlinx.android.synthetic.main.activity_internal.*
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder


class InternalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internal)

        val filter = IntentFilter().apply {
            addAction(ClassifierService.ACTION_SERVICE_STARTED)
            addAction(ClassifierService.ACTION_SERVICE_STOPPED)
        }
        registerReceiver(Receiver(), filter)

        if (SessionRepository.token == null) {
            performLogout()
        } else {
            initOpenCV()
            startService()
            initLayout()
        }
    }

    private fun initOpenCV() {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, object : LoaderCallbackInterface {
            override fun onManagerConnected(status: Int) {
            }

            override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
            }
        })

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "openCv cannot be loaded", Toast.LENGTH_SHORT).show();
        }
    }

    private fun startService() {
        ContextCompat.startForegroundService(this, Intent(this, ClassifierService::class.java))
    }

    private fun stopService() {
        stopService(Intent(this, ClassifierService::class.java));
    }

    private fun initLayout() {
        initButtons()
        updateServiceControls(true)
        txvHostIp.text = "Host IP: %s".format(wifiIpAddress())
    }

    private fun initButtons() {
        btnUploadFrame.setOnClickListener {
            startActivity(Intent(this, UploadFrameActivity::class.java))
        }

        btnPreview.setOnClickListener {
            startActivity(Intent(this, PreviewActivity::class.java))
        }

        btnLogout.setOnClickListener {
            SessionRepository.token = null
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
        stopService(Intent(this, ClassifierService::class.java))
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateServiceControls(isServiceRunning())
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
}
