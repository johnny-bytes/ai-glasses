package com.schloesser.masterthesis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.schloesser.masterthesis.data.base.ApiFactory
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUpload.setOnClickListener { openCameraIntent() }
    }

    private val REQUEST_CAPTURE_IMAGE = 100

    private fun openCameraIntent() {
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (pictureIntent.resolveActivity(packageManager) != null) {
            //Create a file to store the image
            var photoFile: File? = null;
            try {
                photoFile = createImageFile();
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.schloesser.masterthesis.provider", photoFile
                );
                pictureIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    photoURI
                );
                startActivityForResult(
                    pictureIntent,
                    REQUEST_CAPTURE_IMAGE
                );
            }
        }
    }

    var imageFilePath: String = ""

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == Activity.RESULT_OK) {
            Log.d("#TEST", imageFilePath)
            uploadFile(imageFilePath)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val imageFileName = "IMG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        imageFilePath = image.getAbsolutePath()
        return image
    }

    private fun uploadFile(filePath: String) {
        val emotionRecordApi = ApiFactory().emotionRecordApi

        val file = File(filePath)
        val requestBody: RequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file);
        val body = MultipartBody.Part.createFormData("file", "test_frame.jpg", requestBody)

        loadingIndicator.visibility = View.VISIBLE
        txvResult.text = ""

        emotionRecordApi.sendFrame(body).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                loadingIndicator.visibility = View.GONE
                txvResult.text = t.toString()
                t.printStackTrace()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                loadingIndicator.visibility = View.GONE
                Log.d("#TEST", response.body() ?: "Empty Response")
                txvResult.text = response.body() ?: "Empty Response"
            }
        })
    }
}
