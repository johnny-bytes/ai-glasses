package com.schloesser.masterthesis.infrastructure.implementations

import com.schloesser.masterthesis.infrastructure.base.GazeDetector
import org.opencv.core.Mat
import org.opencv.core.Rect

class SimpleGazeDetector: GazeDetector  {

    override fun getGazeFaceIndex(frame: Mat, faces: Array<Rect>): Int? {
        if(faces.isNotEmpty()) {
            var largestFaceIndex = 0
            var largestFaceSize = 0.0
            var index = 0

            faces.forEach {
                if(it.area() > largestFaceSize) {
                    largestFaceIndex = index
                    largestFaceSize = it.area()
                }
                index++
            }
            return largestFaceIndex
        }
        return null
    }
}