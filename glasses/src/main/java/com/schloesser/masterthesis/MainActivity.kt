package com.schloesser.masterthesis

import android.Manifest
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import com.bluelinelabs.logansquare.LoganSquare
import com.bluelinelabs.logansquare.annotation.JsonField
import com.bluelinelabs.logansquare.annotation.JsonObject
import com.peak.salut.Callbacks.SalutCallback
import com.peak.salut.Salut
import com.peak.salut.SalutDataReceiver
import com.peak.salut.SalutServiceData
import com.vuzix.hud.actionmenu.ActionMenuActivity
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : ActionMenuActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST = 231
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            startVideoStreaming()

        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity,
                "Kamera Zugriff benötigt.",
                PERMISSION_REQUEST,
                Manifest.permission.CAMERA
            )
        }
    }

    private var camera: Camera? = null
    private var preview: CameraPreview? = null
    private var thread: Thread? = null

    @AfterPermissionGranted(PERMISSION_REQUEST)
    private fun startVideoStreaming() {
        camera = getCameraInstance()

        preview = CameraPreview(this, camera)
        previewContainer.addView(preview)

        thread = Thread(ClientSocketThread(preview!!))
        thread?.start()
    }

    private fun getCameraInstance(): Camera? {
        var c: Camera? = null
        try {
            c = Camera.open()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return c
    }


    /**
     * WIFI DIRECT
     */
    private val network: MySalut by lazy {
        val dataReceiver = SalutDataReceiver(this, null)
        val serviceData = SalutServiceData("ed", 1234, "glasses")

        MySalut(dataReceiver, serviceData, SalutCallback { Log.e(TAG, "Sorry, but this device does not support WiFi Direct."); })
    }

    private fun startDiscovery() {
        network.discoverNetworkServices(
            { device ->
                Log.d(TAG, "A device has connected with the name " + device.deviceName)

                network.registerWithHost(device,
                    {
                        initMessaging()
                        Log.d(TAG, "registerWithHost")
                    },

                    {
                        Log.d(TAG, "We failed to register.")
                    })
            },
            false
        )
    }

    private fun initMessaging() {
        btnSubmit.isEnabled = true
        btnSubmit.setOnClickListener {
            edtMessage.text.clear()
            sendMessage(edtMessage.text.toString())
        }
    }

    private fun sendMessage(message: String) {
        val myMessage = Message()
        myMessage.text = message
        network.sendToHost(myMessage) { Log.e(TAG, "Oh no! The data failed to send.") }
    }

    @JsonObject
    inner class Message {
        @JsonField
        var text: String? = null
    }

    class MySalut(dataReceiver: SalutDataReceiver?, salutServiceData: SalutServiceData?, deviceNotSupported: SalutCallback?) : Salut(dataReceiver, salutServiceData, deviceNotSupported) {
        override fun serialize(o: Any?): String {
            return LoganSquare.serialize(o)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        network.unregisterClient(false)
        thread?.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
