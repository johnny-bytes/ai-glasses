package com.schloesser.masterthesis.presentation.video.base

import org.opencv.core.Mat
import org.opencv.core.Rect

interface FaceDetector {
    fun detectFaces(image: Mat): Array<Rect>
}
