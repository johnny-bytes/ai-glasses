package com.schloesser.masterthesis

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bluelinelabs.logansquare.LoganSquare
import com.bluelinelabs.logansquare.annotation.JsonField
import com.bluelinelabs.logansquare.annotation.JsonObject
import com.peak.salut.Callbacks.SalutCallback
import com.peak.salut.Salut
import com.peak.salut.SalutDataReceiver
import com.peak.salut.SalutServiceData
import com.vuzix.hud.actionmenu.ActionMenuActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.net.Socket
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




class MainActivity : ActionMenuActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ConnectionHandler.getInstance(this).onResume()

        btnConnect.setOnClickListener { startDiscovery() }
    }

    private val network: MySalut by lazy {
        val dataReceiver = SalutDataReceiver(this, null)
        val serviceData = SalutServiceData("ed", 1234, "glasses")

        MySalut(dataReceiver, serviceData, object : SalutCallback {
            override fun call() {
                Log.e(TAG, "Sorry, but this device does not support WiFi Direct.");
            }
        })
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

    private fun connectToServer() {

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
    }

    private lateinit var socket: Socket
    private lateinit var outputStream: DataOutputStream

    private fun initSocket(address: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                socket = Socket(address, 4003)
                outputStream = DataOutputStream(socket.getOutputStream())
            } catch (e: Throwable) {

                runOnUiThread {
                    Toast.makeText(this@MainActivity, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }
}
