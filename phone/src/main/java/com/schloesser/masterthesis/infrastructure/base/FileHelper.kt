package com.schloesser.masterthesis.infrastructure.base

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object FileHelper {

    fun getOutputFile(context: Context): File {
        val cacheDir = File(context.cacheDir.toString())
        val fileName = "frame_${System.currentTimeMillis()}.jpg"
        return File(cacheDir.path + File.separator + fileName)
    }
}

fun Bitmap.storeImage(context: Context): File {
    val pictureFile = FileHelper.getOutputFile(context)
    try {
        val fos = FileOutputStream(pictureFile)
        this.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()
    } catch (e: FileNotFoundException) {
        Log.d("FileHelper", "File not found: " + e.message)
    } catch (e: IOException) {
        Log.d("FileHelper", "Error accessing file: " + e.message)
    }
    return pictureFile
}

fun File.getRequestBody(): MultipartBody.Part {
    val reqFile = this.asRequestBody("image/*".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", this.name, reqFile)
}
