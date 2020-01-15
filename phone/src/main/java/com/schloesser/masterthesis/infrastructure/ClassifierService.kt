package com.schloesser.masterthesis.infrastructure

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.schloesser.masterthesis.LauncherActivity
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.job.UploadFrameJob
import com.schloesser.masterthesis.data.repository.SettingsRepository
import com.schloesser.masterthesis.infrastructure.base.storeImage
import com.schloesser.shared.wifidirect.SharedConstants
import org.jetbrains.anko.doAsync
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException


class ClassifierService : Service(), ProcessFrameTask.Callback {

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "ai-glasses-notifications"
        const val ACTION_STOP_SERVICE = "stop-service"
        const val ACTION_SERVICE_STARTED = "service-started"
        const val ACTION_SERVICE_STOPPED = "service-stopped"
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_SERVICE_STARTED))
    }

    private var sessionId: Int? = -1
    private var sessionName: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action.equals(ACTION_STOP_SERVICE)) {
            stopSelf()
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        } else {
            sessionId = intent?.getIntExtra("sessionId", -1)
            sessionName = intent?.getStringExtra("sessionName")

            startForeground(NOTIFICATION_ID, getDefaultNotification())
            startServer(false)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isServerRunning = false
    private var serverRunnable: ServerRunnable? = null
    private var socketThread: Thread? = null
    private var outputStream: DataOutputStream? = null
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null


    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        serverSocket?.close()
        socket?.close()
        socketThread?.interrupt()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_SERVICE_STOPPED))
    }

    private fun startServer(reconnecting: Boolean) {
        if (!isServerRunning) {
            isServerRunning = true
            initServer(reconnecting)
        }
    }

    private fun stopServer() {
        isServerRunning = false
    }

    var lastFrame: Bitmap? = null
        @Synchronized set

    var lastFrameProcessed: Bitmap? = null
        @Synchronized set

    var lastFace: Bitmap? = null
        @Synchronized set

    private fun initServer(reconnect: Boolean) {
        updateNotification(if (reconnect) "Reconnecting..." else "Connecting...")
        doAsync {
            try {

                if (serverSocket == null) {
                    serverSocket = ServerSocket()
                    serverSocket?.reuseAddress = true
                    serverSocket?.bind(InetSocketAddress(SharedConstants.SERVERPORT))
                }

                socket = serverSocket!!.accept()
                socket?.keepAlive = true

                try {
                    outputStream = DataOutputStream(socket!!.getOutputStream())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                vibrate(false)
                updateNotification("Connected to " + socket!!.inetAddress.toString().removePrefix("/"))

                serverRunnable = ServerRunnable(socket!!, this@ClassifierService) { e ->
                    e.printStackTrace()

                    if (e is SocketTimeoutException) {
                        vibrate(true)
                        stopServer()
                        startServer(true)
                    }
                }

                socketThread = Thread(serverRunnable)
                socketThread!!.start()

                processingRunnable.run()
                uploadFrameRunnable.run()

            } catch (e: Exception) {
                updateNotification("An error occurred. Please restart the service.")
                vibrate(true)
                e.printStackTrace()
            }
        }
    }

    private val processFrameTask by lazy {
        ProcessFrameTask(this)
    }

    private val processingRunnable = object : Runnable {
        override fun run() {
            try {
                if (lastFrame != null) {
                    processFrameTask.run(lastFrame!!, this@ClassifierService)
//                    lastFrame = null // Set null to avoid multiple processing
                }
            } finally {
                if (isServerRunning)
                    Handler(Looper.getMainLooper()).post(this)
            }
        }
    }

    override fun onProcessFrameResults(faceCount: Int, emotionLabel: String, labelConfidence: Float, frame: Bitmap, processedCenterFace: Bitmap?) {
        sendProcessingResults(faceCount, emotionLabel, labelConfidence)
        lastFrameProcessed = frame
        lastFace = processedCenterFace
    }

    private fun sendProcessingResults(faceCount: Int, emotionLabel: String, labelConfidence: Float) {
        doAsync {
            try {
                outputStream!!.writeUTF(SharedConstants.HEADER_START)
                outputStream!!.writeInt(faceCount)
                outputStream!!.writeUTF(emotionLabel)
                outputStream!!.writeFloat(labelConfidence)
                outputStream!!.writeUTF(SharedConstants.HEADER_END)
                outputStream!!.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private var lastSentFrame: Bitmap? = null

    private val uploadFrameRunnable = object : Runnable {
        override fun run() {
            try {
                if (lastFrame != null && lastFrame != lastSentFrame) {
                    val file = lastFrame?.storeImage(this@ClassifierService)
                    UploadFrameJob.scheduleJob(this@ClassifierService, sessionId!!, file!!)
                    lastSentFrame = lastFrame
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (isServerRunning)
                    Handler(Looper.getMainLooper()).postDelayed(this, SettingsRepository.getInstance(this@ClassifierService).sendFrameIntervalSeconds * 1000L)
            }
        }
    }

    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "AI Glasses",
                NotificationManager.IMPORTANCE_LOW
            )

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private fun getDefaultNotification(): Notification {
        val notificationIntent = Intent(this, LauncherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val stopActionPending = Intent(this, ClassifierService::class.java).also {
            it.action = ACTION_STOP_SERVICE
        }
        val stopActionPendingIntent = PendingIntent.getService(this, 0, stopActionPending, PendingIntent.FLAG_CANCEL_CURRENT)

        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Vuzix Blade Service")
            .setStyle(getNotificationText("Starting..."))
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "Stop Service", stopActionPendingIntent)

        return notificationBuilder.build()
    }

    private fun updateNotification(message: String) {
        if (isServerRunning) {
            notificationBuilder
                .setContentText(message)
                .setStyle(getNotificationText(message))

            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun getNotificationText(message: String): NotificationCompat.BigTextStyle {
        val serviceStatus = "Status: %s".format(message)
        val session = "Session: %s".format(sessionName)
        return NotificationCompat.BigTextStyle().bigText(serviceStatus + "\n" + session)
    }

    private fun vibrate(error: Boolean) {
        val duration = if (error) 800L else 400L

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ClassifierService = this@ClassifierService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}