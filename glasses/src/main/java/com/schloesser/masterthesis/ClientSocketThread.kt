package com.schloesser.masterthesis

import android.app.AlertDialog
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.system.ErrnoException
import android.util.Log
import android.view.WindowManager
import com.schloesser.shared.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.SharedConstants.Companion.TARGET_FPS
import com.schloesser.shared.bluetooth.BluetoothManager
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.SocketException

class ClientSocketThread(
    private val bluetoothManager: BluetoothManager,
    private val cameraPreview: CameraPreview,
    private val context: Context,
    private val handler: Handler,
    private val callback: Callback
) : Runnable {

    // TODO: refactor threading: currently new threads are launched when connection to host fails

    companion object {
        private const val TAG = "ClientSocketThread"
        private const val SERVERIP = "192.168.178.36"
//        private const val SERVERIP = "192.168.178.121"
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: DataInputStream? = null

    override fun run() {
        connectToServer()
    }

    private fun connectToServer() {
        bluetoothManager.startDiscoveryAndConnectIfAvailable { socket ->
            try {
                this.socket = socket
                outputStream = socket.outputStream
                inputStream = DataInputStream(socket.inputStream)

                if (outputStream != null) {
                    startLooper()
                }

            } catch (e: IOException) {
                e.printStackTrace()

                handler.post {
                    showConnectionRetryDialog()
                }
            }
        }
    }

    private fun showConnectionRetryDialog() {
        handler.post {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("Could not connect to $SERVERIP")
            builder.setPositiveButton("Retry") { _, _ -> connectToServer() }
            builder.setCancelable(false)

            try {
                builder.show()
            } catch (e: WindowManager.BadTokenException) {
                // Activity was closed.
            }
        }
    }

    private fun startLooper() {
        try {
            while (true) {
                Log.d(TAG, "tick")

                if (outputStream != null
                    && cameraPreview.frameBuffer != null
                    && cameraPreview.frameBuffer!!.size() > 0
                ) {
                    val dos = DataOutputStream(outputStream)

                    dos.writeUTF(HEADER_START)
                    dos.writeInt(cameraPreview.frameBuffer!!.size())
                    dos.writeUTF(HEADER_END)
                    dos.flush()

                    Log.d(TAG, "Attempting to send frame")

                    // Send image
                    dos.write(cameraPreview.frameBuffer!!.toByteArray())
                    dos.flush()

                    Log.d(TAG, "Sent frame")

                    Thread.sleep((1000 / TARGET_FPS).toLong())

                    Log.d(TAG, "Finished Sleeping")

                    try {
                        if (socket?.isConnected == true &&  inputStream != null) {
                            if (inputStream!!.readUTF() == HEADER_START) {

                                val faceCount = inputStream!!.readInt()
//                            handler.post { callback.onFaceCountChanged(faceCount) }

                                val emotion = inputStream!!.readUTF()
//                            handler.post { callback.onEmotionChanged(emotion) }

                                if (inputStream!!.readUTF() != HEADER_END) {
                                    Log.d(TAG, "Header End Tag not present.")
                                }
                            } else {
                                Log.d(TAG, "Start Header not present")
                            }
                        } else {
                            Log.d(TAG, "Could not send read is")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    Log.d(TAG, "Could not send frame: ${outputStream} / ${cameraPreview.frameBuffer}")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "tick ended")
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

    interface Callback {
        fun onFaceCountChanged(count: Int)
        fun onEmotionChanged(emotion: String)
    }
}
