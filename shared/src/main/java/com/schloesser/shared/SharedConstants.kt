package com.schloesser.shared

import java.util.*

class SharedConstants {

    companion object {
        const val SERVER_PORT = 12345
        const val SERVER_NAME = "ARHappimeterServer"
        val SERVER_UUID = UUID.fromString("0e6452aa-1387-11ea-8d71-362b9e155667")

        const val HEADER_START = "#HEADER#"
        const val HEADER_END = "#HEADER_END#"

        const val FRAME_WIDTH = 1280
        const val FRAME_HEIGHT = 720

        const val TARGET_FPS = 15
        const val IMAGE_QUALITY = 90

        const val BROADCAST_FACE_COUNT = "com.schloesser.masterthesis.FACE_COUNT"
        const val PARAM_FACE_COUNT = "face_count"

        val EMOTION_LABELS = listOf("Angry", "Fear", "Happy", "Sad", "Surpirse", "Neutral")
    }
}