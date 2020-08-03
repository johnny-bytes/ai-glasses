package com.schloesser.masterthesis.data.job

import android.content.Context
import android.util.Log
import androidx.work.*
import com.schloesser.masterthesis.data.base.ApiFactory
import retrofit2.Response
import java.io.File
import java.io.IOException

class UploadFrameJob(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val sessionId = inputData.getInt("sessionId", -1)
        val filePath = inputData.getString("filePath")!!
        val file = File(filePath)
        val body = file.getRequestBody()

        return try {
            val response: Response<String> =
                ApiFactory.getInstance(applicationContext).api.sendFrame(body, sessionId).execute()

            if (response.isSuccessful) {
                Log.d("UploadFrameJob", "success")
                Result.success()
            } else {
                Log.d("UploadFrameJob", "failure")
                Result.failure()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("UploadFrameJob", "failure2")
            Result.failure()
        } finally {
            try {
                file.delete()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {

        fun scheduleJob(context: Context, sessionId: Int, file: File) {

            val data = Data.Builder()
                .putInt("sessionId", sessionId)
                .putString("filePath", file.absolutePath)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val jobRequest = OneTimeWorkRequestBuilder<UploadFrameJob>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            Log.d("UploadFrameJob", "scheduleJob")

            WorkManager.getInstance(context).enqueue(jobRequest)
        }
    }
}