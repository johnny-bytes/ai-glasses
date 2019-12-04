package com.schloesser.shared.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.schloesser.shared.SharedConstants.Companion.SERVER_NAME
import com.schloesser.shared.SharedConstants.Companion.SERVER_UUID
import java.io.IOException

@SuppressLint("MissingPermission")
class AcceptThread(bluetoothAdapter: BluetoothAdapter, private var callback: AcceptCallback?) : Thread() {

    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVER_NAME, SERVER_UUID)
    }

    override fun run() {
        // Keep listening until exception occurs or a socket is returned.
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                Log.e("AcceptThread", "Socket's accept() method failed", e)
                shouldLoop = false
                null
            }
            socket?.also {
                callback?.onConnectionAccepted(it)
                mmServerSocket?.close()
                shouldLoop = false
            }
        }
    }

    fun cancel() {
        try {
            mmServerSocket?.close()
        } catch (e: IOException) {
            Log.e("AcceptThread", "Could not close the connect socket", e)
        }
        callback = null
    }

    interface AcceptCallback {
        fun onConnectionAccepted(socket: BluetoothSocket)
    }
}