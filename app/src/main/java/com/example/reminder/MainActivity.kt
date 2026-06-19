package com.example.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.reminder.ui.theme.ReminderTheme
import java.util.Locale
import kotlinx.coroutines.delay

// --- Data Models ---
data class Reminder(
    val id: Int,
    val title: String,
    val description: String = "",
    val time: String,
    val date: String = "Today",
    val priority: String = "Low",
    val category: String = "General",
    val isCompleted: Boolean = false
)

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReminderTheme {
                MainContainer()
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
fun MainContainer() {
    var selectedItem by rememberSaveable { mutableStateOf(NavItem.HOME) }
    
    // --- APP STATE ---
    val reminders = remember { mutableStateListOf(
        Reminder(1, "Morning Run", "Beach side", "06:30 AM", isCompleted = true),
        Reminder(2, "Design System Sync", "Meeting room 2", "10:00 AM"),
        Reminder(3, "Lunch with Team", "Downtown", "01:00 PM"),
        Reminder(4, "Focus Session", "Library", "03:00 PM")
    ) }
    
    val notes = remember { mutableStateListOf(
        Note(1, "Project Ideas", "Build a high-end reminder app with fluid UI animations and student focus.", "OCT 12"),
        Note(2, "Shopping List", "New mechanical keyboard, ultra-wide monitor, and ergonomical desk chair.", "OCT 14")
    ) }

    var userName by rememberSaveable { mutableStateOf("John Doe") }
    var userBio by rememberSaveable { mutableStateOf("Focus Mode: Active") }

    // Overlays state
    var isAddingReminder by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var isAddingNote by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Dynamic Content
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedItem,
                transitionSpec = {
                    fadeIn(tween(500)) + scaleIn(initialScale = 0.92f) togetherWith 
                    fadeOut(tween(500))
                },
                label = "NavigationTransition"
            ) { destination ->
                when (destination) {
                    NavItem.HOME -> GeneralHome(
                        reminders = reminders,
                        onAddReminder = { isAddingReminder = true },
                        onToggleReminder = { id ->
                            val index = reminders.indexOfFirst { it.id == id }
                            if (index != -1) {
                                reminders[index] = reminders[index].copy(isCompleted = !reminders[index].isCompleted)
                            }
                        },
                        onEditReminder = { reminder -> reminderToEdit = reminder },
                        onDeleteReminder = { id -> reminders.removeIf { it.id == id } }
                    )
                    NavItem.CALENDAR -> UniversalCalendar()
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
                        }
                    )
                }
            }
        }

        // Universal Floating Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            UniversalDock(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        }

        // Add/Edit Reminder Screen Overlay
        AnimatedVisibility(
            visible = isAddingReminder || reminderToEdit != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NewReminderScreen(
                initialReminder = reminderToEdit,
                onDismiss = { 
                    isAddingReminder = false
                    reminderToEdit = null
                },
                onSave = { reminder ->
                    if (reminderToEdit != null) {
                        val index = reminders.indexOfFirst { it.id == reminderToEdit!!.id }
                        if (index != -1) {
                            reminders[index] = reminder.copy(id = reminderToEdit!!.id)
                        }
                        reminderToEdit = null
                    } else {
                        reminders.add(reminder.copy(id = (reminders.maxOfOrNull { it.id } ?: 0) + 1))
                        isAddingReminder = false
                    }
                }
            )
        }

        // Add Note Dialog Overlay
        if (isAddingNote) {
            NewNoteDialog(
                onDismiss = { isAddingNote = false },
                onSave = { title, content ->
                    notes.add(Note(notes.size + 1, title, content, "NOW"))
                    isAddingNote = false
                }
            )
        }
    }
}

