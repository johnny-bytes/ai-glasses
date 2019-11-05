package com.schloesser.masterthesis

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.system.ErrnoException
import android.util.Log
import android.view.WindowManager
import com.schloesser.shared.wifidirect.SharedConstants
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.wifidirect.SharedConstants.Companion.TARGET_FPS
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

class ClientSocketThread(private val cameraPreview: CameraPreview, private val context: Context, private val handler: Handler, private val callback: Callback) : Runnable {

    // TODO: refactor threading: currently new threads are launched when connection to host fails

    companion object {
        private const val TAG = "ClientSocketThread"
        private const val SERVERIP = "192.168.178.36"
    }

    private var outputStream: OutputStream? = null
    private var inputStream: DataInputStream? = null

    override fun run() {
        connectToServer()
    }

    private fun connectToServer() {
        GlobalScope.launch {
            try {
                val socket = Socket(SERVERIP, SharedConstants.SERVERPORT)
                socket.keepAlive = true
                outputStream = socket.getOutputStream()
                inputStream = DataInputStream(socket.getInputStream())

                if (outputStream != null) {
                    startLooper()
                }

            } catch (e: IOException) {
                e.printStackTrace()
                showConnectionRetryDialog()
            }
        }
    }

    private fun showConnectionRetryDialog() {
        handler.post {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("Could not connect to $SERVERIP")
            builder.setPositiveButton("Retry") { _, _ -> connectToServer() }

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
                if (outputStream != null
                    && cameraPreview.frameBuffer != null
                    && cameraPreview.frameBuffer!!.size() > 0
                ) {
                    val dos = DataOutputStream(outputStream)

                    dos.writeUTF(HEADER_START)
                    dos.writeInt(cameraPreview.frameBuffer!!.size())
                    dos.writeUTF(HEADER_END)
                    dos.flush()

                    // Send image
                    dos.write(cameraPreview.frameBuffer!!.toByteArray())
                    dos.flush()

                    Thread.sleep((1000 / TARGET_FPS).toLong())
                }

                try {
                    if (inputStream != null) {
                        if (inputStream!!.readUTF() == HEADER_START) {

                            val faceCount = inputStream!!.readInt()
                            handler.post { callback.onFaceCountChanged(faceCount)}

                            if (inputStream!!.readUTF() != HEADER_END) {
                                Log.d(TAG, "Header End Tag not present.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            try {
                outputStream?.close()
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
    }
}
