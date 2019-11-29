package com.schloesser.masterthesis.presentation.video.base

import org.opencv.core.Mat
import org.opencv.core.Rect

interface Preprocessing {
    fun processFrame(image: Mat): Mat
    fun processFace(frame: Mat, face: Rect): Mat
}
