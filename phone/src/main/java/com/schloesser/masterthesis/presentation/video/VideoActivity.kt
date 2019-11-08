package com.schloesser.masterthesis.presentation.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.R
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.wifidirect.SharedConstants.Companion.SERVERPORT
import com.schloesser.shared.wifidirect.SharedConstants.Companion.TARGET_FPS
import kotlinx.android.synthetic.main.activity_video.*
import org.jetbrains.anko.doAsync
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.roundToInt


class VideoActivity : AppCompatActivity() {

    private var socketThread: Thread? = null
    private var outputStream: DataOutputStream? = null
    private var isPreviewUdaterRunning = true

    var serverSocket: ServerSocket? = null
    var socket: Socket? = null
    var lastFrame: Bitmap? = null
        @Synchronized set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
    }

    override fun onResume() {
        super.onResume()
        initServerSocket()
        previewUpdater.run()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (socketThread != null) socketThread!!.interrupt()
        if (socket != null) socket!!.close()
        if (serverSocket != null) serverSocket!!.close()
        isPreviewUdaterRunning = false
    }

    private fun initServerSocket() {
        doAsync {
            try {
                serverSocket = ServerSocket(SERVERPORT)
                socket = serverSocket!!.accept()

                try {
                    outputStream = DataOutputStream(socket!!.getOutputStream())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                showStatus("Connected to:" + socket!!.inetAddress.toString())
                socketThread = Thread(ServerSocketThread(socket!!, this@VideoActivity))
                socketThread!!.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val faceDetection = FaceDetection()

    private val previewUpdater = object : Runnable {
        override fun run() {
            if (!isPreviewUdaterRunning) return

            try {
                Handler().post {
                    if (lastFrame != null) {

                        val mutableBitmap = lastFrame!!.copy(Bitmap.Config.RGB_565, true)
                        val faces = faceDetection.findFaces(mutableBitmap)

                        doAsync {
                            try {
                                outputStream!!.writeUTF(HEADER_START)
                                outputStream!!.writeInt(faces.size)
                                outputStream!!.writeUTF(HEADER_END)
                                outputStream!!.flush()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                        if (faces.isNotEmpty()) {
                            val face = faces[0]
                            val center = PointF()
                            face!!.getMidPoint(center)

                            val faceBitmap = Bitmap.createBitmap(
                                mutableBitmap,
                                (center.x - face.eyesDistance()).roundToInt(),
                                (center.y - face.eyesDistance()).roundToInt(),
                                face.eyesDistance().roundToInt() * 2,
                                face.eyesDistance().roundToInt() * 2
                            )

                            imvFace.setImageBitmap(faceBitmap)
                        }

                        val canvas = Canvas(mutableBitmap)
                        faceDetection.drawFacesOnCanvas(faces, canvas)
                        imvCameraPreview!!.setImageBitmap(mutableBitmap)
                    }
                }
            } finally {
                Handler().postDelayed(this, (1000 / TARGET_FPS).toLong())
            }
        }
    }

    private fun showStatus(message: String) {
        runOnUiThread { Toast.makeText(this@VideoActivity, message, Toast.LENGTH_LONG).show() }
    }
}
