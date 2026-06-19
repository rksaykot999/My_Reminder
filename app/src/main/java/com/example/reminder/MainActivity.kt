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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.reminder.ui.theme.ReminderTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
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
val BgColor = Color(0xFF0F172A)
val SurfaceColor = Color(0xFF1E293B)
val PrimaryIndigo = Color(0xFF6366F1)
val SuccessEmerald = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRose = Color(0xFFF43F5E)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

// --- Local Storage Manager ---
class DataManager(context: Context) {
    private val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveReminders(reminders: List<Reminder>) {
        val json = gson.toJson(reminders)
        prefs.edit().putString("reminders", json).apply()
    }

    fun loadReminders(): List<Reminder> {
        val json = prefs.getString("reminders", null) ?: return emptyList()
        val type = object : TypeToken<List<Reminder>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveNotes(notes: List<Note>) {
        val json = gson.toJson(notes)
        prefs.edit().putString("notes", json).apply()
    }

    fun loadNotes(): List<Note> {
        val json = prefs.getString("notes", null) ?: return emptyList()
        val type = object : TypeToken<List<Note>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveProfile(name: String, bio: String) {
        prefs.edit().putString("user_name", name).putString("user_bio", bio).apply()
    }

    fun loadProfile(): Pair<String, String> {
        val name = prefs.getString("user_name", "User Name") ?: "User Name"
        val bio = prefs.getString("user_bio", "Set your status") ?: "Set your status"
        return name to bio
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            ReminderTheme {
                MainContainer()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            
            val completionChannel = NotificationChannel("TASK_COMPLETED", "Completions", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel for task completion alerts"
            }
            
            val reminderChannel = NotificationChannel("TASK_REMINDER", "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel for scheduled task reminders"
            }
            
            notificationManager.createNotificationChannel(completionChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val desc = intent.getStringExtra("desc") ?: "You have a task to do!"
        
        val builder = NotificationCompat.Builder(context, "TASK_REMINDER")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }
}

@SuppressLint("MissingPermission", "PostNotifications")
fun showCompletionNotification(context: Context, taskTitle: String) {
    val builder = NotificationCompat.Builder(context, "TASK_COMPLETED")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Task Finished!")
        .setContentText("You completed: $taskTitle")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}

fun scheduleReminder(context: Context, reminder: Reminder) {
    if (reminder.scheduledTimestamp <= System.currentTimeMillis()) return
    
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", reminder.title)
        putExtra("desc", reminder.description)
    }
    
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.scheduledTimestamp, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.scheduledTimestamp, pendingIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.scheduledTimestamp, pendingIntent)
    }
}

@Composable
fun MainContainer() {
    val context = LocalContext.current
    val dataManager = remember { DataManager(context) }
    var selectedItem by rememberSaveable { mutableStateOf(NavItem.HOME) }
    
    // --- APP STATE ---
    val reminders = remember { 
        mutableStateListOf<Reminder>().apply { addAll(dataManager.loadReminders()) } 
    }
    val notes = remember { 
        mutableStateListOf<Note>().apply { addAll(dataManager.loadNotes()) } 
    }

    val (initialName, initialBio) = remember { dataManager.loadProfile() }
    var userName by rememberSaveable { mutableStateOf(initialName) }
    var userBio by rememberSaveable { mutableStateOf(initialBio) }

    var isAddingReminder by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var isAddingNote by remember { mutableStateOf(false) }

    // Save data when lists change
    LaunchedEffect(reminders.size, reminders.count { it.isCompleted }) {
        dataManager.saveReminders(reminders)
    }
    LaunchedEffect(notes.size) {
        dataManager.saveNotes(notes)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedItem,
                transitionSpec = { fadeIn(tween(500)) + scaleIn(initialScale = 0.92f) togetherWith fadeOut(tween(500)) },
                label = "NavigationTransition"
            ) { destination ->
                when (destination) {
                    NavItem.HOME -> GeneralHome(
                        reminders = reminders,
                        onAddReminder = { isAddingReminder = true },
                        onToggleReminder = { id ->
                            val index = reminders.indexOfFirst { it.id == id }
                            if (index != -1) {
                                val wasCompleted = reminders[index].isCompleted
                                reminders[index] = reminders[index].copy(isCompleted = !wasCompleted)
                                if (!wasCompleted) showCompletionNotification(context, reminders[index].title)
                                dataManager.saveReminders(reminders) // Explicit save for completion state
                            }
                        },
                        onEditReminder = { reminder -> reminderToEdit = reminder },
                        onDeleteReminder = { id -> 
                            reminders.removeIf { it.id == id } 
                            dataManager.saveReminders(reminders)
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
                                scheduledTimestamp = System.currentTimeMillis() + 3600000 // Placeholder
                            )
                            reminders.add(newRem)
                            dataManager.saveReminders(reminders)
                        }
                    )
                    NavItem.NOTES -> GeneralNotes(
                        notes = notes,
                        onAddNote = { isAddingNote = true }
                    )
                    NavItem.FOCUS -> ProductivityTools()
                    NavItem.ME -> UniversalProfile(
                        name = userName,
                        bio = userBio,
                        onUpdateProfile = { n, b ->
                            userName = n
                            userBio = b
                            dataManager.saveProfile(n, b)
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        ) {
            UniversalDock(selectedItem = selectedItem, onItemSelected = { selectedItem = it })
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
                }
            )
        }

        if (isAddingNote) {
            NewNoteDialog(
                onDismiss = { isAddingNote = false },
                onSave = { title, content ->
                    notes.add(Note(notes.size + 1, title, content, "NOW"))
                    isAddingNote = false
                    dataManager.saveNotes(notes)
                }
            )
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
        modifier = Modifier.height(64.dp).padding(horizontal = 24.dp),
        shape = RoundedCornerShape(32.dp),
        color = SurfaceColor.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem.entries.forEach { item ->
                val isSelected = selectedItem == item
                val animatedWeight by animateFloatAsState(if (isSelected) 1.2f else 1f, label = "dockWeight")
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                        .clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = item.icon, contentDescription = null, tint = if (isSelected) Color.White else TextSecondary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun GeneralHome(
    reminders: List<Reminder>,
    onAddReminder: () -> Unit,
    onToggleReminder: (Int) -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onDeleteReminder: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(72.dp)) }
            item {
                Column {
                    Text("WELCOME BACK", color = PrimaryIndigo, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Make it count.", color = TextPrimary, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                }
            }
            item {
                val completed = reminders.count { it.isCompleted }
                val total = reminders.size
                val progress = if (total > 0) completed.toFloat() / total else 0f
                Surface(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(28.dp), color = PrimaryIndigo) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Column {
                            Text("DAILY GOAL", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("$completed of $total Tasks Done", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = Color.White, trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeStatCard("Reminders", "${reminders.size}", Icons.Rounded.Notifications, WarningAmber, Modifier.weight(1f))
                    HomeStatCard("Focus", "2.5h", Icons.Rounded.Bolt, SuccessEmerald, Modifier.weight(1f))
                }
            }
            item { Text("Upcoming Today", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            items(reminders, key = { it.id }) { reminder ->
                TaskRow(reminder = reminder, onToggle = { onToggleReminder(reminder.id) }, onEdit = { onEditReminder(reminder) }, onDelete = { onDeleteReminder(reminder.id) })
            }
            item { Spacer(modifier = Modifier.height(110.dp)) }
        }
        FloatingActionButton(onClick = onAddReminder, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 110.dp, end = 24.dp), containerColor = PrimaryIndigo, contentColor = Color.White, shape = RoundedCornerShape(20.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = "New Reminder")
        }
    }
}

@Composable
fun HomeStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier = modifier.height(110.dp), shape = RoundedCornerShape(24.dp), color = SurfaceColor) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Column {
                Text(label, color = TextSecondary, fontSize = 11.sp)
                Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun TaskRow(reminder: Reminder, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { onToggle() }, shape = RoundedCornerShape(20.dp), color = SurfaceColor, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).border(2.dp, if(reminder.isCompleted) SuccessEmerald else PrimaryIndigo, CircleShape).padding(4.dp)) {
                if (reminder.isCompleted) Icon(Icons.Rounded.Check, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.title, color = if(reminder.isCompleted) TextSecondary else TextPrimary, fontWeight = FontWeight.Medium, style = if(reminder.isCompleted) MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyMedium)
                Text("${reminder.date} • ${reminder.time} • ${reminder.category.label}", color = TextSecondary, fontSize = 12.sp)
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More Options", tint = TextSecondary) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceColor)) {
                    DropdownMenuItem(text = { Text("Edit", color = TextPrimary) }, leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = PrimaryIndigo) }, onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete", color = DangerRose) }, leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = DangerRose) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReminderScreen(initialReminder: Reminder? = null, onDismiss: () -> Unit, onSave: (Reminder) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var notes by remember { mutableStateOf(initialReminder?.description ?: "") }
    var priority by remember { mutableStateOf(initialReminder?.priority ?: "Low") }
    var category by remember { mutableStateOf(initialReminder?.category ?: Category.GENERAL) }
    var selectedDate by remember { mutableStateOf(initialReminder?.date ?: "Oct 15") }
    var selectedTime by remember { mutableStateOf(initialReminder?.time ?: "09:00 AM") }
    
    val calendar = remember { 
        Calendar.getInstance().apply {
            if (initialReminder != null && initialReminder.scheduledTimestamp > 0) {
                timeInMillis = initialReminder.scheduledTimestamp
            }
        }
    }

    val datePickerDialog = DatePickerDialog(context, { _, year, month, day ->
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        selectedDate = String.format(Locale.getDefault(), "%s %02d", listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month], day)
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val timePickerDialog = TimePickerDialog(context, { _, hour, min ->
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, min)
        calendar.set(Calendar.SECOND, 0)
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", h, min, amPm)
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

    Surface(
        modifier = Modifier.fillMaxSize().imePadding(), // SMART FIX: Respects keyboard
        color = BgColor
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(64.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Reminder", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TASK INFORMATION", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("What needs to be done?", color = TextSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = SurfaceColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, placeholder = { Text("Add more details or notes...", color = TextSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = SurfaceColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SET PRIORITY", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("LOW" to SuccessEmerald, "MEDIUM" to WarningAmber, "HIGH" to DangerRose).forEach { (label, color) ->
                                val isSelected = priority.equals(label, ignoreCase = true)
                                Surface(modifier = Modifier.weight(1f).height(48.dp).clickable { priority = label }, shape = RoundedCornerShape(12.dp), color = if (isSelected) color else SurfaceColor) {
                                    Box(contentAlignment = Alignment.Center) { Text(label, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SCHEDULE", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScheduleCard("Date", selectedDate, Icons.Rounded.CalendarMonth, Modifier.weight(1f)) { datePickerDialog.show() }
                            ScheduleCard("Time", selectedTime, Icons.Rounded.Schedule, Modifier.weight(1f)) { timePickerDialog.show() }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("CATEGORY", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(Category.entries) { cat ->
                                val isSelected = category == cat
                                Surface(modifier = Modifier.width(100.dp).height(80.dp).clickable { category = cat }, shape = RoundedCornerShape(16.dp), color = if (isSelected) cat.color else SurfaceColor, border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(cat.icon, contentDescription = null, tint = if (isSelected) Color.White else cat.color, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(cat.label, color = if (isSelected) Color.White else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = { if (title.isNotBlank()) onSave(Reminder(0, title, notes, selectedTime, selectedDate, priority = priority, category = category, scheduledTimestamp = calendar.timeInMillis)) }, modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)) {
                Text(if (initialReminder != null) "Update Task" else "Create Task", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ScheduleCard(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor.copy(alpha = 0.7f),
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
                    .background(PrimaryIndigo.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = PrimaryIndigo, 
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label, 
                    color = TextSecondary, 
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
    var selectedDay by remember { mutableIntStateOf(15) } // Default to Oct 15
    var showDayDetail by remember { mutableStateOf(false) }
    var quickNote by remember { mutableStateOf("") }
    
    val filteredReminders = reminders.filter { 
        it.date.contains(selectedDay.toString()) && it.date.contains("Oct") 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(72.dp)) }
            
            // --- CALENDAR GRID SECTION ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("October 2024", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Row {
                            IconButton(onClick = {}) { Icon(Icons.Rounded.ChevronLeft, null, tint = TextPrimary) }
                            IconButton(onClick = {}) { Icon(Icons.Rounded.ChevronRight, null, tint = TextPrimary) }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        days.forEach { day ->
                            Text(
                                day, 
                                modifier = Modifier.weight(1f), 
                                textAlign = TextAlign.Center, 
                                color = if(day == "S") DangerRose else TextSecondary, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    for (row in 0 until 5) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0 until 7) {
                                val i = row * 7 + col
                                if (i < 31) {
                                    val dayNum = i + 1
                                    val isSelected = dayNum == selectedDay
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                                            .clickable { 
                                                selectedDay = dayNum
                                                showDayDetail = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$dayNum", 
                                            color = if (isSelected) Color.White else TextPrimary, 
                                            fontSize = 14.sp,
                                            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
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

            // --- BOTTOM LIST (READ-ONLY) ---
            item {
                Text(
                    text = "Reminders for $selectedDay Oct",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (filteredReminders.isEmpty()) {
                item {
                    Text(
                        "No events scheduled", 
                        color = TextSecondary, 
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), 
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

        // --- SAMSUNG STYLE DAY VIEW POPUP ---
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
                    .imePadding(), // SMART FIX: Pushes UI up when keyboard appears
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .clickable(enabled = false) {}, // Prevent click propagation to background
                    shape = RoundedCornerShape(32.dp),
                    color = SurfaceColor,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$selectedDay", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tuesday", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            }
                            Row {
                                Icon(Icons.AutoMirrored.Rounded.EventNote, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Rounded.EmojiEmotions, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                            drawLine(color = TextSecondary.copy(alpha = 0.2f), start = Offset(0f, 0f), end = Offset(size.width, 0f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Dynamic Event Items
                        Box(modifier = Modifier.heightIn(max = 300.dp)) {
                            if (filteredReminders.isEmpty()) {
                                Text("No events scheduled", color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center)
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

                        // Quick Add Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(BgColor.copy(alpha = 0.5f), CircleShape)
                                .padding(start = 20.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = quickNote,
                                onValueChange = { quickNote = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                decorationBox = { innerTextField ->
                                    if (quickNote.isEmpty()) Text("Add on $selectedDay Oct", color = TextSecondary, fontSize = 14.sp)
                                    innerTextField()
                                }
                            )
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = SurfaceColor,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                onClick = {
                                    if (quickNote.isNotBlank()) {
                                        onQuickAdd("Oct $selectedDay", quickNote)
                                        quickNote = ""
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Add, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
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
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            time, 
            modifier = Modifier.width(45.dp), 
            color = TextPrimary, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.width(3.dp).height(40.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(duration, color = TextSecondary, fontSize = 12.sp)
        }
        Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
    }
}

@Composable
fun GeneralNotes(notes: List<Note>, onAddNote: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Spacer(modifier = Modifier.height(72.dp)) }
            item { Text("My Notes", color = TextPrimary, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    notes.chunked(2).forEach { rowNotes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowNotes.forEach { note -> NoteCard(note, Modifier.weight(1f)) }
                            if (rowNotes.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(110.dp)) }
        }
        FloatingActionButton(onClick = onAddNote, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 110.dp, end = 24.dp), containerColor = PrimaryIndigo, contentColor = Color.White, shape = RoundedCornerShape(20.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = "New Note")
        }
    }
}

@Composable
fun NoteCard(note: Note, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(160.dp), shape = RoundedCornerShape(24.dp), color = SurfaceColor, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(note.title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(note.content, color = TextSecondary, fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.weight(1f))
            Text(note.date, color = PrimaryIndigo, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NewNoteDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth().wrapContentHeight(), shape = RoundedCornerShape(28.dp), color = SurfaceColor) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("New Note", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Title", color = TextSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BgColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = content, onValueChange = { content = it }, placeholder = { Text("Note content", color = TextSecondary.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BgColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryIndigo, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (title.isNotBlank()) onSave(title, content) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo.copy(alpha = if(title.isBlank()) 0.3f else 1f)), shape = RoundedCornerShape(12.dp)) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun ProductivityTools() {
    var selectedTool by remember { mutableIntStateOf(0) }
    var timerTime by remember { mutableLongStateOf(10 * 60 * 1000L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var pomoTime by remember { mutableLongStateOf(25 * 60 * 1000L) }
    var isPomoRunning by remember { mutableStateOf(false) }
    var stopWatchTime by remember { mutableLongStateOf(0L) }
    var isStopWatchRunning by remember { mutableStateOf(false) }
    LaunchedEffect(isTimerRunning, isPomoRunning, isStopWatchRunning, selectedTool) {
        while (true) {
            delay(100.milliseconds)
            when (selectedTool) {
                0 -> if (isTimerRunning && timerTime > 0) timerTime -= 100
                1 -> if (isPomoRunning && pomoTime > 0) pomoTime -= 100
                2 -> if (isStopWatchRunning) stopWatchTime += 100
            }
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Surface(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), color = SurfaceColor) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("Timer", "Pomodoro", "Watch").forEachIndexed { index, title ->
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (selectedTool == index) PrimaryIndigo else Color.Transparent).clickable { selectedTool = index }, contentAlignment = Alignment.Center) {
                            Text(title, color = if (selectedTool == index) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item {
            val displayTime = when (selectedTool) {
                0 -> { val m = (timerTime / 1000) / 60; val s = (timerTime / 1000) % 60; String.format(Locale.getDefault(), "%02d:%02d", m, s) }
                1 -> { val m = (pomoTime / 1000) / 60; val s = (pomoTime / 1000) % 60; String.format(Locale.getDefault(), "%02d:%02d", m, s) }
                else -> { val h = (stopWatchTime / 1000) / 3600; val m = ((stopWatchTime / 1000) % 3600) / 60; val s = (stopWatchTime / 1000) % 60; String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s) }
            }
            val sweepAngle = when (selectedTool) {
                0 -> (timerTime.toFloat() / (10 * 60 * 1000)) * 360f
                1 -> (pomoTime.toFloat() / (25 * 60 * 1000)) * 360f
                else -> ((stopWatchTime % 60000).toFloat() / 60000) * 360f
            }
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(260.dp)) {
                    drawCircle(color = SurfaceColor, style = Stroke(12.dp.toPx()))
                    drawArc(color = PrimaryIndigo, startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, style = Stroke(12.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = displayTime, style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp, fontWeight = FontWeight.Light), color = TextPrimary)
                    Text(text = when(selectedTool) { 0 -> "TIMER"; 1 -> "POMODORO"; else -> "STOPWATCH" }, color = PrimaryIndigo, letterSpacing = 4.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    when (selectedTool) {
                        0 -> { timerTime = 10 * 60 * 1000L; isTimerRunning = false }
                        1 -> { pomoTime = 25 * 60 * 1000L; isPomoRunning = false }
                        2 -> { stopWatchTime = 0L; isStopWatchRunning = false }
                    }
                }, modifier = Modifier.size(56.dp).background(SurfaceColor, CircleShape)) { Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TextPrimary) }
                val isRunning = when(selectedTool) { 0 -> isTimerRunning; 1 -> isPomoRunning; else -> isStopWatchRunning }
                Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = if(isRunning) SuccessEmerald else PrimaryIndigo, onClick = {
                    when (selectedTool) { 0 -> isTimerRunning = !isTimerRunning; 1 -> isPomoRunning = !isPomoRunning; 2 -> isStopWatchRunning = !isStopWatchRunning }
                }) { Box(contentAlignment = Alignment.Center) { Icon(if(isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp)) } }
                IconButton(onClick = {}, modifier = Modifier.size(56.dp).background(SurfaceColor, CircleShape)) { Icon(Icons.Rounded.Settings, contentDescription = null, tint = TextPrimary) }
            }
        }
        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

@Composable
fun UniversalProfile(name: String, bio: String, onUpdateProfile: (String, String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(name) }
    var tempBio by remember { mutableStateOf(bio) }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Spacer(modifier = Modifier.height(72.dp)) }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(modifier = Modifier.size(110.dp), shape = CircleShape, color = PrimaryIndigo.copy(alpha = 0.15f), border = BorderStroke(2.dp, PrimaryIndigo)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = PrimaryIndigo) }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = name, color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = bio, color = SuccessEmerald, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(onClick = { tempName = name; tempBio = bio; isEditing = true }, shape = RoundedCornerShape(16.dp), color = PrimaryIndigo, modifier = Modifier.width(160.dp).height(48.dp), shadowElevation = 8.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { Text("PREFERENCES", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp)) }
            item { ProfileOption("Personal Details", Icons.Rounded.Badge) }
            item { ProfileOption("Notifications", Icons.Rounded.NotificationsActive) }
            item { ProfileOption("Appearance", Icons.Rounded.Palette) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { Text("SUPPORT", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp)) }
            item { ProfileOption("Terms & Privacy", Icons.Rounded.Policy) }
            item { ProfileOption("Help & Support", Icons.Rounded.SupportAgent) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { Text("Version 3.0.0", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp) }
            item { Spacer(modifier = Modifier.height(110.dp)) }
        }
        AnimatedVisibility(visible = isEditing, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            Surface(
                modifier = Modifier.fillMaxSize().imePadding(), // SMART FIX: Respects keyboard
                color = BgColor.copy(alpha = 0.98f)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item { Text("Update Identity", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold) }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = PrimaryIndigo.copy(alpha = 0.1f)) { Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(72.dp), tint = PrimaryIndigo) }
                            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = PrimaryIndigo, border = BorderStroke(2.dp, BgColor)) { IconButton(onClick = {}) { Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) } }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = SurfaceColor, focusedLabelColor = PrimaryIndigo, unfocusedLabelColor = TextSecondary, cursorColor = PrimaryIndigo, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)) }
                    item { OutlinedTextField(value = tempBio, onValueChange = { tempBio = it }, label = { Text("Bio / Status") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = SurfaceColor, focusedLabelColor = PrimaryIndigo, unfocusedLabelColor = TextSecondary, cursorColor = PrimaryIndigo, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)) }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SurfaceColor)) { Text("Cancel", color = TextPrimary) }
                            Button(onClick = { onUpdateProfile(tempName, tempBio); isEditing = false }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)) { Text("Save", color = Color.White) }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(110.dp)) }
                }
            }
        }
    }
}

@Composable
fun ProfileOption(label: String, icon: ImageVector) {
    Surface(modifier = Modifier.fillMaxWidth().height(68.dp), shape = RoundedCornerShape(20.dp), color = SurfaceColor.copy(alpha = 0.6f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)), onClick = {}) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = PrimaryIndigo.copy(alpha = 0.1f)) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp)) } }
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
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
