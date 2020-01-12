package com.schloesser.masterthesis.infrastructure.base

import org.opencv.core.Mat
import org.opencv.core.Rect

interface FaceDetector {
    fun detectFaces(image: Mat): Array<Rect>
}
