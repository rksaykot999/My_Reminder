package com.example.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import androidx.core.content.edit
import com.example.reminder.ui.theme.ReminderTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

// --- Data Models ---
data class Reminder(
    val id: Int,
    val title: String,
    val description: String = "",
    val time: String,
    val date: String,
    val priority: String = "Low",
    val category: Category = Category.GENERAL,
    val isCompleted: Boolean = false,
    val scheduledTimestamp: Long = 0L
)

enum class Category(val label: String, val icon: ImageVector, val color: Color) {
    GENERAL("General", Icons.Rounded.Category, Color(0xFF6366F1)),
    CLASS("Class", Icons.Rounded.School, Color(0xFF10B981)),
    MEAL("Meal", Icons.Rounded.Restaurant, Color(0xFFF59E0B)),
    PERSONAL("Personal", Icons.Rounded.Person, Color(0xFFEC4899)),
    WORK("Work", Icons.Rounded.Work, Color(0xFF06B6D4))
}

data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val date: String
)

// --- Universal Premium Palette ---
val SuccessEmerald = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRose = Color(0xFFF43F5E)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

val LocalAccentColor = compositionLocalOf { Color(0xFF6366F1) }
val LocalBgColor = compositionLocalOf { Color(0xFF0F172A) }
val LocalSurfaceColor = compositionLocalOf { Color(0xFF1E293B) }
val LocalTextPrimary = compositionLocalOf { Color(0xFFF8FAFC) }
val LocalTextSecondary = compositionLocalOf { Color(0xFF94A3B8) }
val LocalIsDarkTheme = compositionLocalOf { true }

// --- Local Storage Manager ---
class DataManager(context: Context) {
    private val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveReminders(reminders: List<Reminder>) {
        val json = gson.toJson(reminders)
        prefs.edit { putString("reminders", json) }
    }

    fun loadReminders(): List<Reminder> {
        val json = prefs.getString("reminders", null) ?: return emptyList()
        val type = object : TypeToken<List<Reminder>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveNotes(notes: List<Note>) {
        val json = gson.toJson(notes)
        prefs.edit { putString("notes", json) }
    }

    fun loadNotes(): List<Note> {
        val json = prefs.getString("notes", null) ?: return emptyList()
        val type = object : TypeToken<List<Note>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveProfile(name: String, bio: String, photoUri: String?) {
        prefs.edit {
            putString("user_name", name)
            putString("user_bio", bio)
            putString("user_photo", photoUri)
        }
    }

    fun loadProfile(): Triple<String, String, String?> {
        val name = prefs.getString("user_name", "User Name") ?: "User Name"
        val bio = prefs.getString("user_bio", "Set your status") ?: "Set your status"
        val photoUri = prefs.getString("user_photo", null)
        return Triple(name, bio, photoUri)
    }

    fun saveTheme(theme: Int) {
        prefs.edit { putInt("app_theme", theme) }
    }

    fun loadTheme(): Int = prefs.getInt("app_theme", 0) // 0: Dark, 1: Light, 2: System

    fun saveAccent(color: Int) {
        prefs.edit { putInt("app_accent", color) }
    }

    fun loadAccent(): Int = prefs.getInt("app_accent", Color(0xFF6366F1).toArgb())

    fun saveNotificationEnabled(key: String, enabled: Boolean) {
        prefs.edit { putBoolean(key, enabled) }
    }

    fun isNotificationEnabled(key: String, default: Boolean = true): Boolean {
        return prefs.getBoolean(key, default)
    }

    fun setGuestMode(enabled: Boolean) {
        prefs.edit { putBoolean("is_guest_mode", enabled) }
    }

    fun isGuestMode(): Boolean = prefs.getBoolean("is_guest_mode", false)

    fun saveLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit { putLong("last_sync_timestamp", timestamp) }
    }

    fun getLastSyncTimestamp(): Long = prefs.getLong("last_sync_timestamp", 0L)
}

private fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
            ((this.red * 255.0f + 0.5f).toInt() shl 16) or
            ((this.green * 255.0f + 0.5f).toInt() shl 8) or
            (this.blue * 255.0f + 0.5f).toInt()
}

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels()
        checkNotificationPermission()
        enableEdgeToEdge()
        setContent {
            MainContainer()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Also check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent().apply {
                        action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback or ignore
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    "TASK_COMPLETED",
                    "Task Completions",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Silent notification channel for completed tasks"
                    setSound(null, null)
                },
                NotificationChannel(
                    "TASK_REMINDER_SILENT",
                    "Upcoming Task Warnings",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Silent countdown notification 2 minutes before task time"
                    setSound(null, null)
                },
                NotificationChannel(
                    "TASK_REMINDER",
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for scheduled task reminders with sound"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    "PRODUCTIVITY_CHANNEL",
                    "Productivity Tools",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Channel for timers and stopwatch"
                    setSound(null, null)
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val dataManager = DataManager(context)
        if (!dataManager.isNotificationEnabled("reminders_enabled")) return

        val action = intent.action
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val desc = intent.getStringExtra("desc") ?: "You have a task scheduled."
        val scheduledTimestamp = intent.getLongExtra("scheduledTimestamp", 0L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == "ACTION_DONE") {
            if (id != -1) {
                markReminderAsDone(context, id)
                notificationManager.cancel(id)
                notificationManager.cancel(id + 5000)
            }
            return
        }

        if (action == "ACTION_PRE_ALARM") {
            // 2 minutes before time: Silent notification with live countdown!
            val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
                this.action = "ACTION_DONE"
                putExtra("id", id)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context,
                id + 1000,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, "TASK_REMINDER_SILENT")
                .setSmallIcon(R.drawable.ic_notification_clock)
                .setContentTitle("Upcoming Task in 2 mins")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(if (scheduledTimestamp > 0) scheduledTimestamp else System.currentTimeMillis() + 120000)
                .setAutoCancel(true)
                .addAction(android.R.drawable.checkbox_on_background, "Mark Done", donePendingIntent)

            with(NotificationManagerCompat.from(context)) {
                if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(id + 5000, builder.build())
                }
            }
            return
        }

        // Exact Alarm Time: Sound notification!
        notificationManager.cancel(id + 5000) // Cancel silent pre-alarm

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = "ACTION_DONE"
            putExtra("id", id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            id + 1000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "TASK_REMINDER")
            .setSmallIcon(R.drawable.ic_notification_clock)
            .setContentTitle("Reminder: $title")
            .setContentText(if (desc.isNotBlank()) desc else "Time for your scheduled task!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", donePendingIntent)

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(id, builder.build())
            }
        }
    }
}

fun markReminderAsDone(context: Context, id: Int) {
    val dataManager = DataManager(context)
    val reminders = dataManager.loadReminders().toMutableList()
    val index = reminders.indexOfFirst { it.id == id }
    if (index != -1) {
        reminders[index] = reminders[index].copy(isCompleted = true)
        dataManager.saveReminders(reminders)
    }
}

@SuppressLint("MissingPermission", "PostNotifications")
fun showCompletionNotification(context: Context, taskTitle: String) {
    val builder = NotificationCompat.Builder(context, "TASK_COMPLETED")
        .setSmallIcon(R.drawable.ic_notification_clock)
        .setContentTitle("Task Finished!")
        .setContentText("You completed: $taskTitle")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSound(null)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}

