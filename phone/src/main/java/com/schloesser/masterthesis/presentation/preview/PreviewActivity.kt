package com.schloesser.masterthesis.presentation.preview

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.infrastructure.ClassifierService
import com.schloesser.shared.SharedConstants.Companion.TARGET_FPS


open class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
            }
        }
        return true
    }

    private var service: ClassifierService? = null
    private var isBoundToService: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ClassifierService.LocalBinder
            this@PreviewActivity.service = binder.getService()
            isBoundToService = true
            processingRunnable.run()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBoundToService = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, ClassifierService::class.java).also { intent ->
            bindService(intent, connection, 0)
        }
    }

    private var processingRunnableStopped = false

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBoundToService = false
        processingRunnableStopped = true
    }

    private val processingRunnable = object : Runnable {
        override fun run() {
            try {
                if (service?.lastFrameProcessed != null) {
                    imvCameraPreview.setImageBitmap(service?.lastFrameProcessed)
                    imvFace.setImageBitmap(service?.lastFace)
                }
            } finally {
                if (!processingRunnableStopped)
                    Handler().postDelayed(this, TARGET_FPS.toLong())
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
    }
}
