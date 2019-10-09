package com.schloesser.masterthesis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.data.base.ApiFactory
import com.schloesser.masterthesis.presentation.extension.gone
import com.schloesser.masterthesis.presentation.extension.visible
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.github.dhaval2404.imagepicker.ImagePicker
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody


class MainActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST = 231;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLogin.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        btnUpload.setOnClickListener { openCameraIntent() }
    }

    @AfterPermissionGranted(PERMISSION_REQUEST)
    private fun openCameraIntent() {

        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            ImagePicker.with(this)
                .maxResultSize(500, 500)
                .start()

        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity,
                "Kamera Zugriff benötigt.",
                PERMISSION_REQUEST,
                Manifest.permission.CAMERA
            )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            uploadFile(ImagePicker.getFile(data)!!)
        }
    }

    private fun getRequestBody(file: File): MultipartBody.Part {
        val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", file.name, reqFile)
    }

    private fun uploadFile(file: File) {
        loadingIndicator.visible()
        txvResult.text = ""

        val body = getRequestBody(file)

        ApiFactory.emotionRecordApi.sendFrame(body).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                loadingIndicator.gone()
                txvResult.text = t.toString()
                t.printStackTrace()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                loadingIndicator.gone()
                Log.d("#TEST", response.body() ?: "Empty Response")
                txvResult.text = response.body() ?: "Empty Response"
            }
        })
    }
}
