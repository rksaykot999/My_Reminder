package com.example.reminder

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import java.util.*

class ProductivityService : Service() {

    private val binder = ProductivityBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var timer: Timer? = null

    var selectedTool = 0 // 0: Pomo, 1: Timer, 2: Stopwatch
    
    var pomoTime = 25 * 60 * 1000L
    var isPomoRunning = false
    var pomoStage = "Focus"

    var timerTime = 10 * 60 * 1000L
    var maxTimerTime = 10 * 60 * 1000L
    var isTimerRunning = false

    var stopwatchTime = 0L
    var isStopwatchRunning = false
    val laps = mutableListOf<Long>()

    private val ID_POMO = 1001
    private val ID_TIMER = 1002
    private val ID_STOPWATCH = 1003
    private val ID_SERVICE = 1000

    inner class ProductivityBinder : Binder() {
        fun getService(): ProductivityService = this@ProductivityService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_POMO" -> isPomoRunning = true
            "PAUSE_POMO" -> isPomoRunning = false
            "RESET_POMO" -> { isPomoRunning = false; pomoTime = 25 * 60 * 1000L; pomoStage = "Focus" }
            "START_TIMER" -> isTimerRunning = true
            "PAUSE_TIMER" -> isTimerRunning = false
            "RESET_TIMER" -> { isTimerRunning = false; timerTime = maxTimerTime }
            "START_STOPWATCH" -> isStopwatchRunning = true
            "PAUSE_STOPWATCH" -> isStopwatchRunning = false
            "RESET_STOPWATCH" -> { isStopwatchRunning = false; stopwatchTime = 0L; laps.clear() }
            "STOP_SERVICE" -> { 
                stopAll()
                stopSelf() 
                return START_NOT_STICKY 
            }
        }
        
        ensureForeground()
        updateAllNotifications()
        startTicking()
        return START_STICKY
    }

    private fun stopAll() {
        isPomoRunning = false
        isTimerRunning = false
        isStopwatchRunning = false
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ID_POMO)
        manager.cancel(ID_TIMER)
        manager.cancel(ID_STOPWATCH)
    }

    private fun ensureForeground() {
        startForeground(ID_SERVICE, createServiceNotification())
    }

    private fun startTicking() {
        if (timer == null) {
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    handler.post { tick() }
                }
            }, 0, 100)
        }
    }

    private fun tick() {
        if (isPomoRunning && pomoTime > 0) {
            pomoTime -= 100
            if (pomoTime <= 0) {
                isPomoRunning = false
                showFinishedNotification("Pomodoro Finished", "Time to take a break!", 2001)
            }
        }
        if (isTimerRunning && timerTime > 0) {
            timerTime -= 100
            if (timerTime <= 0) {
                isTimerRunning = false
                showFinishedNotification("Timer Finished", "Your countdown has ended.", 2002)
            }
        }
        if (isStopwatchRunning) {
            stopwatchTime += 100
        }

        updateAllNotifications()
    }

    private fun updateAllNotifications() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Pomo
        if (isPomoRunning || pomoTime != 25 * 60 * 1000L) {
            manager.notify(ID_POMO, createToolNotification("Pomodoro ($pomoStage)", formatTime(pomoTime), "RESET_POMO"))
        } else {
            manager.cancel(ID_POMO)
        }

        // Timer
        if (isTimerRunning || timerTime != maxTimerTime) {
            manager.notify(ID_TIMER, createToolNotification("Timer", formatTime(timerTime), "RESET_TIMER"))
        } else {
            manager.cancel(ID_TIMER)
        }

        // Stopwatch
        if (isStopwatchRunning || stopwatchTime > 0) {
            manager.notify(ID_STOPWATCH, createToolNotification("Stopwatch", formatTimeWithMs(stopwatchTime), "RESET_STOPWATCH"))
        } else {
            manager.cancel(ID_STOPWATCH)
        }

        // If nothing is running and all are at reset state, we could potentially stop the service, 
        // but user might want to keep it. We'll keep the ID_SERVICE notification as long as service is alive.
    }

    private fun createToolNotification(title: String, content: String, resetAction: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val resetIntent = Intent(this, ProductivityService::class.java).apply { action = resetAction }
        val resetPendingIntent = PendingIntent.getService(this, resetAction.hashCode(), resetIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "PRODUCTIVITY_CHANNEL")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetPendingIntent)
            .build()
    }

    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, ProductivityService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 99, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "PRODUCTIVITY_CHANNEL")
            .setContentTitle("Productivity Tools")
            .setContentText("Tools are running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop All", stopPendingIntent)
            .build()
    }

    private fun showFinishedNotification(title: String, text: String, id: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, "PRODUCTIVITY_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
        manager.notify(id, builder.build())
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", totalSecs / 60, totalSecs % 60)
    }

    private fun formatTimeWithMs(ms: Long): String {
        val totalSecs = ms / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        val millis = (ms % 1000) / 10
        return String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, mins, secs, millis)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("PRODUCTIVITY_CHANNEL", "Productivity Tools", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
