package com.schloesser.masterthesis.infrastructure

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import org.jetbrains.anko.doAsync
import java.io.DataInputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.Socket

class ServerSocketThread @Throws(IOException::class)
constructor(private val socket: Socket, service: ClassifierService, private val callback: (Throwable) -> Unit) : Runnable {

    companion object {
        private const val TAG = "ServerSocketThread"
    }

    private val service: WeakReference<ClassifierService> = WeakReference(service)
    private var inputStream: DataInputStream? = null
    private val bitmapOptions by lazy {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        options
    }

    override fun run() {
        connectToClient()
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
            }
        }
    }

    private fun startLooper() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                if (inputStream != null) readInputStream()
            }
        } catch (e: Exception) {
            e.printStackTrace()
//            callback(e)
/*            try {
                inputStream?.close()
            } catch (e2: Exception) {
                e.printStackTrace()
            }*/
        }
    }

    private fun readInputStream() {
        if (inputStream!!.readUTF() == HEADER_START) {

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
}

