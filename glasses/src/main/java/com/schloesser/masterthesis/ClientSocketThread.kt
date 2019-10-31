package com.schloesser.masterthesis

import android.util.Log
import com.schloesser.shared.wifidirect.SharedConstants
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket

class ClientSocketThread(private val cameraPreview: CameraPreview) : Runnable {

    companion object {
        private const val TAG = "ClientSocketThread"
        private const val SERVERIP = "192.168.178.36"
    }

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    init {
        GlobalScope.launch {
            try {
                socket = Socket(SERVERIP, SharedConstants.SERVERPORT)
                socket!!.keepAlive = true
                outputStream = socket!!.getOutputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun run() {
        try {
            while (true) {
                if (outputStream != null && cameraPreview.frameBuffer != null) {

                    Log.d(TAG, "Start sending image.")

                    val dos = DataOutputStream(outputStream)

                    dos.writeInt(4)
                    dos.writeUTF("#@@#")
                    dos.writeInt(cameraPreview.frameBuffer!!.size())
                    dos.writeUTF("-@@-")
                    dos.flush()

                    // Send image
                    dos.write(cameraPreview.frameBuffer!!.toByteArray())
                    dos.flush()

                    Log.d(TAG, "Sent image.")

                    Thread.sleep((1000 / 15).toLong())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            try {
                outputStream?.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }
        }

    }
}
