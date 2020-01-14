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
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

class ClientSocketThread(
    private val cameraPreview: CameraPreview,
    private val context: Context,
    private val handler: Handler,
    private val callback: Callback
) : Runnable {

    // TODO: refactor threading: currently new threads are launched when connection to host fails

    companion object {
        private const val TAG = "ClientSocketThread"
    }

    private var socket: Socket? = null
    private var settingsRepository = SettingsRepository(context)
    private var outputStream: OutputStream? = null
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
        Log.d("#BUG", "connectToServer")
        GlobalScope.launch {
            try {
                Log.d("#BUG", "connectToServer2")
                socket = Socket()
                socket?.connect(InetSocketAddress(settingsRepository.getServerAddress(), SharedConstants.SERVERPORT), 3000)
                socket?.keepAlive = true

                outputStream = socket?.getOutputStream()
                inputStream = DataInputStream(socket?.getInputStream())

                if (outputStream != null) {
                    startLooper()
                } else {
                    Log.d("#BUG", "outputStream == null")
                }

            } catch (e: Exception) {
                Log.d("#BUG", e.message ?: "")
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
            builder.setCancelable(false)

            try {
                val dialog = builder.show()
                dialog.setOnShowListener {
                    addressInput.clearFocus()
                    val retryButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    retryButton.isFocusable = true
                    retryButton.isFocusableInTouchMode = true
                    retryButton.requestFocus()
                    retryButton.requestFocusFromTouch()
                }
            } catch (e: WindowManager.BadTokenException) {
                // Activity was closed.
            }
        }
    }

    private fun startLooper() {
        Log.d("#BUG", "startLooper")
        try {
            while (shouldRun) {
/*                if(Thread.currentThread().isInterrupted) {
                    socket?.close()
                    outputStream?.close()
                    inputStream?.close()
                }*/
                Log.d("#BUG", "while loop")

                if (cameraPreview.frameBuffer != null
                    && cameraPreview.frameBuffer!!.size() > 0
                ) {
                    val dos = DataOutputStream(outputStream)

                    Log.d("#BUG", "sendData")

                    dos.writeUTF(HEADER_START)
                    dos.writeInt(cameraPreview.frameBuffer!!.size())
                    dos.writeUTF(HEADER_END)
                    dos.flush()

                    // Send image
                    dos.write(cameraPreview.frameBuffer!!.toByteArray())
                    dos.flush()

//                    Thread.sleep((1000 / TARGET_FPS).toLong())

                    try {
                        if (inputStream != null) {
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
                        } else {
                            Log.d(TAG, "inputStream == null")
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("#BUG", e.message ?: "no")
                    }
                } else {
                    Log.d("#BUG", "no data")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("#BUG", e.message ?: "no")


/*            try {
                outputStream?.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }

            try {
                inputStream?.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }*/

            if (e is SocketException || e is ErrnoException) {
                showConnectionRetryDialog()
            }
        }
    }

    interface Callback {
        fun onFaceCountChanged(count: Int)
        fun onEmotionChanged(emotion: String, confidence: Float)
    }
}