fun scheduleReminder(context: Context, reminder: Reminder) {
    if (reminder.scheduledTimestamp <= System.currentTimeMillis()) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 1. Exact Alarm at scheduled time (WITH SOUND)
    val exactIntent = Intent(context, ReminderReceiver::class.java).apply {
        action = "ACTION_EXACT_ALARM"
        putExtra("id", reminder.id)
        putExtra("title", reminder.title)
        putExtra("desc", reminder.description)
        putExtra("scheduledTimestamp", reminder.scheduledTimestamp)
    }

    val exactPendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id,
        exactIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.scheduledTimestamp,
                exactPendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.scheduledTimestamp, exactPendingIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.scheduledTimestamp,
            exactPendingIntent
        )
    }

    // 2. Pre-Alarm 2 minutes before (SILENT WITH LIVE COUNTDOWN)
    val twoMinsMillis = 2 * 60 * 1000L
    val preAlarmTimestamp = reminder.scheduledTimestamp - twoMinsMillis
    if (preAlarmTimestamp > System.currentTimeMillis()) {
        val preIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_PRE_ALARM"
            putExtra("id", reminder.id)
            putExtra("title", reminder.title)
            putExtra("desc", reminder.description)
            putExtra("scheduledTimestamp", reminder.scheduledTimestamp)
        }

        val prePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id + 5000,
            preIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.set(AlarmManager.RTC_WAKEUP, preAlarmTimestamp, prePendingIntent)
    }
}

