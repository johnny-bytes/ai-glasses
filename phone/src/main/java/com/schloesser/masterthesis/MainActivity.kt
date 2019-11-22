package com.schloesser.masterthesis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bluelinelabs.logansquare.LoganSquare
import com.github.dhaval2404.imagepicker.ImagePicker
import com.peak.salut.Callbacks.SalutCallback
import com.peak.salut.Callbacks.SalutDataCallback
import com.peak.salut.Salut
import com.peak.salut.SalutDataReceiver
import com.peak.salut.SalutServiceData
import com.schloesser.masterthesis.data.base.ApiFactory
import com.schloesser.masterthesis.presentation.extension.gone
import com.schloesser.masterthesis.presentation.extension.visible
import com.schloesser.masterthesis.presentation.login.LoginActivity
import com.schloesser.masterthesis.presentation.video.VideoActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class MainActivity : AppCompatActivity()  {

    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST = 231
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLogin.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        btnUpload.setOnClickListener { openCameraIntent() }
        btnConnect.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java))
        }

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, object: LoaderCallbackInterface {
            override fun onManagerConnected(status: Int) {
                Log.d(TAG, "onManagerConnected")
            }

            override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
                Log.d(TAG, "onPackageInstall")
            }
        })

        if(OpenCVLoader.initDebug()){
            Toast.makeText(this, "openCv successfully loaded", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "openCv cannot be loaded", Toast.LENGTH_SHORT).show();
        }
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


/*    private val network: MySalut by lazy {
        val dataReceiver = SalutDataReceiver(this, null)
        val serviceData = SalutServiceData("ed", 50489, "glasses")

        MySalut(dataReceiver, serviceData, SalutCallback { Log.e(TAG, "Sorry, but this device does not support WiFi Direct."); })
    }

    private fun startServer() {
        network.startNetworkService { device -> Log.d(TAG, device?.readableName + " has connected!"); }
    }*/

/*    override fun onDataReceived(data: Any?) {
        Log.d(TAG, "Data: $data")
    }

    class MySalut(dataReceiver: SalutDataReceiver?, salutServiceData: SalutServiceData?, deviceNotSupported: SalutCallback?) : Salut(dataReceiver, salutServiceData, deviceNotSupported) {
        override fun serialize(o: Any?): String {
            return LoganSquare.serialize(o)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        network.stopNetworkService(false);
    }*/
}
