package com.schloesser.masterthesis.presentation.video

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import com.schloesser.masterthesis.presentation.extension.toBitmap
import com.schloesser.masterthesis.presentation.extension.toMat
import com.schloesser.masterthesis.presentation.video.base.Classifier
import com.schloesser.masterthesis.presentation.video.implementations.*
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ProcessFrameTask(private val activity: Activity) {

    private var isRunning = true
    private val handler = Handler(activity.mainLooper)
    private val preprocessing = OpenCVPreprocessing()
    private val faceDetector = OpenCVCascadeFaceDetector(activity)
    private val gazeDetector = SimpleGazeDetector()
    private val emotionClassifier = EmotionClassifier(activity, Classifier.Device.GPU, 1)

    private val faceBuffer: ByteBuffer by lazy {
//        val buffer = ByteBuffer.allocateDirect(1 * 48 * 48 * 4)
        val buffer = ByteBuffer.allocateDirect(1 * 100 * 100 * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer
    }

    fun stop() {
        isRunning = false
    }

    fun run(frame: Bitmap, callback: Callback) {
        if (!isRunning) return

        var emotionLabel = ""
        var processedFace: Mat? = null
        var faceIndex: Int? = null

        val processedFrame = preprocessing.processFrame(frame.toMat())
        val faces = faceDetector.detectFaces(processedFrame)

        if (faces.isNotEmpty()) {

            faceIndex = gazeDetector.getGazeFaceIndex(processedFrame, faces)

            if (faceIndex != null) {
                processedFace = preprocessing.processFace(processedFrame, faces[faceIndex])

                fillFaceBuffer(processedFace)

                val results = emotionClassifier.recognizeImage(faceBuffer, 0)
                emotionLabel = results[0].title
            }
        } else {
            emotionLabel = ""
            processedFace = null
            faceIndex = null
        }

        faces.forEach { face ->
            Imgproc.rectangle(processedFrame, face.tl(), face.br(), Scalar(0.0, 255.0, 0.0, 255.0), 3)
        }

        if(faceIndex != null) {
            val face = faces[faceIndex]
            Imgproc.putText(processedFrame, emotionLabel, Point(face.tl().x + 20, face.tl().y + 50), Imgproc.FONT_HERSHEY_SIMPLEX, 1.5, Scalar(255.0, 255.0, 255.0), 2)
        }

        handler.post {
            callback.onProcessFrameResults(faces.size, emotionLabel, processedFrame.toBitmap(), processedFace?.toBitmap())
        }
    }

    private fun fillFaceBuffer(face: Mat) {
        faceBuffer.rewind()
        for (i in 0 until 100) {
            for (j in 0 until 100) {
/*        for (i in 0 until 48) {
            for (j in 0 until 48) {*/
                faceBuffer.putFloat(face.get(i, j)[0].toFloat())
            }
        }
    }

    interface Callback {
        // Always called on main thread
        fun onProcessFrameResults(faceCount: Int, emotionLabel: String, processedFrame: Bitmap, processedCenterFace: Bitmap?)
    }
}