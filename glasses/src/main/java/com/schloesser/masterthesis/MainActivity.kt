package com.schloesser.masterthesis

import android.Manifest
import android.hardware.Camera
import android.os.Bundle
import com.vuzix.hud.actionmenu.ActionMenuActivity
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : ActionMenuActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_CODE = 231
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startVideoStreaming()
    }

    private var camera: Camera? = null
    private var preview: CameraPreview? = null
    private var thread: Thread? = null

    private fun startVideoStreaming() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {

            camera = getCameraInstance()
            preview = CameraPreview(this, camera)
            previewContainer.addView(preview)

            thread = Thread(ClientSocketThread(preview!!))
            thread?.start()

        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity,
                "Kamera Zugriff benötigt.",
                PERMISSION_REQUEST_CODE,
                Manifest.permission.CAMERA
            )
        }
    }

    private fun getCameraInstance(): Camera? {
        var camera: Camera? = null
        try {
            camera = Camera.open()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return camera
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
