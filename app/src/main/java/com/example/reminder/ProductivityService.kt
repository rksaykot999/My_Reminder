package com.example.reminder

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
    private val ID_ALERTS = 2000

    private var lastUpdatePomo = 0L
    private var lastUpdateTimer = 0L
    private var lastUpdateStopwatch = 0L

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
        
        checkForegroundState()
        updateAllNotifications()
        startTicking()
        return START_STICKY
    }

    private fun checkForegroundState() {
        if (isPomoRunning || isTimerRunning || isStopwatchRunning) {
            ensureForeground()
        } else if (!hasActiveTools()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
    }

    private fun hasActiveTools(): Boolean {
        return (isPomoRunning || (pomoTime > 0 && pomoTime != 25 * 60 * 1000L)) ||
               (isTimerRunning || (timerTime > 0 && timerTime != maxTimerTime)) ||
               (isStopwatchRunning || stopwatchTime > 0)
    }

    private fun stopAll() {
        isPomoRunning = false
        isTimerRunning = false
        isStopwatchRunning = false
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ID_POMO)
        manager.cancel(ID_TIMER)
        manager.cancel(ID_STOPWATCH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun ensureForeground() {
        val notification = createServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(ID_SERVICE, notification, type)
        } else {
            startForeground(ID_SERVICE, notification)
        }
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
        val now = System.currentTimeMillis()
        if (isPomoRunning && pomoTime > 0) {
            pomoTime -= 100
            if (pomoTime <= 0) {
                pomoTime = 0
                isPomoRunning = false
                showFinishedNotification("Pomodoro Finished", "Time to take a break!", ID_ALERTS + 1)
            }
        }
        if (isTimerRunning && timerTime > 0) {
            timerTime -= 100
            if (timerTime <= 0) {
                timerTime = 0
                isTimerRunning = false
                showFinishedNotification("Timer Finished", "Your countdown has ended.", ID_ALERTS + 2)
            }
        }
        if (isStopwatchRunning) {
            stopwatchTime += 100
        }

        if (now - lastUpdatePomo >= 1000) {
            updatePomoNotification()
            lastUpdatePomo = now
        }
        if (now - lastUpdateTimer >= 1000) {
            updateTimerNotification()
            lastUpdateTimer = now
        }
        if (now - lastUpdateStopwatch >= 1000) {
            updateStopwatchNotification()
            lastUpdateStopwatch = now
        }

        checkForegroundState()
    }

    private fun updateAllNotifications() {
        updatePomoNotification()
        updateTimerNotification()
        updateStopwatchNotification()
    }

    private fun updatePomoNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (isPomoRunning || (pomoTime > 0 && pomoTime != 25 * 60 * 1000L)) {
            manager.notify(ID_POMO, createToolNotification("Pomodoro ($pomoStage)", formatTime(pomoTime), "RESET_POMO"))
        } else {
            manager.cancel(ID_POMO)
        }
    }

    private fun updateTimerNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (isTimerRunning || (timerTime > 0 && timerTime != maxTimerTime)) {
            manager.notify(ID_TIMER, createToolNotification("Timer", formatTime(timerTime), "RESET_TIMER"))
        } else {
            manager.cancel(ID_TIMER)
        }
    }

    private fun updateStopwatchNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (isStopwatchRunning || stopwatchTime > 0) {
            manager.notify(ID_STOPWATCH, createToolNotification("Stopwatch", formatTime(stopwatchTime), "RESET_STOPWATCH"))
        } else {
            manager.cancel(ID_STOPWATCH)
        }
    }

    private fun createToolNotification(title: String, content: String, resetAction: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, resetAction.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)

        val resetIntent = Intent(this, ProductivityService::class.java).apply { action = resetAction }
        val resetPendingIntent = PendingIntent.getService(this, resetAction.hashCode(), resetIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "PRODUCTIVITY_CHANNEL")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification_clock)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetPendingIntent)
            .build()
    }

    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 99, intent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, ProductivityService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 100, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "PRODUCTIVITY_CHANNEL")
            .setContentTitle("My Reminder")
            .setContentText("Focus session active")
            .setSmallIcon(R.drawable.ic_notification_clock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop All", stopPendingIntent)
            .build()
    }

    private fun showFinishedNotification(title: String, text: String, id: Int) {
        val dataManager = DataManager(this)
        if (!dataManager.isNotificationEnabled("focus_enabled")) return

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, "PRODUCTIVITY_ALERTS")
            .setSmallIcon(R.drawable.ic_notification_clock)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
        manager.notify(id, builder.build())
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", totalSecs / 60, totalSecs % 60)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val toolsChannel = NotificationChannel(
                "PRODUCTIVITY_CHANNEL",
                "Productivity Tools",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
            }
            val alertsChannel = NotificationChannel(
                "PRODUCTIVITY_ALERTS",
                "Productivity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(toolsChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
