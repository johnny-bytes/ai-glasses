package com.schloesser.masterthesis.infrastructure.implementations

import android.content.Context
import android.util.Log
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.infrastructure.base.DetectionBasedTracker
import com.schloesser.masterthesis.infrastructure.base.FaceDetector
import com.schloesser.masterthesis.infrastructure.implementations.EmotionClassifier.FACE_SIZE
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class OpenCVCascadeFaceDetector(context: Context): FaceDetector {

    companion object {
        const val TAG = "OpenCVCascadeFaceDetector"
    }

    private lateinit var javaFaceDetector: CascadeClassifier
    private lateinit var nativeFaceDetector: DetectionBasedTracker

    init {
        try {
            val inputStream = context.resources.openRawResource(R.raw.lbpcascade_frontalface)
            val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
            val cascadeFile = File(cascadeDir, "lbpcascade_frontalface.xml")
            val os = FileOutputStream(cascadeFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int = 0
            while (bytesRead != -1) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead != -1) os.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            os.close()

            javaFaceDetector = CascadeClassifier(cascadeFile.absolutePath)
            if (javaFaceDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier")
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.absolutePath)

//            nativeFaceDetector = DetectionBasedTracker(cascadeFile.absolutePath, 0)

            cascadeDir.delete()

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Failed to load cascade. Exception thrown: $e")
        }
    }

    override fun detectFaces(image: Mat): Array<Rect> {
        val faces = MatOfRect()
        javaFaceDetector.detectMultiScale(image, faces, 1.1, 4, 2, Size(FACE_SIZE, FACE_SIZE), Size())
        return faces.toArray()
    }

}