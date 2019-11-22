package com.schloesser.masterthesis.presentation.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.classifier.Classifier
import com.schloesser.masterthesis.presentation.extension.toBitmap
import com.schloesser.masterthesis.presentation.extension.toGray
import com.schloesser.shared.wifidirect.SharedConstants.Companion.FRAME_HEIGHT
import com.schloesser.shared.wifidirect.SharedConstants.Companion.FRAME_WIDTH
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.wifidirect.SharedConstants.Companion.SERVERPORT
import com.schloesser.shared.wifidirect.SharedConstants.Companion.TARGET_FPS
import kotlinx.android.synthetic.main.activity_video.*
import org.jetbrains.anko.doAsync
import org.opencv.core.CvType
import org.opencv.core.CvType.CV_32F
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class VideoActivity : AppCompatActivity() {

    private var socketThread: Thread? = null
    private var outputStream: DataOutputStream? = null
    private var isPreviewUdaterRunning = true

    var serverSocket: ServerSocket? = null
    var socket: Socket? = null
    var lastFrame: Bitmap? = null
        @Synchronized set

    lateinit var emotionClassifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        emotionClassifier = Classifier.create(this, Classifier.Model.FLOAT, Classifier.Device.GPU, 1)
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
    private val grayscaleFace = Mat(FRAME_HEIGHT, FRAME_WIDTH, CvType.CV_8UC4)
    private val faceData: ByteBuffer by lazy {
        val buffer = ByteBuffer.allocateDirect(1 * 100 * 100 * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer
    }

    private val previewUpdater = object : Runnable {
        override fun run() {
            if (!isPreviewUdaterRunning) return

            try {
                Handler().post {
                    if (lastFrame != null) {

                        val mutableBitmap = lastFrame!!.copy(Bitmap.Config.RGB_565, true)
                        val faces = faceDetection.findFaces(mutableBitmap)
                        var emotion = ""

                        if (faces.isNotEmpty()) {
                            val face = faces[0]
                            val center = PointF()
                            face!!.getMidPoint(center)

                            val x = max(0, (center.x - face.eyesDistance() * 2).roundToInt())
                            val y = max(0, (center.y - face.eyesDistance() * 2).roundToInt())

                            val width = min((center.x + face.eyesDistance() * 4), mutableBitmap.width - center.x).roundToInt()
                            val height = min((center.y + face.eyesDistance() * 4), mutableBitmap.height - center.y).roundToInt()

                            val faceBitmap = Bitmap.createBitmap(mutableBitmap, x, y, width, height)

                            grayscaleFace.toGray(faceBitmap)

                            val scaleSize = Size(100.0, 100.0)
                            val scaledFace = Mat()
                            Imgproc.resize(grayscaleFace, scaledFace, scaleSize)

                            val greyFace = scaledFace.toBitmap()
                            imvFace.setImageBitmap(greyFace)

                            val floatMat = Mat()
                            scaledFace.convertTo(floatMat, CV_32F)

                            faceData.rewind()
                            for (i in 0 until 100) {
                                for (j in 0 until 100) {
                                    faceData.putFloat(floatMat.get(i, j)[0].toFloat())
                                }
                            }

                            val results = emotionClassifier.recognizeImage(faceData, 0)
                            emotion = results[0].title
                        }

                        doAsync {
                            try {
                                outputStream!!.writeUTF(HEADER_START)
                                outputStream!!.writeInt(faces.size)
                                outputStream!!.writeUTF(emotion)
                                outputStream!!.writeUTF(HEADER_END)
                                outputStream!!.flush()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
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
