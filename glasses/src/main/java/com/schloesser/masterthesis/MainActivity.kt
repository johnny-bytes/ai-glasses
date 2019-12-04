@file:Suppress("DEPRECATION")

package com.schloesser.masterthesis

import android.Manifest
import android.annotation.SuppressLint
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.schloesser.shared.bluetooth.BluetoothManager
import com.vuzix.hud.actionmenu.ActionMenuActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.bluetoothManager
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : ActionMenuActivity(), ClientSocketThread.Callback {

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_CODE = 231
    }

    private val bluetoothManager by lazy {
        val manager = BluetoothManager(context, Handler())
        manager.init()
        manager
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startVideoStreaming()
    }

    private val camera: Camera? by lazy {
        var camera: Camera? = null
        try {
            camera = Camera.open()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        camera
    }

    private var preview: CameraPreview? = null
    private var thread: Thread? = null

    private fun startVideoStreaming() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            if (camera == null) {
                Toast.makeText(this, "Could not access camera.", Toast.LENGTH_LONG).show()
            }

            preview = CameraPreview(this, camera!!)
            previewContainer.addView(preview)

            thread = Thread(ClientSocketThread(bluetoothManager, preview!!, this, Handler(), this))
            thread?.start()

        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity,
                "",
                PERMISSION_REQUEST_CODE,
                Manifest.permission.CAMERA
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onFaceCountChanged(count: Int) {
        txvFaceCount.text = "Found $count faces."
    }

    override fun onEmotionChanged(emotion: String) {
        txvEmotion.text = emotion
    }

    override fun onDestroy() {
        super.onDestroy()
        thread?.interrupt()
        bluetoothManager.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startVideoStreaming()
    }
}
