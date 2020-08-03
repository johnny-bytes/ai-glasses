@file:Suppress("DEPRECATION")

package com.schloesser.masterthesis

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.schloesser.shared.SharedConstants.Companion.FRAME_HEIGHT
import com.schloesser.shared.SharedConstants.Companion.FRAME_WIDTH

// Camera 1 API is recommended for use with Vuzix Blade: https://www.vuzix.com/Developer/KnowledgeBase/Detail/1085
@SuppressLint("ViewConstructor")
class CameraPreview(context: Context, private var camera: Camera) : SurfaceView(context),
    SurfaceHolder.Callback, Camera.PreviewCallback {


    init {
        holder.addCallback(this)
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera.setPreviewDisplay(holder)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera.setPreviewCallback(null)
        camera.release()
        frameBuffer = null
    }

    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        try {
            camera.stopPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val parameters = camera.parameters
            parameters.setPreviewSize(FRAME_WIDTH, FRAME_HEIGHT)

            frameWidth = parameters.previewSize.width
            frameHeight = parameters.previewSize.height

            parameters.previewFormat = ImageFormat.NV21
            camera.parameters = parameters

            camera.setPreviewCallback(this)
            camera.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Volatile
    var frameBuffer: ByteArray? = null

    override fun onPreviewFrame(data: ByteArray?, camera: Camera) {
        frameBuffer = data
    }
}

