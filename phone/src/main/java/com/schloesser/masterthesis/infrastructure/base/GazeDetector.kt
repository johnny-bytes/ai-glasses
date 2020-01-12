package com.schloesser.masterthesis.infrastructure.base

import org.opencv.core.Mat
import org.opencv.core.Rect

interface GazeDetector {
    fun getGazeFaceIndex(frame: Mat, faces: Array<Rect>): Int?
}
