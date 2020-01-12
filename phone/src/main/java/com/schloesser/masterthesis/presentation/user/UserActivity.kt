package com.schloesser.masterthesis.presentation.user

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.schloesser.masterthesis.MainActivity
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.base.ApiFactory
import com.schloesser.masterthesis.data.repository.SessionRepository
import com.schloesser.masterthesis.entity.EmotionRecord
import com.schloesser.masterthesis.presentation.extension.gone
import com.schloesser.masterthesis.presentation.extension.visible
import kotlinx.android.synthetic.main.activity_user.*
import kotlinx.android.synthetic.main.item_emotion_record.view.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class UserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        btnUploadFace.setOnClickListener {
            openCameraIntent()
        }

        btnLogout.setOnClickListener {
            SessionRepository.token = null
            finish()
        }

        swipeContainer.setOnRefreshListener {
            loadEmotionRecords()
        }

        loadEmotionRecords()
    }

    private fun loadEmotionRecords() {
        ApiFactory.userApi.getAllEmotionRecords(SessionRepository.token ?: "")
            .enqueue(object : Callback<List<EmotionRecord>> {

                override fun onFailure(call: Call<List<EmotionRecord>>, t: Throwable) {
                    swipeContainer.isRefreshing = false
                    Toast.makeText(this@UserActivity, t.localizedMessage, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<List<EmotionRecord>>,
                    response: Response<List<EmotionRecord>>
                ) {
                    swipeContainer.isRefreshing = false

                    if (response.body() != null) {
                        populateEmotionRecords(response.body()!!)
                    }
                }
            })
    }

    private fun populateEmotionRecords(records: List<EmotionRecord>) {
        emotionRecordContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)

        records.forEach {
            val item = inflater.inflate(R.layout.item_emotion_record, emotionRecordContainer, false)
            item.txvEmotion.text = it.data
            item.txvDate.text = it.created?.replace("T", "  ")
            emotionRecordContainer.addView(item)
        }
    }


    @AfterPermissionGranted(MainActivity.PERMISSION_REQUEST)
    private fun openCameraIntent() {

        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            ImagePicker.with(this)
                .maxResultSize(500, 500)
                .start()

        } else {
            EasyPermissions.requestPermissions(
                this,
                "Kamera Zugriff benötigt.",
                MainActivity.PERMISSION_REQUEST,
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

        ApiFactory.userApi.sendFace(body, SessionRepository.token!!).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                loadingIndicator.gone()
                txvResult.text = t.toString()
                t.printStackTrace()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                loadingIndicator.gone()
                txvResult.text = response.body() ?: "Empty Response"
            }
        })
    }
}
