package com.schloesser.shared.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.util.Log

class ConnectionHandler private constructor(private val context: Context) : BroadcastReceiver() {

    companion object {
        private const val TAG = "ConnectionHandler"
        const val SERVICE_NAME = "emotiondetection"

        @SuppressLint("StaticFieldLeak")
        private var instance: ConnectionHandler? = null

        @Synchronized
        fun getInstance(context: Context): ConnectionHandler {
            if (instance == null) {
                instance = ConnectionHandler(context)
            }
            return instance!!
        }
    }

    var clientCallback: ClientCallback? = null
    var hostCallback: HostCallback? = null

    private val intentFilter by lazy {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        intentFilter
    }

    private val manager: WifiP2pManager by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }

    private val channel: WifiP2pManager.Channel by lazy {
        manager.initialize(context, context.mainLooper, null)
    }

    fun connectToPeer(address: String) {

        val config = WifiP2pConfig().apply {
            deviceAddress = address
            wps.setup = WpsInfo.PBC
        }

        Log.d(TAG, "manager.connect()")
        manager.connect(channel, config, actionListener)
    }

    fun startService() {

        val record: Map<String, String> = mapOf(
            "listenport" to "4003",
            "servicename" to SERVICE_NAME
        )

        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_$SERVICE_NAME", "_http._tcp", record)

        manager.clearLocalServices(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                manager.addLocalService(channel, serviceInfo, actionListener)
                manager.discoverPeers(channel, actionListener)
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "Error: $reason")
            }

        })
    }

    private val services = mutableMapOf<String, String>()
    private val devices = mutableListOf<WifiP2pDevice>()

    fun discoverServices() {
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, record, device ->

            Log.d(TAG, "DnsSdTxtRecord available -$record")
            Log.d(TAG, "Device available -$device")

            record["servicename"]?.also {
                services[device.deviceAddress] = it
            }
        }

        val serviceListener =
            WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, device ->

                Log.d(TAG, "onDnsSdServiceAvailable $instanceName")
                Log.d(TAG, "onDnsSdServiceAvailable $device")

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                device.deviceName = services[device.deviceAddress] ?: device.deviceName

                devices.add(device)
                clientCallback?.onAvailableServicesChanged(devices)
            }

        manager.setDnsSdResponseListeners(channel, serviceListener, txtListener)

        manager.clearServiceRequests(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(), actionListener)
                manager.discoverServices(channel, actionListener)
                manager.discoverPeers(channel, actionListener)
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "Error: $reason")
            }
        })
    }

    fun onResume() {
        context.registerReceiver(this, intentFilter)
    }

    fun onPause() {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION")
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION")
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Log.d(TAG, "WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION")
            }
        }
    }

    private val actionListener by lazy {
        object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
            }

            override fun onFailure(reason: Int) {
                when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> {
                        Log.d(TAG, "P2P isn't supported on this device.")
                    }
                    else -> Log.d(TAG, "Error: $reason")
                }
            }
        }
    }

    interface ClientCallback {
        fun onAvailableServicesChanged(services: List<WifiP2pDevice>)
    }

    interface HostCallback {

    }
}