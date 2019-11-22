@file:Suppress("DEPRECATION")

package com.schloesser.masterthesis

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.schloesser.shared.wifidirect.SharedConstants.Companion.BROADCAST_FACE_COUNT
import com.schloesser.shared.wifidirect.SharedConstants.Companion.PARAM_FACE_COUNT
import com.vuzix.hud.actionmenu.ActionMenuActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.newFixedThreadPoolContext
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : ActionMenuActivity(), ClientSocketThread.Callback {

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_CODE = 231
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
            if(camera == null) {
                Toast.makeText(this, "Could not access camera.", Toast.LENGTH_LONG).show()
            }

            preview = CameraPreview(this, camera!!)
            previewContainer.addView(preview)

            thread = Thread(ClientSocketThread(preview!!, this, Handler(), this))
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
