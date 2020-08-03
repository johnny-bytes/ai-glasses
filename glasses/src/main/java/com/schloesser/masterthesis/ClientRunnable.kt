package com.schloesser.masterthesis

import android.app.AlertDialog
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Handler
import android.system.ErrnoException
import android.util.Log
import android.widget.EditText
import com.schloesser.shared.SharedConstants.Companion.FRAME_HEIGHT
import com.schloesser.shared.SharedConstants.Companion.FRAME_WIDTH
import com.schloesser.shared.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.SharedConstants.Companion.TARGET_FPS
import com.schloesser.shared.SharedConstants
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

class ClientRunnable(
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
        socket.close()
    }

    private fun connectToServer() {
        GlobalScope.launch {
            try {
                socket = Socket()
                socket.connect(
                    InetSocketAddress(
                        settingsRepository.getServerAddress(),
                        SharedConstants.SERVERPORT
                    ), 3000
                )
                socket.keepAlive = true

                outputStream = DataOutputStream(socket.getOutputStream())
                inputStream = DataInputStream(socket.getInputStream())

                shouldRun = true

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

            } catch (e: Exception) {
                // Activity was closed.
            }
        }
    }


    private fun startLooper() {
        try {
            while (shouldRun) {

                val bufferSize = cameraPreview.frameBuffer?.size ?: 0

                if (bufferSize > 0) {

                    val frameBytes = getFrame()

                    outputStream.writeUTF(HEADER_START)
                    outputStream.writeInt(frameBytes.size)
                    outputStream.writeUTF(HEADER_END)
                    outputStream.flush()

                    outputStream.write(frameBytes)
                    outputStream.flush()

                    Thread.sleep((1000 / TARGET_FPS).toLong())

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            try {
                outputStream.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }

            try {
                inputStream.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }

            if (e is SocketException || e is ErrnoException) {
                shouldRun = false
                showConnectionRetryDialog()
            }
        }
    }

    private fun getFrame(): ByteArray {

        val yuvimage = YuvImage(
            cameraPreview.frameBuffer, ImageFormat.NV21,
            FRAME_WIDTH, FRAME_HEIGHT, null
        )

        val baos = ByteArrayOutputStream()
        yuvimage.compressToJpeg(
            Rect(0, 0, FRAME_WIDTH, FRAME_HEIGHT),
            SharedConstants.IMAGE_QUALITY, baos
        )

        return baos.toByteArray()

    }

    private val readInputRunnable = Runnable {
        while (shouldRun) {
            try {

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
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    interface Callback {
        fun onFaceCountChanged(count: Int)
        fun onEmotionChanged(emotion: String, confidence: Float)
        fun onCloseApp()
    }
}