@Composable
fun UniversalDock(selectedItem: NavItem, onItemSelected: (NavItem) -> Unit) {
    Surface(
        modifier = Modifier
            .height(64.dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(32.dp),
        color = SurfaceColor.copy(alpha = 0.98f),
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
                        .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                        .clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// --- SCREEN: HOME ---
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
                
                Surface(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = PrimaryIndigo
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Column {
                            Text("DAILY GOAL", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("$completed of $total Tasks Done", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f)
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

            item {
                Text("Upcoming Today", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            items(reminders, key = { it.id }) { reminder ->
                TaskRow(
                    reminder = reminder, 
                    onToggle = { onToggleReminder(reminder.id) },
                    onEdit = { onEditReminder(reminder) },
                    onDelete = { onDeleteReminder(reminder.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
        }

        // ADD REMINDER FAB
        FloatingActionButton(
            onClick = onAddReminder,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 24.dp),
            containerColor = PrimaryIndigo,
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
        color = SurfaceColor
    ) {
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
fun TaskRow(
    reminder: Reminder, 
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(24.dp).border(2.dp, if(reminder.isCompleted) SuccessEmerald else PrimaryIndigo, CircleShape).padding(4.dp)
            ) {
                if (reminder.isCompleted) Icon(Icons.Rounded.Check, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title, 
                    color = if(reminder.isCompleted) TextSecondary else TextPrimary, 
                    fontWeight = FontWeight.Medium,
                    style = if(reminder.isCompleted) MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyMedium
                )
                Text("${reminder.time} • ${reminder.category}", color = TextSecondary, fontSize = 12.sp)
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More Options", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceColor)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = PrimaryIndigo) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = DangerRose) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = DangerRose) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

// --- SCREEN: NEW REMINDER (Full Overlay) ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewReminderScreen(
    initialReminder: Reminder? = null,
    onDismiss: () -> Unit, 
    onSave: (Reminder) -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var notes by remember { mutableStateOf(initialReminder?.description ?: "") }
    var priority by remember { mutableStateOf(initialReminder?.priority ?: "Low") }
    var category by remember { mutableStateOf(initialReminder?.category ?: "General") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgColor
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Reminder", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Task Info
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Task Information", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("What needs to be done?", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = SurfaceColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Add more details or notes...", color = TextSecondary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = SurfaceColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }

                // Set Priority
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Set Priority", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("LOW" to SuccessEmerald, "MEDIUM" to WarningAmber, "HIGH" to DangerRose).forEach { (label, color) ->
                                val isSelected = priority.equals(label, ignoreCase = true)
                                Surface(
                                    modifier = Modifier.weight(1f).height(48.dp).clickable { priority = label },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) color else SurfaceColor
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(label, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Schedule & Category
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Schedule & Category", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScheduleCard("Date", "Jun 19", Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                            ScheduleCard("Time", "09:52 pm", Icons.Rounded.Schedule, Modifier.weight(1f))
                        }
                        
                        Text("Category", color = TextSecondary, fontSize = 12.sp)
                        val categories = listOf("General", "Class", "Meal", "Personal", "Work")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = category == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { category = cat },
                                    label = { Text(cat) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceColor,
                                        labelColor = TextSecondary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = { 
                    if (title.isNotBlank()) onSave(Reminder(0, title, notes, "10:00 AM", priority = priority, category = category))
                },
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Create Reminder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceColor
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = TextSecondary, fontSize = 10.sp)
                Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// --- SCREEN: CALENDAR ---
@Composable
fun UniversalCalendar() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        item { Spacer(modifier = Modifier.height(72.dp)) }
        item {
            Text("Planner", color = TextPrimary, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("October 2024", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = TextPrimary) }
                    IconButton(onClick = {}) { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextPrimary) }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            val days = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until 5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0 until 7) {
                            val i = row * 7 + col
                            if (i < 31) {
                                val isSelected = i == 14
                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${i + 1}", color = if (isSelected) Color.White else TextPrimary, fontSize = 14.sp)
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Text("Events", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = SurfaceColor) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(4.dp, 32.dp).clip(CircleShape).background(WarningAmber))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Dinner with Family", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("7:00 PM", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

// --- SCREEN: NOTES ---
@Composable
fun GeneralNotes(
    notes: List<Note>,
    onAddNote: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(72.dp)) }
            item {
                Text("My Notes", color = TextPrimary, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val chunks = notes.chunked(2)
                    chunks.forEach { rowNotes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowNotes.forEach { note ->
                                NoteCard(note, Modifier.weight(1f))
                            }
                            if (rowNotes.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(110.dp)) }
        }
        
        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 110.dp, end = 24.dp),
            containerColor = PrimaryIndigo,
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "New Note")
        }
    }
}

@Composable
fun NoteCard(note: Note, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(note.title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                note.content,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(note.date, color = PrimaryIndigo, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- SCREEN: NEW NOTE (Dialog) ---
@Composable
fun NewNoteDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceColor
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("New Note", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BgColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Note content", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BgColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onSave(title, content) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo.copy(alpha = if(title.isBlank()) 0.3f else 1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

// --- SCREEN: FOCUS (Timer & Stopwatch) ---
@Composable
fun ProductivityTools() {
    var selectedTool by remember { mutableIntStateOf(0) } // 0: Timer, 1: Pomodoro, 2: Watch
    
    // Timer Logic
    var timerTime by remember { mutableLongStateOf(10 * 60 * 1000L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    
    // Pomodoro Logic
    var pomoTime by remember { mutableLongStateOf(25 * 60 * 1000L) }
    var isPomoRunning by remember { mutableStateOf(false) }
    
    // Stopwatch Logic
    var stopWatchTime by remember { mutableLongStateOf(0L) }
    var isStopWatchRunning by remember { mutableStateOf(false) }

    // Coroutine for timing
    LaunchedEffect(isTimerRunning, isPomoRunning, isStopWatchRunning, selectedTool) {
        while (true) {
            delay(100)
            when (selectedTool) {
                0 -> if (isTimerRunning && timerTime > 0) timerTime -= 100
                1 -> if (isPomoRunning && pomoTime > 0) pomoTime -= 100
                2 -> if (isStopWatchRunning) stopWatchTime += 100
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }
        
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceColor
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("Timer", "Pomodoro", "Watch").forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                                .background(if (selectedTool == index) PrimaryIndigo else Color.Transparent)
                                .clickable { selectedTool = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title, color = if (selectedTool == index) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            val displayTime = when (selectedTool) {
                0 -> {
                    val mins = (timerTime / 1000) / 60
                    val secs = (timerTime / 1000) % 60
                    String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                }
                1 -> {
                    val mins = (pomoTime / 1000) / 60
                    val secs = (pomoTime / 1000) % 60
                    String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                }
                else -> {
                    val hours = (stopWatchTime / 1000) / 3600
                    val mins = ((stopWatchTime / 1000) % 3600) / 60
                    val secs = (stopWatchTime / 1000) % 60
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
                }
            }
            
            val sweepAngle = when (selectedTool) {
                0 -> (timerTime.toFloat() / (10 * 60 * 1000)) * 360f
                1 -> (pomoTime.toFloat() / (25 * 60 * 1000)) * 360f
                else -> ((stopWatchTime % 60000).toFloat() / 60000) * 360f
            }

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(260.dp)) {
                    drawCircle(color = SurfaceColor, style = Stroke(12.dp.toPx()))
                    drawArc(
                        color = PrimaryIndigo,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(12.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp, fontWeight = FontWeight.Light),
                        color = TextPrimary
                    )
                    Text(
                        text = when(selectedTool) { 0 -> "TIMER"; 1 -> "POMODORO"; else -> "STOPWATCH" },
                        color = PrimaryIndigo,
                        letterSpacing = 4.sp,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        when (selectedTool) {
                            0 -> { timerTime = 10 * 60 * 1000L; isTimerRunning = false }
                            1 -> { pomoTime = 25 * 60 * 1000L; isPomoRunning = false }
                            2 -> { stopWatchTime = 0L; isStopWatchRunning = false }
                        }
                    }, 
                    modifier = Modifier.size(56.dp).background(SurfaceColor, CircleShape)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TextPrimary)
                }
                
                val isRunning = when(selectedTool) { 0 -> isTimerRunning; 1 -> isPomoRunning; else -> isStopWatchRunning }
                
                Surface(
                    modifier = Modifier.size(80.dp), 
                    shape = CircleShape, 
                    color = if(isRunning) SuccessEmerald else PrimaryIndigo, 
                    onClick = {
                        when (selectedTool) {
                            0 -> isTimerRunning = !isTimerRunning
                            1 -> isPomoRunning = !isPomoRunning
                            2 -> isStopWatchRunning = !isStopWatchRunning
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if(isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = {}, modifier = Modifier.size(56.dp).background(SurfaceColor, CircleShape)) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = TextPrimary)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

// --- SCREEN: ME (Profile) ---
@Composable
fun UniversalProfile(
    name: String,
    bio: String,
    onUpdateProfile: (String, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(name) }
    var tempBio by remember { mutableStateOf(bio) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(72.dp)) }

            // Profile Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(110.dp),
                            shape = CircleShape,
                            color = PrimaryIndigo.copy(alpha = 0.15f),
                            border = BorderStroke(2.dp, PrimaryIndigo)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = PrimaryIndigo)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = name, color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = bio, color = SuccessEmerald, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        onClick = {
                            tempName = name
                            tempBio = bio
                            isEditing = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = PrimaryIndigo,
                        modifier = Modifier.width(160.dp).height(48.dp),
                        shadowElevation = 8.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text("PREFERENCES", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp))
            }
            item { ProfileOption("Personal Details", Icons.Rounded.Badge) }
            item { ProfileOption("Notifications", Icons.Rounded.NotificationsActive) }
            item { ProfileOption("Appearance", Icons.Rounded.Palette) }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item {
                Text("SUPPORT", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp))
            }
            item { ProfileOption("Terms & Privacy", Icons.Rounded.Policy) }
            item { ProfileOption("Help & Support", Icons.Rounded.SupportAgent) }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Text("Version 3.0.0", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            item { Spacer(modifier = Modifier.height(110.dp)) }
        }

        // Edit Profile Overlay
        AnimatedVisibility(
            visible = isEditing,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = BgColor.copy(alpha = 0.98f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item { Text("Update Identity", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold) }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    
                    item {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = PrimaryIndigo.copy(alpha = 0.1f)) {
                                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(72.dp), tint = PrimaryIndigo)
                            }
                            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = PrimaryIndigo, border = BorderStroke(2.dp, BgColor)) {
                                IconButton(onClick = {}) { Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = SurfaceColor, focusedLabelColor = PrimaryIndigo, unfocusedLabelColor = TextSecondary, cursorColor = PrimaryIndigo, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = tempBio,
                            onValueChange = { tempBio = it },
                            label = { Text("Bio / Status") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = SurfaceColor, focusedLabelColor = PrimaryIndigo, unfocusedLabelColor = TextSecondary, cursorColor = PrimaryIndigo, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SurfaceColor)) {
                                Text("Cancel", color = TextPrimary)
                            }
                            Button(onClick = { onUpdateProfile(tempName, tempBio); isEditing = false }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)) {
                                Text("Save", color = Color.White)
                            }
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
    Surface(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        onClick = {}
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = PrimaryIndigo.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp)) }
            }
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
