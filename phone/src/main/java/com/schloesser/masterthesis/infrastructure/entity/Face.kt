package com.schloesser.masterthesis.infrastructure.entity

import org.opencv.core.Rect

data class Face(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun size() = width * height

    fun toRect(): Rect {
        return Rect(x, y, width, height)
    }

    companion object {
        fun fromRect(rect: Rect): Face {
            return Face(
                x = rect.x,
                y = rect.y,
                width = rect.width,
                height = rect.height
            )
        }
    }
}