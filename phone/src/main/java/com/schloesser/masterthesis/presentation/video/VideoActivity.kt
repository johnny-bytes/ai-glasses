package com.schloesser.masterthesis.presentation.video

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.presentation.extension.gone
import com.schloesser.masterthesis.presentation.extension.visible
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.wifidirect.SharedConstants.Companion.SERVERPORT
import kotlinx.android.synthetic.main.activity_video.*
import org.jetbrains.anko.doAsync
import java.io.DataOutputStream
import java.io.IOException
import java.math.BigInteger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.nio.ByteOrder


class VideoActivity : AppCompatActivity(), ProcessFrameTask.Callback {

    companion object {
        private const val TAG = "VideoActivity"
    }

    private var socketThread: Thread? = null
    private var outputStream: DataOutputStream? = null
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    var lastFrame: Bitmap? = null
        @Synchronized set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        txvIpAddress.text = "Server address: ${wifiIpAddress()}"
    }

    override fun onResume() {
        super.onResume()
        startServer()
    }

    private fun startServer() {
        initServerSocket()
        processingRunnable.run()
    }

    private fun stopServer() {
        if (socketThread != null) socketThread!!.interrupt()
        if (socket != null) socket!!.close()
        if (serverSocket != null) serverSocket!!.close()
        processFrameTask.stop()
    }

    private fun initServerSocket() {
        doAsync {
            try {
                serverSocket = ServerSocket(SERVERPORT)
                socket = serverSocket!!.accept()

                try {
                    outputStream = DataOutputStream(socket!!.getOutputStream())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                showStatus("Connected to:" + socket!!.inetAddress.toString())

                socketThread = Thread(ServerSocketThread(socket!!, this@VideoActivity) { _ ->
                    showStatus("Connection lost")
                    imvCameraPreview.setImageBitmap(null)
                    imvFace.setImageBitmap(null)

                    stopServer()
                    startServer()
                })
                socketThread!!.start()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val processFrameTask by lazy {
        ProcessFrameTask(this)
    }

    private val processingRunnable = object : Runnable {
        override fun run() {
            try {
                if (lastFrame != null) {
                    processFrameTask.run(lastFrame!!, this@VideoActivity)
                    lastFrame = null // Set as null to avoid multiple processing
                }
            } finally {
                Handler().post(this)
            }
        }
    }

    override fun onProcessFrameResults(faceCount: Int, emotionLabel: String, frame: Bitmap, processedCenterFace: Bitmap?) {
        sendProcessingResults(faceCount, emotionLabel)
        imvCameraPreview.setImageBitmap(frame)
        imvFace.setImageBitmap(processedCenterFace)
    }

    private fun sendProcessingResults(faceCount: Int, emotionLabel: String) {
        doAsync {
            try {
                outputStream!!.writeUTF(HEADER_START)
                outputStream!!.writeInt(faceCount)
                outputStream!!.writeUTF(emotionLabel)
                outputStream!!.writeUTF(HEADER_END)
                outputStream!!.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun showStatus(message: String) {
        runOnUiThread { Toast.makeText(this@VideoActivity, message, Toast.LENGTH_LONG).show() }
    }

    override fun onPause() {
        super.onPause()
        stopServer()
    }

    protected fun wifiIpAddress(): String? {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        var ipAddress = wifiManager.connectionInfo.ipAddress

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress)
        }

        val ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()

        var ipAddressString: String?
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress()
        } catch (ex: UnknownHostException) {
            Log.e("WIFIIP", "Unable to get host address.")
            ipAddressString = null
        }

        return ipAddressString
    }
}
