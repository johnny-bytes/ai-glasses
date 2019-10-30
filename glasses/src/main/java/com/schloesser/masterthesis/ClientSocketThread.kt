package com.schloesser.masterthesis

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket

class ClientSocketThread(private val cameraPreview: CameraPreview) : Runnable {

    companion object {
        private const val TAG = "ClientSocketThread"
        private const val SERVERIP = "192.168.178.121"
        private const val SERVERPORT = 1337
    }

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    init {
        GlobalScope.launch {
            try {
                socket = Socket(SERVERIP, SERVERPORT)
                socket!!.keepAlive = true
                outputStream = socket!!.getOutputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun run() {
        Log.d(TAG, "run")

        try {
            while (true) {
                if (outputStream != null && cameraPreview.mFrameBuffer != null) {

                    val dos = DataOutputStream(outputStream)
                    dos.writeInt(4)
                    dos.writeUTF("#@@#")
                    dos.writeInt(cameraPreview.mFrameBuffer.size())
                    dos.writeUTF("-@@-")
                    dos.flush()
                    println(cameraPreview.mFrameBuffer.size())
                    dos.write(cameraPreview.mFrameBuffer.toByteArray())
                    dos.flush()
                    Thread.sleep((1000 / 30).toLong())
                } else {
                    Log.d(TAG, "socket is null")
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