@Composable
fun MainContainer() {
    val context = LocalContext.current
    val dataManager = remember { DataManager(context) }
    val firebaseSyncManager = remember { FirebaseSyncManager(context) }

    var selectedItem by rememberSaveable { mutableStateOf(NavItem.HOME) }
    var showAuthDialog by remember { mutableStateOf(false) }

    var isGuestMode by remember { mutableStateOf(dataManager.isGuestMode()) }
    var userHasChosenAuthMode by remember {
        mutableStateOf(firebaseSyncManager.isLoggedIn || isGuestMode)
    }

    // --- APP STATE ---
    val reminders = remember {
        mutableStateListOf<Reminder>().apply { addAll(dataManager.loadReminders()) }
    }
    val notes = remember {
        mutableStateListOf<Note>().apply { addAll(dataManager.loadNotes()) }
    }

    val (initialName, initialBio, initialPhoto) = remember { dataManager.loadProfile() }
    var userName by rememberSaveable { mutableStateOf(initialName) }
    var userBio by rememberSaveable { mutableStateOf(initialBio) }
    var userPhotoUri by rememberSaveable { mutableStateOf(initialPhoto) }

    // Cloud sync handler
    val performCloudSync = {
        if (firebaseSyncManager.isLoggedIn) {
            firebaseSyncManager.syncRemindersToCloud(reminders)
            firebaseSyncManager.syncNotesToCloud(notes)
            firebaseSyncManager.fetchRemindersFromCloud { cloudReminders ->
                if (cloudReminders.isNotEmpty()) {
                    cloudReminders.forEach { cloudRem ->
                        val index = reminders.indexOfFirst { it.id == cloudRem.id }
                        if (index != -1) {
                            reminders[index] = cloudRem
                        } else {
                            reminders.add(cloudRem)
                        }
                    }
                    dataManager.saveReminders(reminders)
                }
            }
            firebaseSyncManager.fetchNotesFromCloud { cloudNotes ->
                if (cloudNotes.isNotEmpty()) {
                    cloudNotes.forEach { cloudNote ->
                        val index = notes.indexOfFirst { it.id == cloudNote.id }
                        if (index != -1) {
                            notes[index] = cloudNote
                        } else {
                            notes.add(cloudNote)
                        }
                    }
                    dataManager.saveNotes(notes)
                }
            }
        }
    }

    val handleLogoutToGuest = {
        firebaseSyncManager.signOut()
        dataManager.setGuestMode(true)
        dataManager.saveProfile("User Name", "Set your status", null)
        userName = "User Name"
        userBio = "Set your status"
        userPhotoUri = null
        isGuestMode = true
        userHasChosenAuthMode = true
        Toast.makeText(context, "Logged out. Switched to Guest Mode.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        val lastSync = dataManager.getLastSyncTimestamp()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        if (firebaseSyncManager.isLoggedIn && (System.currentTimeMillis() - lastSync > oneDayMillis)) {
            performCloudSync()
        }
    }

    LaunchedEffect(firebaseSyncManager.currentUser) {
        val user = firebaseSyncManager.currentUser
        if (user != null) {
            val nameFromAuth = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: userName
            val photoFromAuth = user.photoUrl?.toString()

            userName = nameFromAuth
            if (!photoFromAuth.isNullOrEmpty()) {
                userPhotoUri = photoFromAuth
            }
            dataManager.saveProfile(userName, userBio, userPhotoUri)
        }
        performCloudSync()
    }

    // --- THEME & APPEARANCE STATE ---
    var appTheme by remember { mutableIntStateOf(dataManager.loadTheme()) }
    var accentColorInt by remember { mutableIntStateOf(dataManager.loadAccent()) }
    val currentAccentColor = remember(accentColorInt) { Color(accentColorInt) }

    val isDark = when (appTheme) {
        0 -> true
        1 -> false
        else -> isSystemInDarkTheme()
    }

    val dynamicBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val dynamicSurfaceColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val dynamicTextPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val dynamicTextSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var isAddingReminder by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var isAddingNote by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }

    // Save data when lists change & Sync with Firebase
    LaunchedEffect(reminders.size, reminders.count { it.isCompleted }) {
        dataManager.saveReminders(reminders)
        if (firebaseSyncManager.isLoggedIn) {
            firebaseSyncManager.syncRemindersToCloud(reminders)
        }
    }
    LaunchedEffect(notes.size) {
        dataManager.saveNotes(notes)
        if (firebaseSyncManager.isLoggedIn) {
            firebaseSyncManager.syncNotesToCloud(notes)
        }
    }

    // Back press & Double-tap to exit handling
    val activity = context as? ComponentActivity
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        when {
            isAddingReminder || reminderToEdit != null -> {
                isAddingReminder = false
                reminderToEdit = null
            }
            isAddingNote || noteToEdit != null -> {
                isAddingNote = false
                noteToEdit = null
            }
            showAuthDialog -> {
                showAuthDialog = false
            }
            selectedItem != NavItem.HOME -> {
                selectedItem = NavItem.HOME
            }
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000) {
                    activity?.finish()
                } else {
                    backPressedTime = currentTime
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAccentColor provides currentAccentColor,
        LocalBgColor provides dynamicBgColor,
        LocalSurfaceColor provides dynamicSurfaceColor,
        LocalTextPrimary provides dynamicTextPrimary,
        LocalTextSecondary provides dynamicTextSecondary,
        LocalIsDarkTheme provides isDark
    ) {
        ReminderTheme(darkTheme = isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalBgColor.current)
            ) {
                if (!userHasChosenAuthMode && !firebaseSyncManager.isLoggedIn) {
                    FullScreenAuthScreen(
                        firebaseSyncManager = firebaseSyncManager,
                        onAuthSuccess = {
                            dataManager.setGuestMode(false)
                            isGuestMode = false
                            userHasChosenAuthMode = true
                            performCloudSync()
                        },
                        onContinueGuest = {
                            dataManager.setGuestMode(true)
                            isGuestMode = true
                            userHasChosenAuthMode = true
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = selectedItem,
                            transitionSpec = {
                                fadeIn(tween(500)) + scaleIn(initialScale = 0.92f) togetherWith fadeOut(
                                    tween(500)
                                )
                            },
                            label = "NavigationTransition"
                        ) { destination ->
                            when (destination) {
                                NavItem.HOME -> GeneralHome(
                                    reminders = reminders,
                                    userName = userName,
                                    firebaseSyncManager = firebaseSyncManager,
                                    onOpenAuth = { showAuthDialog = true },
                                    onSyncCloud = { performCloudSync() },
                                    onNavigateToProfile = { selectedItem = NavItem.ME },
                                    onAddReminder = { isAddingReminder = true },
                                    onToggleReminder = { id ->
                                        val index = reminders.indexOfFirst { it.id == id }
                                        if (index != -1) {
                                            val wasCompleted = reminders[index].isCompleted
                                            reminders[index] = reminders[index].copy(isCompleted = !wasCompleted)
                                            if (!wasCompleted) showCompletionNotification(
                                                context,
                                                reminders[index].title
                                            )
                                            dataManager.saveReminders(reminders)
                                            if (firebaseSyncManager.isLoggedIn) {
                                                firebaseSyncManager.syncRemindersToCloud(reminders)
                                            }
                                        }
                                    },
                                    onEditReminder = { reminder -> reminderToEdit = reminder },
                                    onDeleteReminder = { id ->
                                        reminders.removeIf { it.id == id }
                                        dataManager.saveReminders(reminders)
                                        if (firebaseSyncManager.isLoggedIn) {
                                            firebaseSyncManager.deleteReminderFromCloud(id)
                                            firebaseSyncManager.syncRemindersToCloud(reminders)
                                        }
                                    }
                                )
                                NavItem.CALENDAR -> UniversalCalendar(
                                    reminders = reminders,
                                    onQuickAdd = { dateStr, title ->
                                        val newRem = Reminder(
                                            id = (reminders.maxOfOrNull { it.id } ?: 0) + 1,
                                            title = title,
                                            time = "09:00 AM",
                                            date = dateStr,
                                            scheduledTimestamp = System.currentTimeMillis() + 3600000
                                        )
                                        reminders.add(newRem)
                                        dataManager.saveReminders(reminders)
                                        if (firebaseSyncManager.isLoggedIn) {
                                            firebaseSyncManager.syncRemindersToCloud(reminders)
                                        }
                                    }
                                )
                                NavItem.NOTES -> GeneralNotes(
                                    notes = notes,
                                    onAddNote = { isAddingNote = true },
                                    onEditNote = { note -> noteToEdit = note },
                                    onDeleteNote = { id ->
                                        notes.removeIf { it.id == id }
                                        dataManager.saveNotes(notes)
                                        if (firebaseSyncManager.isLoggedIn) {
                                            firebaseSyncManager.deleteNoteFromCloud(id)
                                            firebaseSyncManager.syncNotesToCloud(notes)
                                        }
                                    }
                                )
                                NavItem.FOCUS -> ProductivityTools()
                                NavItem.ME -> UniversalProfile(
                                    name = userName,
                                    bio = userBio,
                                    photoUri = userPhotoUri,
                                    currentTheme = appTheme,
                                    currentAccent = currentAccentColor,
                                    firebaseSyncManager = firebaseSyncManager,
                                    onOpenAuth = { showAuthDialog = true },
                                    onSyncCloud = { performCloudSync() },
                                    onLogoutToGuest = handleLogoutToGuest,
                                    onUpdateProfile = { n, b, p ->
                                        userName = n
                                        userBio = b
                                        userPhotoUri = p
                                        dataManager.saveProfile(n, b, p)
                                    },
                                    onUpdateAppearance = { theme, accent ->
                                        appTheme = theme
                                        accentColorInt = accent.toArgb()
                                        dataManager.saveTheme(theme)
                                        dataManager.saveAccent(accent.toArgb())
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp)
                    ) {
                        UniversalDock(selectedItem = selectedItem, onItemSelected = { selectedItem = it })
                    }

                    if (showAuthDialog) {
                        AuthDialog(
                            firebaseSyncManager = firebaseSyncManager,
                            onDismiss = { showAuthDialog = false },
                            onAuthSuccess = {
                                showAuthDialog = false
                                performCloudSync()
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isAddingReminder || reminderToEdit != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        NewReminderScreen(
                            initialReminder = reminderToEdit,
                            onDismiss = { isAddingReminder = false; reminderToEdit = null },
                            onSave = { reminder ->
                                if (reminderToEdit != null) {
                                    val index = reminders.indexOfFirst { it.id == reminderToEdit!!.id }
                                    if (index != -1) reminders[index] = reminder.copy(id = reminderToEdit!!.id)
                                    scheduleReminder(context, reminders[index])
                                    reminderToEdit = null
                                } else {
                                    val newReminder = reminder.copy(id = (reminders.maxOfOrNull { it.id } ?: 0) + 1)
                                    reminders.add(newReminder)
                                    scheduleReminder(context, newReminder)
                                    isAddingReminder = false
                                }
                                dataManager.saveReminders(reminders)
                                if (firebaseSyncManager.isLoggedIn) {
                                    firebaseSyncManager.syncRemindersToCloud(reminders)
                                }
                            }
                        )
                    }

                    if (isAddingNote || noteToEdit != null) {
                        NewNoteDialog(
                            initialNote = noteToEdit,
                            onDismiss = { isAddingNote = false; noteToEdit = null },
                            onSave = { title, content ->
                                if (noteToEdit != null) {
                                    val index = notes.indexOfFirst { it.id == noteToEdit!!.id }
                                    if (index != -1) {
                                        notes[index] = noteToEdit!!.copy(title = title, content = content)
                                    }
                                    noteToEdit = null
                                } else {
                                    val dateStr =
                                        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Calendar.getInstance().time)
                                    notes.add(Note((notes.maxOfOrNull { it.id } ?: 0) + 1, title, content, dateStr))
                                    isAddingNote = false
                                }
                                dataManager.saveNotes(notes)
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class NavItem(val icon: ImageVector) {
    HOME(Icons.Rounded.Home),
    CALENDAR(Icons.Rounded.CalendarMonth),
    NOTES(Icons.AutoMirrored.Rounded.StickyNote2),
    FOCUS(Icons.Rounded.Timer),
    ME(Icons.Rounded.Person)
}

@Composable
fun UniversalDock(selectedItem: NavItem, onItemSelected: (NavItem) -> Unit) {
    Surface(
        modifier = Modifier
            .height(64.dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(32.dp),
        color = LocalSurfaceColor.current.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem.entries.forEach { item ->
                val isSelected = selectedItem == item
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) LocalAccentColor.current else Color.Transparent)
                        .clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else LocalTextSecondary.current,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralHome(
    reminders: List<Reminder>,
    userName: String,
    firebaseSyncManager: FirebaseSyncManager,
    onOpenAuth: () -> Unit,
    onSyncCloud: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onAddReminder: () -> Unit,
    onToggleReminder: (Int) -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onDeleteReminder: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPriorityFilter by remember { mutableStateOf("ALL") }

    val filteredReminders = reminders.filter { rem ->
        val matchesSearch = searchQuery.isBlank() ||
                rem.title.contains(searchQuery, ignoreCase = true) ||
                rem.description.contains(searchQuery, ignoreCase = true)
        val matchesPriority = selectedPriorityFilter == "ALL" || rem.priority.equals(selectedPriorityFilter, ignoreCase = true)
        matchesSearch && matchesPriority
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(72.dp)) }

            // Welcome Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "WELCOME BACK",
                            color = LocalAccentColor.current,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (firebaseSyncManager.isLoggedIn && userName.isNotBlank()) "Hi, $userName" else "My Reminder",
                            color = LocalTextPrimary.current,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(56.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            // Firebase Account Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToProfile() },
                    color = LocalSurfaceColor.current,
                    border = BorderStroke(1.dp, LocalAccentColor.current.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LocalAccentColor.current.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (firebaseSyncManager.isLoggedIn) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                    contentDescription = "Sync",
                                    tint = LocalAccentColor.current,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (firebaseSyncManager.isLoggedIn) "Firebase Cloud Connected" else "Guest Mode (Local Only)",
                                    color = LocalTextPrimary.current,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = firebaseSyncManager.currentUser?.email ?: "Tap to sign in or sync with cloud",
                                    color = LocalTextSecondary.current,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = "Go",
                            tint = LocalTextSecondary.current,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress Banner
            item {
                val completed = reminders.count { it.isCompleted }
                val total = reminders.size
                val progress = if (total > 0) completed.toFloat() / total else 0f
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = LocalAccentColor.current
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Column {
                            Text(
                                "DAILY GOAL",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                "$completed of $total Tasks Done",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Stats row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeStatCard(
                        "Reminders",
                        "${reminders.size}",
                        Icons.Rounded.Notifications,
                        WarningAmber,
                        Modifier.weight(1f)
                    )
                    HomeStatCard(
                        "Completed",
                        "${reminders.count { it.isCompleted }}",
                        Icons.Rounded.CheckCircle,
                        SuccessEmerald,
                        Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search reminders...", color = LocalTextSecondary.current.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = LocalAccentColor.current) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = LocalTextSecondary.current)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAccentColor.current,
                        unfocusedBorderColor = LocalSurfaceColor.current,
                        focusedContainerColor = LocalSurfaceColor.current,
                        unfocusedContainerColor = LocalSurfaceColor.current
                    )
                )
            }

            // Priority Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "PRIORITY FILTER",
                        color = LocalTextSecondary.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to LocalAccentColor.current, "HIGH" to DangerRose, "MEDIUM" to WarningAmber, "LOW" to SuccessEmerald)
                            .forEach { (priority, color) ->
                                val isSelected = selectedPriorityFilter == priority
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPriorityFilter = priority },
                                    label = { Text(priority, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = color,
                                        selectedLabelColor = Color.White,
                                        containerColor = LocalSurfaceColor.current,
                                        labelColor = LocalTextPrimary.current
                                    ),
                                    border = null
                                )
                            }
                    }
                }
            }

            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Upcoming Today",
                        color = LocalTextPrimary.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        "${filteredReminders.size} tasks",
                        color = LocalTextSecondary.current,
                        fontSize = 12.sp
                    )
                }
            }

            if (filteredReminders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventNote,
                            contentDescription = "Empty",
                            tint = LocalTextSecondary.current.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedPriorityFilter != "ALL") "No matching reminders found" else "No reminders yet! Tap + to add one",
                            color = LocalTextSecondary.current,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredReminders, key = { it.id }) { reminder ->
                    TaskRow(
                        reminder = reminder,
                        onToggle = { onToggleReminder(reminder.id) },
                        onEdit = { onEditReminder(reminder) },
                        onDelete = { onDeleteReminder(reminder.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
        }

        FloatingActionButton(
            onClick = onAddReminder,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 24.dp),
            containerColor = LocalAccentColor.current,
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "New Reminder")
        }
    }
}

@Composable
fun HomeStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = LocalSurfaceColor.current
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Column {
                Text(label, color = LocalTextSecondary.current, fontSize = 11.sp)
                Text(value, color = LocalTextPrimary.current, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun TaskRow(reminder: Reminder, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val priorityColor = when (reminder.priority.uppercase()) {
        "HIGH" -> DangerRose
        "MEDIUM" -> WarningAmber
        else -> SuccessEmerald
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        color = LocalSurfaceColor.current,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        2.dp,
                        if (reminder.isCompleted) SuccessEmerald else LocalAccentColor.current,
                        CircleShape
                    )
                    .padding(4.dp)
            ) {
                if (reminder.isCompleted) Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = SuccessEmerald,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    color = if (reminder.isCompleted) LocalTextSecondary.current else LocalTextPrimary.current,
                    fontWeight = FontWeight.Medium,
                    style = if (reminder.isCompleted) MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    ) else MaterialTheme.typography.bodyMedium
                )
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        color = LocalTextSecondary.current,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${reminder.date} • ${reminder.time}",
                        color = LocalTextSecondary.current,
                        fontSize = 11.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = reminder.category.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = reminder.category.label,
                            color = reminder.category.color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = reminder.priority.uppercase(),
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "More Options",
                        tint = LocalTextSecondary.current
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(LocalSurfaceColor.current)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = LocalTextPrimary.current) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = LocalAccentColor.current
                            )
                        },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = DangerRose) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = DangerRose
                            )
                        },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReminderScreen(
    initialReminder: Reminder? = null,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var notes by remember { mutableStateOf(initialReminder?.description ?: "") }
    var priority by remember { mutableStateOf(initialReminder?.priority ?: "Low") }
    var category by remember { mutableStateOf(initialReminder?.category ?: Category.GENERAL) }
    var selectedDate by remember {
        mutableStateOf(
            initialReminder?.date
                ?: SimpleDateFormat("MMM dd", Locale.getDefault()).format(Calendar.getInstance().time)
        )
    }
    var selectedTime by remember {
        mutableStateOf(initialReminder?.time ?: "09:00 AM")
    }

    val calendar = remember {
        Calendar.getInstance().apply {
            if (initialReminder != null && initialReminder.scheduledTimestamp > 0) {
                timeInMillis = initialReminder.scheduledTimestamp
            }
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            selectedDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, min ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, min)
            calendar.set(Calendar.SECOND, 0)
            selectedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = LocalBgColor.current
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = LocalTextPrimary.current
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "New Reminder",
                    color = LocalTextPrimary.current,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "TASK INFORMATION",
                            color = LocalTextSecondary.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    "What needs to be done?",
                                    color = LocalTextSecondary.current.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LocalAccentColor.current,
                                unfocusedBorderColor = LocalSurfaceColor.current,
                                focusedTextColor = LocalTextPrimary.current,
                                unfocusedTextColor = LocalTextPrimary.current
                            )
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = {
                                Text(
                                    "Add more details or notes...",
                                    color = LocalTextSecondary.current.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LocalAccentColor.current,
                                unfocusedBorderColor = LocalSurfaceColor.current,
                                focusedTextColor = LocalTextPrimary.current,
                                unfocusedTextColor = LocalTextPrimary.current
                            )
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "SET PRIORITY",
                            color = LocalTextSecondary.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("LOW" to SuccessEmerald, "MEDIUM" to WarningAmber, "HIGH" to DangerRose)
                                .forEach { (label, color) ->
                                    val isSelected = priority.equals(label, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clickable { priority = label },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) color else LocalSurfaceColor.current
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                label,
                                                color = if (isSelected) Color.White else color,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "SCHEDULE",
                            color = LocalTextSecondary.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScheduleCard(
                                "Date",
                                selectedDate,
                                Icons.Rounded.CalendarMonth,
                                Modifier.weight(1f)
                            ) { datePickerDialog.show() }
                            ScheduleCard(
                                "Time",
                                selectedTime,
                                Icons.Rounded.Schedule,
                                Modifier.weight(1f)
                            ) { timePickerDialog.show() }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "CATEGORY",
                            color = LocalTextSecondary.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(Category.entries) { cat ->
                                val isSelected = category == cat
                                Surface(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(80.dp)
                                        .clickable { category = cat },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) cat.color else LocalSurfaceColor.current,
                                    border = if (isSelected) null else BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            cat.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else cat.color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            cat.label,
                                            color = if (isSelected) Color.White else LocalTextPrimary.current,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(
                        Reminder(
                            0,
                            title,
                            notes,
                            selectedTime,
                            selectedDate,
                            priority = priority,
                            category = category,
                            scheduledTimestamp = calendar.timeInMillis
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current)
            ) {
                Text(
                    if (initialReminder != null) "Update Task" else "Create Task",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ScheduleCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = LocalSurfaceColor.current.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LocalAccentColor.current.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    color = LocalTextSecondary.current,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun UniversalCalendar(reminders: List<Reminder>, onQuickAdd: (String, String) -> Unit) {
    var calendarState by remember { 
        mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) 
    }
    var selectedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    // Clamp selected day when month changes
    LaunchedEffect(calendarState) {
        val maxDays = calendarState.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (selectedDay > maxDays) {
            selectedDay = maxDays
        }
    }
    var showDayDetail by remember { mutableStateOf(false) }
    var quickNote by remember { mutableStateOf("") }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())
    
    val currentMonthName = monthYearFormat.format(calendarState.time)
    val monthShort = monthShortFormat.format(calendarState.time)

    val filteredReminders = reminders.filter {
        it.date.contains(selectedDay.toString()) && it.date.contains(monthShort)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(72.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            LocalSurfaceColor.current.copy(alpha = 0.3f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            currentMonthName,
                            color = LocalTextPrimary.current,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Row {
                            IconButton(onClick = {
                                val newCal = calendarState.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                calendarState = newCal
                            }) {
                                Icon(
                                    Icons.Rounded.ChevronLeft,
                                    null,
                                    tint = LocalTextPrimary.current
                                )
                            }
                            IconButton(onClick = {
                                val newCal = calendarState.clone() as Calendar
                                newCal.add(Calendar.MONTH, 1)
                                calendarState = newCal
                            }) {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    null,
                                    tint = LocalTextPrimary.current
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { day ->
                            Text(
                                day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                color = if (day == "S") DangerRose else LocalTextSecondary.current,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Correct calendar grid logic
                    val daysInMonth = calendarState.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val firstDayOfWeek = (calendarState.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Align M=0, T=1...
                    
                    val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7
                    
                    for (row in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - firstDayOfWeek + 1
                                
                                if (dayNum in 1..daysInMonth) {
                                    val isSelected = dayNum == selectedDay
                                    val isToday = dayNum == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && 
                                                 calendarState.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) &&
                                                 calendarState.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> LocalAccentColor.current
                                                    isToday -> LocalAccentColor.current.copy(alpha = 0.2f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                selectedDay = dayNum
                                                showDayDetail = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$dayNum",
                                            color = if (isSelected) Color.White else LocalTextPrimary.current,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                Text(
                    text = "Reminders for $selectedDay $monthShort",
                    color = LocalTextPrimary.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (filteredReminders.isEmpty()) {
                item {
                    Text(
                        "No events scheduled",
                        color = LocalTextSecondary.current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(filteredReminders) { reminder ->
                    DayEventItem(
                        time = reminder.time.split(":")[0],
                        title = reminder.title,
                        duration = reminder.time,
                        color = reminder.category.color,
                        icon = reminder.category.icon
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        AnimatedVisibility(
            visible = showDayDetail,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showDayDetail = false }
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(32.dp),
                    color = LocalSurfaceColor.current,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "$selectedDay",
                                    color = LocalTextPrimary.current,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Details",
                                    color = LocalTextPrimary.current,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row {
                                Icon(
                                    Icons.AutoMirrored.Rounded.EventNote,
                                    null,
                                    tint = LocalTextSecondary.current,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    Icons.Rounded.EmojiEmotions,
                                    null,
                                    tint = LocalTextSecondary.current,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val dividerColor = LocalTextSecondary.current.copy(alpha = 0.2f)
                        Canvas(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)) {
                            drawLine(
                                color = dividerColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(modifier = Modifier.heightIn(max = 300.dp)) {
                            if (filteredReminders.isEmpty()) {
                                Text(
                                    "No events scheduled",
                                    color = LocalTextSecondary.current,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    items(filteredReminders) { reminder ->
                                        DayEventItem(
                                            time = reminder.time.split(":")[0],
                                            title = reminder.title,
                                            duration = reminder.time,
                                            color = reminder.category.color,
                                            icon = reminder.category.icon
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(LocalBgColor.current.copy(alpha = 0.5f), CircleShape)
                                .padding(start = 20.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = quickNote,
                                onValueChange = { quickNote = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LocalTextPrimary.current),
                                decorationBox = { innerTextField ->
                                    if (quickNote.isEmpty()) Text(
                                        "Add on $selectedDay $monthShort",
                                        color = LocalTextSecondary.current,
                                        fontSize = 14.sp
                                    )
                                    innerTextField()
                                }
                            )
                            Surface(
                                modifier = Modifier
                                    .size(40.dp),
                                shape = CircleShape,
                                color = LocalSurfaceColor.current,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                onClick = {
                                    if (quickNote.isNotBlank()) {
                                        onQuickAdd("$monthShort $selectedDay", quickNote)
                                        quickNote = ""
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        null,
                                        tint = LocalTextPrimary.current,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayEventItem(time: String, title: String, duration: String, color: Color, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            time,
            modifier = Modifier.width(45.dp),
            color = LocalTextPrimary.current,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LocalTextPrimary.current, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(duration, color = LocalTextSecondary.current, fontSize = 12.sp)
        }
        Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
    }
}

// --- UPDATED NOTES SECTION ---
@Composable
fun GeneralNotes(
    notes: List<Note>,
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit,
    onDeleteNote: (Int) -> Unit
) {
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 72.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    "My Notes",
                    color = LocalTextPrimary.current,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(notes) { note ->
                NoteCard(
                    note = note,
                    onClick = { selectedNote = note }
                )
            }
        }

        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 24.dp),
            containerColor = LocalAccentColor.current,
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "New Note")
        }
    }

    // Note Detail Dialog
    if (selectedNote != null) {
        Dialog(
            onDismissRequest = { selectedNote = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = LocalSurfaceColor.current,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedNote!!.title,
                            color = LocalTextPrimary.current,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(onClick = {
                                onEditNote(selectedNote!!)
                                selectedNote = null
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = LocalAccentColor.current)
                            }
                            IconButton(onClick = {
                                onDeleteNote(selectedNote!!.id)
                                selectedNote = null
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = DangerRose)
                            }
                            IconButton(onClick = { selectedNote = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = LocalTextSecondary.current)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        selectedNote!!.content,
                        color = LocalTextSecondary.current,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        selectedNote!!.date,
                        color = LocalAccentColor.current,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: Note, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = LocalSurfaceColor.current,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                note.title,
                color = LocalTextPrimary.current,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                note.content,
                color = LocalTextSecondary.current,
                fontSize = 13.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                note.date,
                color = LocalAccentColor.current,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// --- END NOTES SECTION ---

@Composable
fun NewNoteDialog(
    initialNote: Note? = null,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = LocalSurfaceColor.current
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (initialNote != null) "Edit Note" else "New Note",
                    color = LocalTextPrimary.current,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", color = LocalTextSecondary.current.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAccentColor.current,
                        unfocusedBorderColor = LocalBgColor.current,
                        focusedTextColor = LocalTextPrimary.current,
                        unfocusedTextColor = LocalTextPrimary.current
                    )
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Note content", color = LocalTextSecondary.current.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAccentColor.current,
                        unfocusedBorderColor = LocalBgColor.current,
                        focusedTextColor = LocalTextPrimary.current,
                        unfocusedTextColor = LocalTextPrimary.current
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LocalAccentColor.current, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onSave(title, content) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalAccentColor.current.copy(
                                alpha = if (title.isBlank()) 0.3f else 1f
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (initialNote != null) "Update" else "Save", color = Color.White)
                    }
                }
            }
        }
    }
}

// --- UPDATED PRODUCTIVITY TOOLS ---
@Composable
fun ProductivityTools() {
    val context = LocalContext.current
    var service by remember { mutableStateOf<ProductivityService?>(null) }

    DisposableEffect(Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
                val binder = iBinder as ProductivityService.ProductivityBinder
                service = binder.getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
        val intent = Intent(context, ProductivityService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(connection)
        }
    }

    var selectedTool by remember { mutableIntStateOf(0) } // 0: Pomodoro, 1: Timer, 2: Stopwatch
    
    // UI state synchronized with service
    var timerTime by remember { mutableLongStateOf(10 * 60 * 1000L) }
    var maxTimerTime by remember { mutableLongStateOf(10 * 60 * 1000L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var pomoTime by remember { mutableLongStateOf(25 * 60 * 1000L) }
    var isPomoRunning by remember { mutableStateOf(false) }
    var pomoStage by remember { mutableStateOf("Focus") }
    var stopWatchTime by remember { mutableLongStateOf(0L) }
    var isStopWatchRunning by remember { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }

    // Manual timer input
    var manualMinutes by remember { mutableStateOf("10") }

    LaunchedEffect(service, selectedTool) {
        service?.selectedTool = selectedTool
        while (true) {
            delay(100)
            service?.let { s ->
                timerTime = s.timerTime
                maxTimerTime = s.maxTimerTime
                isTimerRunning = s.isTimerRunning
                pomoTime = s.pomoTime
                isPomoRunning = s.isPomoRunning
                pomoStage = s.pomoStage
                stopWatchTime = s.stopwatchTime
                isStopWatchRunning = s.isStopwatchRunning
                if (laps.size != s.laps.size) {
                    laps.clear()
                    laps.addAll(s.laps)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(72.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = LocalSurfaceColor.current
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                listOf("Pomodoro", "Timer", "Stopwatch").forEachIndexed { index, title ->
                    val isSelected = selectedTool == index
                    val isActive = when(index) {
                        0 -> isPomoRunning
                        1 -> isTimerRunning
                        2 -> isStopWatchRunning
                        else -> false
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isSelected) LocalAccentColor.current else Color.Transparent)
                            .clickable { selectedTool = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                title,
                                color = if (isSelected) Color.White else LocalTextSecondary.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (isActive && !isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).background(SuccessEmerald, CircleShape))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        when (selectedTool) {
            0 -> PomodoroUI(
                pomoTime,
                isPomoRunning,
                pomoStage,
                onToggle = { 
                    val action = if (isPomoRunning) "PAUSE_POMO" else "START_POMO"
                    context.startService(Intent(context, ProductivityService::class.java).apply { this.action = action })
                },
                onReset = {
                    context.startService(Intent(context, ProductivityService::class.java).apply { action = "RESET_POMO" })
                },
                onStageChange = { stage, time ->
                    service?.pomoStage = stage
                    service?.pomoTime = time
                    context.startService(Intent(context, ProductivityService::class.java).apply { action = "PAUSE_POMO" })
                }
            )
            1 -> TimerUI(
                timerTime,
                maxTimerTime,
                isTimerRunning,
                onToggle = { 
                    val action = if (isTimerRunning) "PAUSE_TIMER" else "START_TIMER"
                    context.startService(Intent(context, ProductivityService::class.java).apply { this.action = action })
                },
                onReset = {
                    context.startService(Intent(context, ProductivityService::class.java).apply { action = "RESET_TIMER" })
                },
                onSetTime = { newTime ->
                    service?.timerTime = newTime
                    service?.maxTimerTime = newTime
                    context.startService(Intent(context, ProductivityService::class.java).apply { action = "PAUSE_TIMER" })
                },
                manualMinutes = manualMinutes,
                onManualMinutesChange = { manualMinutes = it }
            )
            2 -> StopwatchUI(
                stopWatchTime,
                isStopWatchRunning,
                laps,
                onToggle = { 
                    val action = if (isStopWatchRunning) "PAUSE_STOPWATCH" else "START_STOPWATCH"
                    context.startService(Intent(context, ProductivityService::class.java).apply { this.action = action })
                },
                onReset = {
                    context.startService(Intent(context, ProductivityService::class.java).apply { action = "RESET_STOPWATCH" })
                },
                onLap = { 
                    service?.laps?.add(0, stopWatchTime)
                }
            )
        }
    }
}

@Composable
fun TimerUI(
    time: Long,
    maxTime: Long,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSetTime: (Long) -> Unit,
    manualMinutes: String,
    onManualMinutesChange: (String) -> Unit
) {
    val m = (time / 1000) / 60
    val s = (time / 1000) % 60
    val totalTime = maxTime.toFloat()
    val sweepAngle = if (totalTime > 0) (time.toFloat() / totalTime) * 360f else 0f
    val animatedSweep by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "timerArc"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val accent = LocalAccentColor.current
    val surface = LocalSurfaceColor.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(pulse)) {
            Canvas(modifier = Modifier.size(260.dp)) {
                drawCircle(color = surface, style = Stroke(12.dp.toPx()))
                drawArc(
                    brush = Brush.sweepGradient(listOf(accent.copy(0.5f), accent)),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(12.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", m, s),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light
                ),
                color = LocalTextPrimary.current
            )
        }
        Spacer(modifier = Modifier.height(30.dp))

        // Manual timer input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualMinutes,
                onValueChange = { onManualMinutesChange(it) },
                modifier = Modifier.width(80.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = LocalTextPrimary.current,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = surface,
                    focusedTextColor = LocalTextPrimary.current,
                    unfocusedTextColor = LocalTextPrimary.current
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("min", color = LocalTextSecondary.current, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    val mins = manualMinutes.toIntOrNull() ?: 1
                    if (mins > 0) onSetTime(mins * 60 * 1000L)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Set", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(1, 5, 10, 15).forEach { mins ->
                FilterChip(
                    selected = false,
                    onClick = { onSetTime(mins * 60 * 1000L) },
                    label = { Text("${mins}m") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = surface,
                        labelColor = LocalTextSecondary.current
                    ),
                    shape = CircleShape
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        TimerControls(isRunning, onToggle, onReset)
    }
}

@Composable
fun PomodoroUI(
    time: Long,
    isRunning: Boolean,
    stage: String,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onStageChange: (String, Long) -> Unit
) {
    val m = (time / 1000) / 60
    val s = (time / 1000) % 60
    val totalTime = (when (stage) {
        "Focus" -> 25
        "Short" -> 5
        else -> 15
    } * 60 * 1000f)
    val sweepAngle = if (totalTime > 0) (time.toFloat() / totalTime) * 360f else 0f
    val animatedSweep by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "pomoArc"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pomoPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isRunning) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    val surface = LocalSurfaceColor.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PomodoroChip("Focus", stage == "Focus") {
                onStageChange("Focus", 25 * 60 * 1000L)
            }
            PomodoroChip("Short", stage == "Short") {
                onStageChange("Short", 5 * 60 * 1000L)
            }
            PomodoroChip("Long", stage == "Long") {
                onStageChange("Long", 15 * 60 * 1000L)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(260.dp)) {
                drawCircle(color = surface, style = Stroke(8.dp.toPx()))
                // Neon Glow Effect
                drawCircle(
                    color = SuccessEmerald.copy(alpha = glowAlpha * 0.2f),
                    radius = (130.dp + 10.dp).toPx()
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(SuccessEmerald.copy(0.5f), SuccessEmerald)),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", m, s),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LocalTextPrimary.current
                )
                Text(
                    text = stage.uppercase(),
                    color = SuccessEmerald,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
        TimerControls(isRunning, onToggle, onReset)
    }
}

@Composable
fun PomodoroChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = if (isSelected) SuccessEmerald else LocalSurfaceColor.current
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (isSelected) Color.White else LocalTextSecondary.current,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StopwatchUI(
    time: Long,
    isRunning: Boolean,
    laps: List<Long>,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    val h = (time / 1000) / 3600
    val m = ((time / 1000) % 3600) / 60
    val s = (time / 1000) % 60
    val ms = (time % 1000) / 10

    val surface = LocalSurfaceColor.current
    val accent = LocalAccentColor.current
    val textPrimary = LocalTextPrimary.current
    val textSecondary = LocalTextSecondary.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", h, m, s, ms),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(56.dp)
                    .background(surface, CircleShape)
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = textPrimary)
            }
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = if (isRunning) SuccessEmerald else accent,
                onClick = onToggle
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            IconButton(
                onClick = onLap,
                enabled = isRunning,
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isRunning) surface else surface.copy(0.3f), CircleShape)
            ) {
                Icon(
                    Icons.Rounded.Timer,
                    null,
                    tint = if (isRunning) textPrimary else textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(laps) { index, lapTime ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap ${laps.size - index}", color = textSecondary)
                    Text(
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d.%02d",
                            (lapTime / 60000) % 60,
                            (lapTime / 1000) % 60,
                            (lapTime % 1000) / 10
                        ),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(color = Color.White.copy(0.05f))
            }
        }
    }
}

@Composable
fun TimerControls(isRunning: Boolean, onToggle: () -> Unit, onReset: () -> Unit) {
    val surface = LocalSurfaceColor.current
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(56.dp)
                .background(surface, CircleShape)
        ) {
            Icon(Icons.Rounded.Refresh, null, tint = LocalTextPrimary.current)
        }
        Spacer(modifier = Modifier.width(40.dp))
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = if (isRunning) SuccessEmerald else accent,
            onClick = onToggle
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
// --- END PRODUCTIVITY TOOLS ---

@Composable
fun UniversalProfile(
    name: String,
    bio: String,
    photoUri: String?,
    currentTheme: Int,
    currentAccent: Color,
    firebaseSyncManager: FirebaseSyncManager,
    onOpenAuth: () -> Unit,
    onSyncCloud: () -> Unit,
    onLogoutToGuest: () -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onUpdateAppearance: (Int, Color) -> Unit
) {
    var currentSubScreen by remember { mutableStateOf<ProfileSubScreen?>(null) }
    var isEditingProfile by remember { mutableStateOf(false) }

    if (currentSubScreen != null || isEditingProfile) {
        BackHandler {
            if (isEditingProfile) {
                isEditingProfile = false
            } else {
                currentSubScreen = null
            }
        }
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "ProfileNavigation"
    ) { subScreen ->
        if (subScreen == null) {
            ProfileMain(
                name = name,
                bio = bio,
                photoUri = photoUri,
                firebaseSyncManager = firebaseSyncManager,
                onOpenAuth = onOpenAuth,
                onSyncCloud = onSyncCloud,
                onLogoutToGuest = onLogoutToGuest,
                onEditProfile = { isEditingProfile = true },
                onNavigate = { currentSubScreen = it }
            )
        } else {
            when (subScreen) {
                ProfileSubScreen.PERSONAL -> PersonalDetailsScreen(
                    name = name,
                    bio = bio,
                    onBack = { currentSubScreen = null },
                    onSave = { n, b -> onUpdateProfile(n, b, photoUri) }
                )
                ProfileSubScreen.NOTIFICATIONS -> NotificationsSettingsScreen(onBack = { currentSubScreen = null })
                ProfileSubScreen.APPEARANCE -> AppearanceSettingsScreen(
                    currentTheme = currentTheme,
                    currentAccent = currentAccent,
                    onBack = { currentSubScreen = null },
                    onUpdate = onUpdateAppearance
                )
                ProfileSubScreen.TERMS -> TermsPrivacyScreen(onBack = { currentSubScreen = null })
                ProfileSubScreen.MANAGE_ACCOUNT -> ManageAccountScreen(
                    firebaseSyncManager = firebaseSyncManager,
                    onBack = { currentSubScreen = null },
                    onLogoutToGuest = {
                        currentSubScreen = null
                        onLogoutToGuest()
                    }
                )
            }
        }
    }

    if (isEditingProfile) {
        EditProfileOverlay(
            initialName = name,
            initialBio = bio,
            initialPhotoUri = photoUri,
            onDismiss = { isEditingProfile = false },
            onSave = { n, b, p ->
                onUpdateProfile(n, b, p)
                isEditingProfile = false
            }
        )
    }
}

enum class ProfileSubScreen {
    PERSONAL, NOTIFICATIONS, APPEARANCE, TERMS, MANAGE_ACCOUNT
}

@Composable
fun ProfileMain(
    name: String,
    bio: String,
    photoUri: String?,
    firebaseSyncManager: FirebaseSyncManager,
    onOpenAuth: () -> Unit,
    onSyncCloud: () -> Unit,
    onLogoutToGuest: () -> Unit,
    onEditProfile: () -> Unit,
    onNavigate: (ProfileSubScreen) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(72.dp)) }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    val accent = LocalAccentColor.current
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.15f),
                        border = BorderStroke(2.dp, accent)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_app_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = bio,
                    color = SuccessEmerald,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .width(180.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Edit Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            SectionHeader("FIREBASE & CLOUD SYNC")
        }
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = LocalSurfaceColor.current,
                border = BorderStroke(1.dp, LocalAccentColor.current.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Cloud,
                                contentDescription = "Cloud",
                                tint = LocalAccentColor.current,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (firebaseSyncManager.isLoggedIn) "Cloud Sync Active" else "Guest Mode (Local Only)",
                                    color = LocalTextPrimary.current,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = firebaseSyncManager.currentUser?.email ?: "Sign in to back up data to the cloud",
                                    color = LocalTextSecondary.current,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (firebaseSyncManager.isLoggedIn) {
                            Button(
                                onClick = onSyncCloud,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cloud Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    firebaseSyncManager.signOut()
                                    onLogoutToGuest()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRose)
                            ) {
                                Text("Log Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onOpenAuth,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In / Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            SectionHeader("PREFERENCES")
        }
        item { ProfileOption("Personal Details", Icons.Rounded.Badge) { onNavigate(ProfileSubScreen.PERSONAL) } }
        item { ProfileOption("Notifications", Icons.Rounded.NotificationsActive) { onNavigate(ProfileSubScreen.NOTIFICATIONS) } }
        item { ProfileOption("Appearance", Icons.Rounded.Palette) { onNavigate(ProfileSubScreen.APPEARANCE) } }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            SectionHeader("SUPPORT")
        }
        item { ProfileOption("Terms & Privacy", Icons.Rounded.Policy) { onNavigate(ProfileSubScreen.TERMS) } }
        item { ProfileOption("Manage Account", Icons.Rounded.ManageAccounts) { onNavigate(ProfileSubScreen.MANAGE_ACCOUNT) } }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Text(
                "Version 1.0.0",
                color = LocalTextSecondary.current.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun ProfileOption(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = LocalSurfaceColor.current.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = LocalAccentColor.current.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = LocalAccentColor.current,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                label,
                color = LocalTextPrimary.current,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = LocalTextSecondary.current.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalBgColor.current)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = LocalTextPrimary.current)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = LocalTextPrimary.current,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        content()
    }
}

@Composable
fun PersonalDetailsScreen(name: String, bio: String, onBack: () -> Unit, onSave: (String, String) -> Unit) {
    var tempName by remember { mutableStateOf(name) }
    var tempBio by remember { mutableStateOf(bio) }

    SubScreenScaffold("Personal Details", onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ProfileInputField("Full Name", tempName) { tempName = it }
            ProfileInputField("Bio / Status", tempBio) { tempBio = it }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onSave(tempName, tempBio); onBack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalAccentColor.current,
                unfocusedBorderColor = LocalSurfaceColor.current,
                focusedTextColor = LocalTextPrimary.current,
                unfocusedTextColor = LocalTextPrimary.current
            )
        )
    }
}

@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataManager = remember { DataManager(context) }
    var remindersEnabled by remember { mutableStateOf(dataManager.isNotificationEnabled("reminders_enabled")) }
    var focusEnabled by remember { mutableStateOf(dataManager.isNotificationEnabled("focus_enabled")) }
    var soundEnabled by remember { mutableStateOf(dataManager.isNotificationEnabled("sound_enabled", false)) }

    SubScreenScaffold("Notifications", onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingSwitchRow("Reminders", "Get notified for your tasks", remindersEnabled) { 
                remindersEnabled = it
                dataManager.saveNotificationEnabled("reminders_enabled", it)
            }
            SettingSwitchRow("Focus Mode", "Alerts when session ends", focusEnabled) { 
                focusEnabled = it
                dataManager.saveNotificationEnabled("focus_enabled", it)
            }
            SettingSwitchRow("Sound & Vibration", "Alert with sound", soundEnabled) { 
                soundEnabled = it
                dataManager.saveNotificationEnabled("sound_enabled", it)
            }
        }
    }
}

@Composable
fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = LocalSurfaceColor.current.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LocalTextPrimary.current, fontWeight = FontWeight.Bold)
                Text(subtitle, color = LocalTextSecondary.current, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = SuccessEmerald)
            )
        }
    }
}

@Composable
fun AppearanceSettingsScreen(
    currentTheme: Int,
    currentAccent: Color,
    onBack: () -> Unit,
    onUpdate: (Int, Color) -> Unit
) {
    var selectedTheme by remember { mutableIntStateOf(currentTheme) }
    var selectedAccent by remember { mutableStateOf(currentAccent) }

    val accentColors = listOf(
        Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFF43F5E),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4)
    )

    SubScreenScaffold("Appearance", onBack) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("THEME MODE", color = LocalTextSecondary.current, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeCard("Dark", Icons.Rounded.DarkMode, selectedTheme == 0, Modifier.weight(1f)) { 
                            selectedTheme = 0
                            onUpdate(0, selectedAccent)
                        }
                        ThemeCard("Light", Icons.Rounded.LightMode, selectedTheme == 1, Modifier.weight(1f)) { 
                            selectedTheme = 1
                            onUpdate(1, selectedAccent)
                        }
                        ThemeCard("System", Icons.Rounded.SettingsBrightness, selectedTheme == 2, Modifier.weight(1f)) { 
                            selectedTheme = 2
                            onUpdate(2, selectedAccent)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("ACCENT COLOR", color = LocalTextSecondary.current, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        items(accentColors) { color ->
                            val isSelected = selectedAccent == color
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        selectedAccent = color
                                        onUpdate(selectedTheme, color)
                                    }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("PREVIEW", color = LocalTextSecondary.current, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = LocalSurfaceColor.current.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color.White.copy(0.05f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier
                                    .size(40.dp)
                                    .background(selectedAccent.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Notifications, null, tint = selectedAccent, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Sample Task", color = LocalTextPrimary.current, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Today • 10:00 AM", color = LocalTextSecondary.current, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = selectedAccent, checkedTrackColor = selectedAccent.copy(0.3f)))
                            }
                            Button(
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = selectedAccent)
                            ) {
                                Text("Primary Action", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ThemeCard(label: String, icon: ImageVector, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) LocalAccentColor.current else LocalSurfaceColor.current,
        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(icon, null, tint = if (isSelected) Color.White else LocalTextSecondary.current, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = if (isSelected) Color.White else LocalTextSecondary.current, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun TermsPrivacyScreen(onBack: () -> Unit) {
    SubScreenScaffold("Terms & Privacy", onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(
                    "This application respects your privacy. All your data (Reminders, Notes, Profile) is stored locally on your device and is not shared with any third-party servers.",
                    color = TextSecondary,
                    lineHeight = 24.sp
                )
            }
            item {
                Text("Terms of Service", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "By using Reminder Pro, you agree to local data management and responsible use of notification features.",
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ManageAccountScreen(
    firebaseSyncManager: FirebaseSyncManager,
    onBack: () -> Unit,
    onLogoutToGuest: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val user = firebaseSyncManager.currentUser

    SubScreenScaffold("Manage Account", onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SectionHeader("ACCOUNT INFORMATION")
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = LocalSurfaceColor.current
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(LocalAccentColor.current.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = LocalAccentColor.current,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = user?.displayName?.ifBlank { null } ?: user?.email?.substringBefore("@") ?: "Guest Mode",
                                    color = LocalTextPrimary.current,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = user?.email ?: "Not signed in (Local Storage Only)",
                                    color = LocalTextSecondary.current,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (user != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Account ID: ${user.uid}",
                                color = LocalTextSecondary.current.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (user != null) {
                item {
                    SectionHeader("ACCOUNT ACTIONS")
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                firebaseSyncManager.signOut()
                                Toast.makeText(context, "Logged out. Switched to Guest Mode.", Toast.LENGTH_SHORT).show()
                                onLogoutToGuest()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = LocalSurfaceColor.current
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Deactivate / Sign Out",
                                    color = LocalTextPrimary.current,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Signs out and switches back to Guest Mode.",
                                    color = LocalTextSecondary.current,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader("DANGER ZONE")
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteConfirmDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = DangerRose.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                tint = DangerRose,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Delete Account Permanently",
                                    color = DangerRose,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Permanently deletes your account and cloud data.",
                                    color = LocalTextSecondary.current,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete your account? All your cloud reminders and notes will be permanently erased. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        firebaseSyncManager.deleteAccount(
                            onSuccess = {
                                isDeleting = false
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "Account permanently deleted.", Toast.LENGTH_SHORT).show()
                                onLogoutToGuest()
                            },
                            onError = { err ->
                                isDeleting = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRose),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            },
            containerColor = LocalSurfaceColor.current
        )
    }
}

@Composable
fun EditProfileOverlay(
    initialName: String,
    initialBio: String,
    initialPhotoUri: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    var tempName by remember { mutableStateOf(initialName) }
    var tempBio by remember { mutableStateOf(initialBio) }
    var tempPhotoUri by remember { mutableStateOf(initialPhotoUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) tempPhotoUri = uri.toString()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = LocalBgColor.current.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Update Identity", style = MaterialTheme.typography.headlineMedium, color = LocalTextPrimary.current, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(contentAlignment = Alignment.BottomEnd) {
                val accent = LocalAccentColor.current
                Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = accent.copy(alpha = 0.1f)) {
                    if (tempPhotoUri != null) {
                        AsyncImage(
                            model = tempPhotoUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Rounded.Person, null, modifier = Modifier.size(72.dp), tint = accent)
                    }
                }
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = accent,
                    border = BorderStroke(2.dp, LocalBgColor.current),
                    onClick = { photoPickerLauncher.launch("image/*") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            ProfileInputField("Full Name", tempName) { tempName = it }
            ProfileInputField("Bio / Status", tempBio) { tempBio = it }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LocalSurfaceColor.current)
                ) {
                    Text("Cancel", color = LocalTextPrimary.current)
                }
                Button(
                    onClick = { onSave(tempName, tempBio, tempPhotoUri) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current)
                ) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    ReminderTheme {
        MainContainer()
    }
}