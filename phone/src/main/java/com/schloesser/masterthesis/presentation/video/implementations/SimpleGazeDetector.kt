package com.schloesser.masterthesis.presentation.video.implementations

import com.schloesser.masterthesis.presentation.video.base.GazeDetector
import com.schloesser.masterthesis.presentation.video.base.Preprocessing
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class SimpleGazeDetector: GazeDetector  {

    override fun getGazeFaceIndex(frame: Mat, faces: Array<Rect>): Int? {
        if(faces.isNotEmpty()) {
            return 0
        }
        return null
    }
}