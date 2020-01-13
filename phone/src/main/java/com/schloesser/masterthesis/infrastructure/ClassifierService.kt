package com.schloesser.masterthesis.infrastructure

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.schloesser.masterthesis.LauncherActivity
import com.schloesser.masterthesis.R
import com.schloesser.shared.wifidirect.SharedConstants
import org.jetbrains.anko.doAsync
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket


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
            startServer()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isServerRunning = false

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_SERVICE_STOPPED))
    }

    private fun startServer() {
        if (!isServerRunning) {
            isServerRunning = true
            initServerSocket()
            processingRunnable.run()
        }
    }

    private var socketThread: Thread? = null
    private var outputStream: DataOutputStream? = null
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

    private fun stopServer() {
        isServerRunning = false
        if (socketThread != null) socketThread!!.interrupt()
        if (socket != null) socket!!.close()
        if (serverSocket != null) serverSocket!!.close()
        processFrameTask.stop()
    }

    var lastFrame: Bitmap? = null
        @Synchronized set

    var lastProcessedFrame: Bitmap? = null
        @Synchronized set

    var lastFace: Bitmap? = null
        @Synchronized set

    private fun initServerSocket() {
        updateNotification("Connecting...")
        doAsync {
            try {
                serverSocket = ServerSocket(SharedConstants.SERVERPORT)
                socket = serverSocket!!.accept()
                socket?.keepAlive = true

                try {
                    outputStream = DataOutputStream(socket!!.getOutputStream())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                updateNotification("Connected to " + socket!!.inetAddress.toString().removePrefix("/"))

                socketThread = Thread(ServerSocketThread(socket!!, this@ClassifierService) { _ ->
                    updateNotification("Connection lost")
                    lastFrame = null
                    lastProcessedFrame = null
                    lastFace = null

                    stopServer()
                    startServer()
                })
                socketThread!!.start()

            } catch (e: Exception) {
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
                    lastFrame = null // Set null to avoid multiple processing
                }
            } finally {
                Handler(Looper.getMainLooper()).post(this)
            }
        }
    }

    override fun onProcessFrameResults(faceCount: Int, emotionLabel: String, frame: Bitmap, processedCenterFace: Bitmap?) {
        sendProcessingResults(faceCount, emotionLabel)
        lastProcessedFrame = frame
        lastFace = processedCenterFace
    }

    private fun sendProcessingResults(faceCount: Int, emotionLabel: String) {
        doAsync {
            try {
                outputStream!!.writeUTF(SharedConstants.HEADER_START)
                outputStream!!.writeInt(faceCount)
                outputStream!!.writeUTF(emotionLabel)
                outputStream!!.writeUTF(SharedConstants.HEADER_END)
                outputStream!!.flush()
            } catch (e: IOException) {
                e.printStackTrace()
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
        notificationBuilder
            .setContentText(message + sessionName)
            .setStyle(getNotificationText(message))

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun getNotificationText(message: String): NotificationCompat.BigTextStyle {
        val serviceStatus = "Status: %s".format(message)
        val session = "Session: %s".format(sessionName)
        return NotificationCompat.BigTextStyle().bigText(serviceStatus + "\n" + session)
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ClassifierService = this@ClassifierService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }
}