package com.schloesser.masterthesis.presentation.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.classifier.Classifier
import com.schloesser.masterthesis.presentation.extension.toBitmap
import com.schloesser.masterthesis.presentation.extension.toGray
import com.schloesser.masterthesis.presentation.extension.toMat
import com.schloesser.shared.wifidirect.SharedConstants.Companion.FRAME_HEIGHT
import com.schloesser.shared.wifidirect.SharedConstants.Companion.FRAME_WIDTH
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_END
import com.schloesser.shared.wifidirect.SharedConstants.Companion.HEADER_START
import com.schloesser.shared.wifidirect.SharedConstants.Companion.SERVERPORT
import com.schloesser.shared.wifidirect.SharedConstants.Companion.TARGET_FPS
import kotlinx.android.synthetic.main.activity_video.*
import org.jetbrains.anko.doAsync
import org.opencv.core.*
import org.opencv.core.CvType.CV_32F
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class VideoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoActivity"
    }

    private var socketThread: Thread? = null
    private var outputStream: DataOutputStream? = null
    private var isPreviewUdaterRunning = true

    var serverSocket: ServerSocket? = null
    var socket: Socket? = null
    var lastFrame: Bitmap? = null
        @Synchronized set

    lateinit var emotionClassifier: Classifier

    private lateinit var javaFaceDetector: CascadeClassifier
    private lateinit var nativeFaceDetector: DetectionBasedTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        emotionClassifier = Classifier.create(this, Classifier.Model.FLOAT, Classifier.Device.GPU, 1)

        initFaceDetection()
    }

    private fun initFaceDetection() {
        try {
            // load cascade file from application resources
            val inputStream = resources.openRawResource(R.raw.lbpcascade_frontalface)
            val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
            val cascadeFile = File(cascadeDir, "lbpcascade_frontalface.xml")
            val os = FileOutputStream(cascadeFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int = 0
            while (bytesRead != -1) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead != -1) os.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            os.close()

            javaFaceDetector = CascadeClassifier(cascadeFile.absolutePath)
            if (javaFaceDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier")
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.absolutePath)

//            nativeFaceDetector = DetectionBasedTracker(cascadeFile.absolutePath, 0)

            cascadeDir.delete()

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Failed to load cascade. Exception thrown: $e")
        }
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

    private val faceRectColor = Scalar(0.0, 255.0, 0.0, 255.0)
//    private val faceDetection = FaceDetection()
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
                        val frameMat = lastFrame!!.toMat()

                        val grayFrameMat = Mat()
                        Imgproc.cvtColor(frameMat, grayFrameMat, Imgproc.COLOR_RGB2GRAY)

                        var emotion = ""
                        val faces = MatOfRect()
                        javaFaceDetector.detectMultiScale(frameMat, faces, 1.1, 2, 2, Size(30.0, 30.0), Size())
                        val facesArray = faces.toArray();

                        if (facesArray.isNotEmpty()) {

                            val croppedMat = Mat(grayFrameMat, facesArray[0])

                            val scaleSize = Size(100.0, 100.0)
                            val scaledFace = Mat()
                            Imgproc.resize(croppedMat, scaledFace, scaleSize)

                            imvFace.setImageBitmap(scaledFace.toBitmap())

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
                                outputStream!!.writeInt(facesArray.size)
                                outputStream!!.writeUTF(emotion)
                                outputStream!!.writeUTF(HEADER_END)
                                outputStream!!.flush()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                        facesArray.forEach { face ->
                            Imgproc.rectangle(frameMat, face.tl(), face.br(), faceRectColor, 3);
                        }
                        imvCameraPreview!!.setImageBitmap(frameMat.toBitmap())
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
