package com.hearthush.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hearthush.app.logic.AuthService
import com.hearthush.app.logic.EventItem
import com.hearthush.app.logic.EventStore
import com.hearthush.app.logic.alarmBeep
import com.hearthush.app.logic.alarmStop
import com.hearthush.app.logic.platformDataDir
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.delay

private val FILTERS = listOf("All", "Today", "Next 7 days", "Featured", "With secret")
private val SORTS = listOf("Happening next", "Name A–Z", "Biggest countdown")
private val ACCENTS = listOf(
    "Auto" to "",
    "Pink" to "#FF5D97",
    "Violet" to "#7C6CFF",
    "Teal" to "#22C4A8",
    "Amber" to "#FFB020",
    "Sky" to "#38BDF8",
    "Rose" to "#F472B6",
    "Green" to "#4ADE80"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val store = remember { EventStore(platformDataDir()).also { it.load() } }
    val auth = remember { AuthService() }
    var tick by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FILTERS[0]) }
    var sort by remember { mutableStateOf(SORTS[0]) }
    var editing by remember { mutableStateOf<EventItem?>(null) }
    var editIsNew by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var secretOf by remember { mutableStateOf<EventItem?>(null) }
    var alarmOf by remember { mutableStateOf<EventItem?>(null) }
    var confirmDelete by remember { mutableStateOf<EventItem?>(null) }
    val shownSecrets = remember { mutableSetOf<String>() }

    fun refresh() {
        tick++
    }

    // per-second ticker for live countdowns
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }

    // due-today prompts on launch
    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        for (e in store.items()) {
            if (e.isDueToday(today) && !shownSecrets.contains(e.id)) {
                shownSecrets.add(e.id)
                if (e.hasSecret()) secretOf = e else {
                    if (e.soundEnabled) alarmBeep()
                    alarmOf = e
                }
            }
        }
    }

    val today = LocalDate.now()
    val now = LocalDateTime.now()
    @Suppress("UNUSED_EXPRESSION")
    tick
    val shown = remember(tick, query, filter, sort, store) {
        var list = store.sortedByNext(today).filter { e ->
            (query.isBlank() || (e.title + " " + e.message + " " + e.displayCategory())
                .contains(query.trim(), ignoreCase = true)) &&
                when (filter) {
                    "Today" -> e.isDueToday(today)
                    "Next 7 days" -> !e.isPast(today) && e.daysUntil(today) <= 7
                    "Featured" -> e.featured
                    "With secret" -> e.hasSecret()
                    else -> true
                }
        }
        list = when (sort) {
            "Name A–Z" -> list.sortedBy { it.title.lowercase() }
            "Biggest countdown" -> list.sortedByDescending { it.daysUntil(today) }
            else -> list
        }
        list
    }
    var todayN = 0
    var weekN = 0
    for (e in store.items()) {
        if (e.isDueToday(today)) todayN++
        else if (!e.isPast(today) && e.daysUntil(today) <= 7) weekN++
    }
    val admin = auth.isAdmin()

    HeartHushTheme {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("♥ HeartHush Countdowns") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Text(
                "${store.items().size} total • $todayN today • $weekN this week • " +
                    if (shown.isEmpty()) "nothing" else "next: ${shown[0].title}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp
            )
            OutlinedTextField(
                query, { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                placeholder = { Text("Search countdowns…") },
                singleLine = true
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropDown(
                    "Filter", FILTERS, filter, { filter = it },
                    Modifier.weight(1f)
                )
                DropDown(
                    "Sort", SORTS, sort, { sort = it },
                    Modifier.weight(1f)
                )
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(shown, key = { it.id }) { e ->
                    EventCard(
                        e, now, admin,
                        onEdit = { editing = e.copyFromJson(); editIsNew = false },
                        onDuplicate = {
                            val copy = e.copyFromJson()
                            copy.id = UUID.randomUUID().toString().replace("-", "")
                            copy.title = e.title + " (copy)"
                            copy.createdAt = LocalDateTime.now()
                            store.addOrUpdate(copy)
                            refresh()
                        },
                        onRing = {
                            if (e.soundEnabled) alarmBeep()
                            if (e.hasSecret()) secretOf = e else alarmOf = e
                        },
                        onDelete = { confirmDelete = e }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (admin) {
                    Button(onClick = {
                        val item = EventItem()
                        item.date = LocalDate.now().plusDays(7)
                        editing = item
                        editIsNew = true
                    }) { Text("+ New") }
                    OutlinedButton(onClick = { alarmBeep() }) { Text("Test") }
                    OutlinedButton(onClick = { showPassword = true }) { Text("Password") }
                    OutlinedButton(onClick = {
                        sample(store, "Birthday 🎂", 14, "Birthday", "🎂", "#FFB020", "Cake, friends and music!", true)
                        sample(store, "Final Exams 🎓", 30, "Exam", "🎓", "#22C4A8", "One chapter a day keeps stress away.", true)
                        sample(store, "Android App Launch 🚀", 60, "App Release", "🚀", "#7C6CFF", "Release v2.0 to the Play Store.", false)
                        sample(store, "Beach Trip ✈", 90, "Trip", "✈", "#38BDF8", "Sunscreen, playlists, passports.", false)
                        sample(store, "Wedding Day 💖", 120, "Wedding", "💖", "#F472B6", "The big day!", true)
                        refresh()
                    }) { Text("Samples") }
                }
                OutlinedButton(onClick = {
                    if (admin) {
                        auth.logout()
                        refresh()
                    } else showLogin = true
                }) { Text(if (admin) "Log out" else "Admin login") }
            }
        }

        editing?.let { item ->
            EditDialog(
                item, editIsNew,
                onSave = { store.addOrUpdate(it); editing = null; refresh() },
                onDelete = { store.delete(it.id); editing = null; refresh() },
                onCancel = { editing = null }
            )
        }
        if (showLogin) {
            var pw by remember { mutableStateOf("") }
            var denied by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showLogin = false },
                title = { Text("Admin access") },
                text = {
                    Column {
                        Text("Unlock create / edit / delete. First run password: admin123")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            pw, { pw = it; denied = false },
                            placeholder = { Text("Password") },
                            singleLine = true
                        )
                        if (denied) Text("Access denied.", color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (auth.verify(pw)) {
                            showLogin = false
                            refresh()
                        } else denied = true
                    }) { Text("UNLOCK") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogin = false }) { Text("CANCEL") }
                }
            )
        }
        if (showPassword) {
            var cur by remember { mutableStateOf("") }
            var next by remember { mutableStateOf("") }
            var err by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showPassword = false },
                title = { Text("Admin password") },
                text = {
                    Column {
                        OutlinedTextField(cur, { cur = it }, placeholder = { Text("Current password") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(next, { next = it }, placeholder = { Text("New password (4+ chars)") }, singleLine = true)
                        if (err) Text("Incorrect or too short.", color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (auth.changePassword(cur, next)) showPassword = false
                        else err = true
                    }) { Text("CHANGE") }
                },
                dismissButton = {
                    TextButton(onClick = { showPassword = false }) { Text("CANCEL") }
                }
            )
        }
        secretOf?.let { item ->
            var opened by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { secretOf = null },
                title = { Text("💌 You have a message") },
                text = {
                    Column {
                        Text("${item.title} • ${item.displayCategory()} — the day is here!")
                        Spacer(Modifier.height(8.dp))
                        if (opened) Text(item.secretMessage)
                        else Text("The countdown reached zero. Open your message when you're ready.")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { opened = true },
                        enabled = !opened
                    ) { Text(if (opened) "OPENED" else "OPEN MESSAGE") }
                },
                dismissButton = {
                    TextButton(onClick = { secretOf = null }) { Text("CLOSE") }
                }
            )
        }
        alarmOf?.let { item ->
            val msg = if (item.message.trim().isEmpty()) "The day has arrived - open the app to celebrate!"
            else item.message
            AlertDialog(
                onDismissRequest = { alarmOf = null },
                title = { Text("${item.displayIcon()} It's time — ${item.title}") },
                text = { Text("${item.displayCategory()} • ${item.dateLabel()}\n\n$msg") },
                confirmButton = {
                    TextButton(onClick = { alarmStop(); alarmOf = null }) { Text("STOP") }
                },
                dismissButton = {
                    TextButton(onClick = { alarmStop(); alarmOf = null }) { Text("SNOOZE") }
                }
            )
        }
        confirmDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text("Delete countdown") },
                text = { Text("Delete '${item.title}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        store.delete(item.id)
                        confirmDelete = null
                        refresh()
                    }) { Text("YES") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("NO") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropDown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }, modifier = modifier) {
        OutlinedTextField(
            selected, {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(open, { open = false }) {
            for (o in options) {
                DropdownMenuItem(
                    { Text(o) },
                    onClick = { onSelect(o); open = false }
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    e: EventItem,
    now: LocalDateTime,
    admin: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRing: () -> Unit,
    onDelete: () -> Unit
) {
    val today = now.toLocalDate()
    val due = e.isDueToday(today)
    val accent = e.accentColor(Brand)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.displayIcon(), fontSize = 30.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        (if (e.featured) "★ " else "") + e.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    val bits = mutableListOf(
                        e.displayCategory().uppercase(),
                        e.shortCountdown(today).uppercase()
                    )
                    if (e.hasSecret()) bits.add("SECRET ARMED")
                    if (e.repeatYearly) bits.add("YEARLY")
                    Text(bits.joinToString(" • "), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (due) "♥ DAY IS HERE ♥" else "♥ LIVE ♥",
                        color = if (due) Success else accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(e.countdownText(now), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(e.shortCountdown(today), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("📅 ${e.dateLabel()}", fontSize = 12.sp)
            Text(if (e.message.isEmpty()) "A special moment is waiting…" else e.message, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { e.progress01(today) },
                modifier = Modifier.fillMaxWidth()
            )
            if (admin) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDuplicate) { Text("Copy") }
                    TextButton(onClick = onRing) { Text("Ring") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDialog(
    initial: EventItem,
    isNew: Boolean,
    onSave: (EventItem) -> Unit,
    onDelete: (EventItem) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var category by remember { mutableStateOf(initial.displayCategory()) }
    var icon by remember { mutableStateOf(initial.displayIcon()) }
    var date by remember { mutableStateOf(initial.date) }
    var yearly by remember { mutableStateOf(initial.repeatYearly) }
    var featured by remember { mutableStateOf(initial.featured) }
    var sound by remember { mutableStateOf(initial.soundEnabled) }
    var secretOn by remember { mutableStateOf(initial.secretEnabled) }
    var message by remember { mutableStateOf(initial.message) }
    var secretMsg by remember { mutableStateOf(initial.secretMessage) }
    var accentIdx by remember {
        mutableStateOf(maxOf(0, ACCENTS.indexOfFirst { it.second == initial.accentHex }))
    }
    var showDate by remember { mutableStateOf(false) }
    var titleErr by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (isNew) "Create countdown" else "Edit countdown") },
        text = {
            Column {
                OutlinedTextField(
                    title, { title = it; titleErr = false },
                    label = { Text("Title") },
                    singleLine = true,
                    isError = titleErr
                )
                Spacer(Modifier.height(6.dp))
                DropDown("Category", EventItem.CATEGORY_PRESETS, category, { category = it })
                Spacer(Modifier.height(6.dp))
                DropDown("Icon", EventItem.ICON_PRESETS, icon, { icon = it })
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Target date: $date")
                }
                if (showDate) {
                    val state = rememberDatePickerState(
                        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDate = false },
                        confirmButton = {
                            TextButton(onClick = {
                                state.selectedDateMillis?.let {
                                    date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                }
                                showDate = false
                            }) { Text("OK") }
                        }
                    ) { DatePicker(state) }
                }
                CheckRow("Repeat every year", yearly) { yearly = it }
                CheckRow("Featured (pinned first)", featured) { featured = it }
                CheckRow("Sound alert", sound) { sound = it }
                CheckRow("Secret message at zero", secretOn) { secretOn = it }
                DropDown("Accent", ACCENTS.map { it.first }, ACCENTS[accentIdx].first, {
                    accentIdx = ACCENTS.indexOfFirst { a -> a.first == it }
                })
                OutlinedTextField(message, { message = it }, label = { Text("Public message") })
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(secretMsg, { secretMsg = it }, label = { Text("Secret message") })
                if (titleErr) Text("Enter a countdown title.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.trim().isEmpty()) {
                    titleErr = true
                    return@TextButton
                }
                val item = initial.copyFromJson()
                item.title = title.trim()
                item.date = date
                item.category = category.trim().ifEmpty { "Countdown" }
                item.icon = icon.trim().ifEmpty { "📅" }
                item.accentHex = ACCENTS[accentIdx].second
                item.repeatYearly = yearly
                item.featured = featured
                item.soundEnabled = sound
                item.secretEnabled = secretOn
                item.message = message.trim()
                item.secretMessage = secretMsg
                if (!item.secretEnabled) item.secretMessage = ""
                onSave(item)
            }) { Text("SAVE") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = { onDelete(initial) }) { Text("DELETE") }
                TextButton(onClick = onCancel) { Text("CANCEL") }
            }
        }
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange)
        Text(label)
    }
}

private fun sample(
    store: EventStore,
    title: String,
    daysOut: Long,
    category: String,
    icon: String,
    accent: String,
    msg: String,
    yearly: Boolean
) {
    val e = EventItem()
    e.title = title
    e.date = LocalDate.now().plusDays(daysOut)
    e.category = category
    e.icon = icon
    e.accentHex = accent
    e.message = msg
    e.repeatYearly = yearly
    e.soundEnabled = true
    store.addOrUpdate(e)
}
