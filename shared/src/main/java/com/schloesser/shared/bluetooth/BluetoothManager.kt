package com.schloesser.shared.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.schloesser.shared.SharedConstants.Companion.SERVER_NAME

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context, private val handler: Handler) : BroadcastReceiver() {

    private lateinit var bluetoothAdapter: BluetoothAdapter

    fun init(): Boolean {
        val nullableBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        if (nullableBluetoothAdapter == null) {
            showToast("Bluetooth not supported on this device.")
            return false
        } else {
            bluetoothAdapter = nullableBluetoothAdapter
        }

        if (!bluetoothAdapter.isEnabled) {
            showToast("Please enable Bluetooth.")
            return false
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(this, filter)

        return true
    }

    private var clientCallback: ((BluetoothSocket) -> Unit)? = null
    private var connectThread: ConnectThread? = null

    fun startDiscoveryAndConnectIfAvailable(callback: (BluetoothSocket) -> Unit) {
        this.clientCallback = callback
//        bluetoothAdapter.startDiscovery()

        val device = bluetoothAdapter.bondedDevices.find { it.name == "G6" }

        if (device != null) {
            connectThread = ConnectThread(bluetoothAdapter, device, object : ConnectThread.ConnectCallback {
                override fun onConnectedToServer(socket: BluetoothSocket) {
                    clientCallback?.invoke(socket)
                }
            })
            connectThread?.start()
        }

/*        bluetoothAdapter.bondedDevices.forEach { device ->
            if(device.name == "G6") {

            }
        }*/
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                Log.d("#TEST", device?.name ?: "Device is null")

                if (device != null && device.name == SERVER_NAME) {
                    showToast("Bluetooth Device found.")
                    connectThread = ConnectThread(bluetoothAdapter, device, object : ConnectThread.ConnectCallback {
                        override fun onConnectedToServer(socket: BluetoothSocket) {
                            handler.post {
                                clientCallback?.invoke(socket)
                            }
                        }
                    })
                    connectThread?.start()
                }
            }
            else -> showToast("onReceive: ${intent.action}")
        }
    }

    private var acceptThread: AcceptThread? = null

    fun makeDeviceDiscoverable(activity: Activity, callback: (BluetoothSocket) -> Unit) {
/*        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        }
        activity.startActivity(discoverableIntent)*/

        acceptThread = AcceptThread(bluetoothAdapter, object : AcceptThread.AcceptCallback {
            override fun onConnectionAccepted(socket: BluetoothSocket) {
                handler.post {
                    callback(socket)
                }
            }
        })
        acceptThread?.start()
    }


    fun onDestroy() {
        context.unregisterReceiver(this)
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        acceptThread?.cancel()
        connectThread?.cancel()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}