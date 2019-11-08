package com.schloesser.masterthesis.presentation.video

import android.graphics.*
import android.media.FaceDetector
import com.schloesser.shared.wifidirect.SharedConstants

class FaceDetection() {

    companion object {
        private const val MAX_FACES = 10 // max. number of faces to be detected
    }

    private val faceDetector by lazy {
        FaceDetector(SharedConstants.FRAME_WIDTH, SharedConstants.FRAME_HEIGHT, MAX_FACES)
    }

    fun findFaces(frame: Bitmap): List<FaceDetector.Face?> {
        val faces = arrayOfNulls<FaceDetector.Face>(MAX_FACES)
        faceDetector.findFaces(frame, faces)
        return faces.filterNotNull()
    }

    private val point = PointF()
    private val paint by lazy {
        val paint = Paint()
        paint.color = Color.RED
        paint.alpha = 100
        paint
    }

    fun drawFacesOnCanvas(faces: List<FaceDetector.Face?>, canvas: Canvas) {
        faces.forEach { face ->
            if (face != null) {
                face.getMidPoint(point)
                canvas.drawCircle(point.x, point.y, face.eyesDistance(), paint)
            }
        }
    }
}