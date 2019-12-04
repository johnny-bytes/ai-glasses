package com.schloesser.shared.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.schloesser.shared.SharedConstants.Companion.SERVER_UUID
import java.io.IOException

@SuppressLint("MissingPermission")
class ConnectThread(private val bluetoothAdapter: BluetoothAdapter, private val device: BluetoothDevice, private val callback: ConnectCallback?) : Thread() {

    private var mmSocket: BluetoothSocket? = null

    override fun run() {
        bluetoothAdapter.cancelDiscovery()

        mmSocket = device.createRfcommSocketToServiceRecord(SERVER_UUID)

        mmSocket?.use { socket ->
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect()

            callback?.onConnectedToServer(socket)
        }
    }

    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            Log.e("ConnectThread", "Could not close the client socket", e)
        }
    }

    interface ConnectCallback {
        fun onConnectedToServer(socket: BluetoothSocket)
    }
}