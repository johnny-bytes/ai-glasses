package com.schloesser.masterthesis.presentation.video.implementations

import com.schloesser.masterthesis.presentation.video.base.Preprocessing
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class OpenCVPreprocessing: Preprocessing  {

    override fun processFrame(image: Mat): Mat {
        val outputMat = Mat()
        Imgproc.cvtColor(image, outputMat, Imgproc.COLOR_RGB2GRAY)
//        Imgproc.threshold(outputMat, outputMat, 2.0, 255.0, Imgproc.THRESH_BINARY);
        return outputMat
    }

    override fun processFace(frame: Mat, face: Rect): Mat {
        val croppedMat = Mat(frame, face)
//        val scaleSize = Size(48.0, 48.0)
        val scaleSize = Size(100.0, 100.0)
        val scaledFace = Mat()
        Imgproc.resize(croppedMat, scaledFace, scaleSize)
        return scaledFace
    }
}