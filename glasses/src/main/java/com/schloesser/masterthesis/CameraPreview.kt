package com.schloesser.masterthesis

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.ByteArrayOutputStream

class CameraPreview(context: Context, private var camera: Camera?) : SurfaceView(context), SurfaceHolder.Callback, Camera.PreviewCallback {
    
    var frameBuffer: ByteArrayOutputStream? = null

    init {
        holder.addCallback(this)
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera!!.setPreviewDisplay(holder)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera!!.setPreviewCallback(null)
        camera!!.release()
        camera = null
    }

    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        try {
            camera!!.stopPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val parameters = camera!!.parameters
            parameters.setPreviewSize(320, 240)

            frameWidth = parameters.previewSize.width
            frameHeight = parameters.previewSize.height

            parameters.previewFormat = ImageFormat.NV21
            camera!!.parameters = parameters

            camera!!.setDisplayOrientation(90)

            camera!!.setPreviewCallback(this)
            camera!!.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        try {
            val yuvimage = YuvImage(data, ImageFormat.NV21, frameWidth, frameHeight, null)

            val baos = ByteArrayOutputStream()
            yuvimage.compressToJpeg(Rect(0, 0, frameWidth, frameHeight), 100, baos)
            frameBuffer = baos
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}

