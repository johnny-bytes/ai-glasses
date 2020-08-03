@file:Suppress("DEPRECATION")

package com.schloesser.masterthesis

import android.Manifest
import android.content.Context
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Toast
import com.vuzix.hud.actionmenu.ActionMenuActivity
import pub.devrel.easypermissions.EasyPermissions
import java.util.*


class MainActivity : ActionMenuActivity(), ClientRunnable.Callback {

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_CODE = 231
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aig:wakelock")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock.acquire()
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

    private var cameraPreview: CameraPreview? = null
    private var clientRunnable: ClientRunnable? = null
    private var thread: Thread? = null

    private fun startVideoStreaming() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            if (camera == null) {
                Toast.makeText(this, "Could not access camera.", Toast.LENGTH_LONG).show()
            }

            cameraPreview = CameraPreview(this, camera!!)
            previewContainer.addView(cameraPreview)

            clientRunnable = ClientRunnable(cameraPreview!!, this, Handler(), this)
            thread = Thread(clientRunnable)
            thread.start()

        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity,
                "",
                PERMISSION_REQUEST_CODE,
                Manifest.permission.CAMERA
            )
        }
    }

    override fun onFaceCountChanged(count: Int) {
        txvFaceCount.text = when (count) {
            0 -> "No faces detected"
            1 -> "Detected 1 face"
            else -> "Detected $count faces"
        }

        if (count == 0) txvEmotion.text = ""
    }

    private val emotionLabels = LimitedQueue<String>(3)

    override fun onEmotionChanged(emotion: String, confidence: Float) {
        if (emotion.isNotBlank()) {
            emotionLabels.add(emotion)
        }

        txvEmotion.text = getMedianEmotionLabel()

        if (emotion.isBlank() || confidence < 0) {
            txvEmotion.text = ""
        } else {
//            txvEmotion.text = "${emotion}: ${(confidence * 100f).roundToInt()}%"
            txvEmotion.text = getMedianEmotionLabel()
        }
    }

    override fun onCloseApp() {
        finish()
    }

    private fun getMedianEmotionLabel(): String {
        var result = ""

        if (emotionLabels.isNotEmpty()) {
            val grouped = emotionLabels.groupBy { it }
            val sorted = grouped.toList().sortedByDescending { (_, value) -> value.size }

            result = sorted[0].first
        }

        return result
    }

    override fun onStop() {
        super.onStop()
        thread.interrupt()
        clientRunnable?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock.release()
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

    inner class LimitedQueue<E>(private val limit: Int) : LinkedList<E>() {

        override fun add(o: E): Boolean {
            val added = super.add(o)
            while (added && this.size > limit) {
                super.remove()
            }
            return added
        }
    }
}
