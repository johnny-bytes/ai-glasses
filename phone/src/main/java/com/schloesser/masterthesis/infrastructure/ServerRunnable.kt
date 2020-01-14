package com.schloesser.masterthesis.infrastructure

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import kotlinx.coroutines.delay
import org.jetbrains.anko.doAsync
import java.io.DataInputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.Socket
import java.net.SocketTimeoutException

class ServerRunnable @Throws(IOException::class)
constructor(
    private val socket: Socket, service: ClassifierService,
    private val callback: (Throwable) -> Unit
) : Runnable {

    companion object {
        private const val TAG = "ServerSocketThread"
        private const val READ_TIMEOUT = 3000;
    }

    private var shouldRun = true
    private val service: WeakReference<ClassifierService> = WeakReference(service)
    private var inputStream: DataInputStream? = null
    private val bitmapOptions by lazy {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        options
    }

    override fun run() {
        statusCheckRunnable.run()
        connectToClient()
    }

    fun stop() {
        shouldRun = false
    }

    private fun connectToClient() {
        doAsync {

            try {
                inputStream = DataInputStream(socket.getInputStream())
                if (inputStream != null) {
                    startLooper()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                callback(e)
            }
        }
    }

    private fun startLooper() {
        try {
            while (true) {

                if (inputStream != null) {
                    readInputStream()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(e)
        }
    }

    private var lastSocketRead: Long = 0

    private fun readInputStream() {
        if (inputStream!!.readUTF() == HEADER_START) {

            lastSocketRead = System.currentTimeMillis()

            val imgLength = inputStream!!.readInt()

            if (inputStream!!.readUTF() != HEADER_END) {
                Log.d(TAG, "Header End Tag not present.")
            }

            val buffer = ByteArray(imgLength)
            var len = 0
            while (len < imgLength) {
                len += inputStream!!.read(buffer, len, imgLength - len)
            }

            service.get()?.lastFrame = BitmapFactory.decodeByteArray(buffer, 0, buffer.size, bitmapOptions)
        }
    }

    private val statusCheckRunnable = object : Runnable {
        override fun run() {
            try {
                if(lastSocketRead > 0 && System.currentTimeMillis() - lastSocketRead > READ_TIMEOUT ) {
                    stop()
                    callback(SocketTimeoutException("Received no data from client within ${READ_TIMEOUT}ms."))
                }
            } finally {
                if (shouldRun)
                    Handler(Looper.getMainLooper()).postDelayed(this, 1000)
            }
        }
    }
}

