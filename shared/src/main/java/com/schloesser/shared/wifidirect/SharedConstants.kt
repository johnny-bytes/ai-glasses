package com.schloesser.shared.wifidirect

class SharedConstants {

    companion object {
        const val SERVERPORT = 12345

        const val HEADER_START = "#HEADER#"
        const val HEADER_END = "#HEADER_END#"

        const val FRAME_WIDTH = 320
        const val FRAME_HEIGHT = 240

        const val TARGET_FPS = 15
        const val IMAGE_QUALITY = 90

        const val BROADCAST_FACE_COUNT = "com.schloesser.masterthesis.FACE_COUNT"
        const val PARAM_FACE_COUNT = "face_count"
    }
}