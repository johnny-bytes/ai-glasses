package com.schloesser.masterthesis

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.system.ErrnoException
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import com.schloesser.shared.wifidirect.SharedConstants
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

class ClientSocketThread(
    private val cameraPreview: CameraPreview,
    private val context: Context,
    private val handler: Handler,
    private val callback: Callback
) : Runnable {

    companion object {
        private const val TAG = "ClientSocketThread"
    }

    private var socket: Socket? = null
    private var settingsRepository = SettingsRepository(context)
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null

    override fun run() {
        connectToServer()
    }

    private var shouldRun = true

    fun stop() {
        shouldRun = false
        cameraPreview.frameBuffer = null
        socket?.close()
    }

    private fun connectToServer() {
        GlobalScope.launch {
            try {
                socket = Socket()
                socket?.connect(
                    InetSocketAddress(
                        settingsRepository.getServerAddress(),
                        SharedConstants.SERVERPORT
                    ), 3000
                )
                socket?.keepAlive = true

                outputStream = DataOutputStream(socket?.getOutputStream())
                inputStream = DataInputStream(socket?.getInputStream())

                Thread(readInputRunnable).start()
                startLooper()

            } catch (e: Exception) {
                e.printStackTrace()
                showConnectionRetryDialog()
            }
        }
    }

    private fun showConnectionRetryDialog() {
        handler.post {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("Could not connect to ${settingsRepository.getServerAddress()}")

            val addressInput = EditText(context)
            addressInput.setText(settingsRepository.getServerAddress())

            builder.setView(addressInput)
            builder.setPositiveButton("Retry") { _, _ ->
                settingsRepository.setServerAddress(addressInput.text.toString().trim())
                connectToServer()
            }
            builder.setNegativeButton("Close App") { _, _ ->
                callback.onCloseApp()
            }
            builder.setCancelable(false)

            try {
                val dialog = builder.create()
                dialog.setOnShowListener {
                    val retryButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    retryButton.isFocusable = true
                    retryButton.isFocusableInTouchMode = true
                    retryButton.requestFocus()
                }
                dialog.show()

            } catch (e: WindowManager.BadTokenException) {
                // Activity was closed.
            }
        }
    }


    private fun startLooper() {
        try {
            while (shouldRun) {

                if (cameraPreview.frameBuffer != null
                    && cameraPreview.frameBuffer!!.size() > 0
                ) {

                    outputStream?.writeUTF(HEADER_START)
                    outputStream?.writeInt(cameraPreview.frameBuffer!!.size())
                    outputStream?.writeUTF(HEADER_END)
                    outputStream?.flush()

                    // Send image
                    outputStream?.write(cameraPreview.frameBuffer!!.toByteArray())
                    outputStream?.flush()
                }

//                Thread.sleep((1000 / TARGET_FPS).toLong())
            }
        } catch (e: Exception) {
            e.printStackTrace()

            try {
                outputStream?.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }

            try {
                inputStream?.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }

            if (e is SocketException || e is ErrnoException) {
                showConnectionRetryDialog()
            }
        }
    }

    private val readInputRunnable = Runnable {
        try {
            while (shouldRun) {

                if (inputStream!!.readUTF() == HEADER_START) {

                    val faceCount = inputStream!!.readInt()
                    handler.post { callback.onFaceCountChanged(faceCount) }

                    val emotion = inputStream!!.readUTF()
                    val confidence = inputStream!!.readFloat()
                    handler.post { callback.onEmotionChanged(emotion, confidence) }

                    if (inputStream!!.readUTF() != HEADER_END) {
                        Log.d(TAG, "Header End Tag not present.")
                    }
                }
            }
        } catch (e: IOException) {
                e.printStackTrace()
        }
    }


    interface Callback {
        fun onFaceCountChanged(count: Int)
        fun onEmotionChanged(emotion: String, confidence: Float)
        fun onCloseApp()
    }
}
