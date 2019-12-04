package com.schloesser.masterthesis.presentation.video

import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.R
import com.schloesser.shared.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.bluetooth.BluetoothManager
import kotlinx.android.synthetic.main.activity_video.*
import org.jetbrains.anko.doAsync
import java.io.DataOutputStream
import java.io.IOException


class VideoActivity : AppCompatActivity(), ProcessFrameTask.Callback {

    companion object {
        private const val TAG = "VideoActivity"
    }

    private var socketThread: Thread? = null
    private var outputStream: DataOutputStream? = null
    private var socket: BluetoothSocket? = null

    var lastFrame: Bitmap? = null
        @Synchronized set

    private val bluetoothManager by lazy {
        val manager = BluetoothManager(this, Handler())
        manager.init()
        manager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
    }

    override fun onResume() {
        super.onResume()

        bluetoothManager.makeDeviceDiscoverable(this) { socket ->
            initSocket(socket)
            processingRunnable.run()
        }
    }

    private fun initSocket(blSocket: BluetoothSocket) {
        doAsync {
            socket = blSocket
            try {
                try {
                    outputStream = DataOutputStream(socket!!.outputStream)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                showStatus("Connected to: " + socket!!.remoteDevice.name.toString())
                socketThread = Thread(ServerSocketThread(socket!!, this@VideoActivity))
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

    override fun onDestroy() {
        super.onDestroy()
        if (socketThread != null) socketThread!!.interrupt()
        if (socket != null) socket!!.close()
        processFrameTask.stop()
        bluetoothManager.onDestroy()
    }
}
