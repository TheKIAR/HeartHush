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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hearthush.app.logic.EventItem
import com.hearthush.app.logic.EventStore
import com.hearthush.app.logic.PairStore
import com.hearthush.app.logic.PinLock
import com.hearthush.app.logic.SyncEngine
import com.hearthush.app.logic.alarmBeep
import com.hearthush.app.logic.alarmStop
import com.hearthush.app.logic.platformDataDir
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val pin = remember { PinLock() }
    val pair = remember { PairStore() }
    val engine = remember { SyncEngine(store, pair) }
    val scope = rememberCoroutineScope()

    var tick by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FILTERS[0]) }
    var sort by remember { mutableStateOf(SORTS[0]) }
    var editing by remember { mutableStateOf<EventItem?>(null) }
    var editIsNew by remember { mutableStateOf(false) }
    var showConnect by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var secretOf by remember { mutableStateOf<EventItem?>(null) }
    var alarmOf by remember { mutableStateOf<EventItem?>(null) }
    var confirmDelete by remember { mutableStateOf<EventItem?>(null) }
    var severPrompt by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    val shownSecrets = remember { mutableSetOf<String>() }

    fun refresh() {
        tick++
    }

    fun doSync() {
        if (syncing) return
        syncing = true
        scope.launch {
            try {
                val res = engine.syncNow()
                if (res.justPaired) notice = "Connected! You can now send countdowns to each other."
                if (res.severAsked) severPrompt = true
                if (res.severDeclined) notice = "Your partner declined to disconnect. Still connected."
                if (res.severed) notice = "Connection severed by mutual agreement."
                if (res.offline) notice = "Offline — will retry automatically."
            } finally {
                syncing = false
                refresh()
            }
        }
    }

    // per-second ticker for live countdowns
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    // online sync every 25s + once at launch
    LaunchedEffect(Unit) {
        doSync()
        while (true) {
            delay(25_000)
            doSync()
        }
    }

    if (!pin.isUnlocked()) {
        PinGate(pin, onUnlock = { refresh() })
        return
    }

    val today = LocalDate.now()
    val now = LocalDateTime.now()
    @Suppress("UNUSED_EXPRESSION")
    tick
    val myId = pair.accountId
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

    // due-today reveals: personal items and partner-sent items open here;
    // items I sent are revealed on the partner's device instead.
    LaunchedEffect(tick) {
        for (e in store.items()) {
            if (!e.isDueToday(today) || shownSecrets.contains(e.id)) continue
            val mine = e.isMine(myId)
            val forMe = e.isForMe(myId)
            if (!mine && !forMe) continue
            if (mine && e.forPartner) continue
            shownSecrets.add(e.id)
            if (e.soundEnabled) alarmBeep()
            if (e.hasSecret()) secretOf = e else alarmOf = e
            if (forMe) {
                scope.launch { engine.sendDelivered(e.id) }
            }
        }
    }

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
                    if (shown.isEmpty()) "nothing" else "next: ${shown[0].title}" +
                        if (pair.isPaired()) " • ✉ ${pair.partnerCode()}" else " • not connected",
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
                DropDown("Filter", FILTERS, filter, { filter = it }, Modifier.weight(1f))
                DropDown("Sort", SORTS, sort, { sort = it }, Modifier.weight(1f))
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(shown, key = { it.id }) { e ->
                    EventCard(
                        e, now, myId,
                        onEdit = { editing = e.copyFromJson(); editIsNew = false },
                        onDuplicate = {
                            val copy = e.copyFromJson()
                            copy.id = UUID.randomUUID().toString().replace("-", "")
                            copy.title = e.title + " (copy)"
                            copy.createdAt = LocalDateTime.now()
                            copy.senderId = myId
                            store.addOrUpdate(copy)
                            if (copy.forPartner) scope.launch { engine.sendCountdown(copy) }
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
                Button(onClick = {
                    val item = EventItem()
                    item.date = LocalDate.now().plusDays(7)
                    item.senderId = myId
                    editing = item
                    editIsNew = true
                }) { Text("+ New") }
                OutlinedButton(onClick = {
                    sample(store, myId, "Birthday 🎂", 14, "Birthday", "🎂", "#FFB020", "Cake, friends and music!", true)
                    sample(store, myId, "Final Exams 🎓", 30, "Exam", "🎓", "#22C4A8", "One chapter a day keeps stress away.", true)
                    sample(store, myId, "Android App Launch 🚀", 60, "App Release", "🚀", "#7C6CFF", "Release v2.0 to the Play Store.", false)
                    sample(store, myId, "Beach Trip ✈", 90, "Trip", "✈", "#38BDF8", "Sunscreen, playlists, passports.", false)
                    sample(store, myId, "Wedding Day 💖", 120, "Wedding", "💖", "#F472B6", "The big day!", true)
                    refresh()
                }) { Text("Samples") }
                OutlinedButton(onClick = { doSync() }) { Text(if (syncing) "Sync…" else "Sync") }
                OutlinedButton(onClick = { showConnect = true }) {
                    Text(if (pair.isPaired()) "✉ ${pair.partnerCode()}" else "Connect")
                }
                OutlinedButton(onClick = { showPin = true }) { Text("PIN") }
            }
        }

        editing?.let { item ->
            EditDialog(
                item, editIsNew, pair,
                onSave = { saved, send ->
                    store.addOrUpdate(saved)
                    if (send) scope.launch { engine.sendCountdown(saved) }
                    editing = null
                    refresh()
                },
                onDelete = { store.delete(it.id); editing = null; refresh() },
                onCancel = { editing = null }
            )
        }
        if (showConnect) {
            ConnectDialog(
                pair, engine,
                onClose = { showConnect = false; refresh() },
                onNotice = { notice = it; refresh() },
                onSeverPrompt = { severPrompt = true }
            )
        }
        if (showPin) {
            PinDialog(pin, onClose = { showPin = false; refresh() })
        }
        if (severPrompt && pair.isPaired()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Partner wants to disconnect") },
                text = { Text("Your partner asked to sever the connection. It only ends if you also agree. Agree?") },
                confirmButton = {
                    TextButton(onClick = {
                        severPrompt = false
                        scope.launch {
                            engine.agreeSever()
                            notice = "Connection severed by mutual agreement."
                            refresh()
                        }
                    }) { Text("AGREE") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        severPrompt = false
                        scope.launch {
                            engine.declineSever()
                            refresh()
                        }
                    }) { Text("KEEP CONNECTED") }
                }
            )
        }
        notice?.let { msg ->
            AlertDialog(
                onDismissRequest = { notice = null },
                title = { Text("HeartHush") },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { notice = null }) { Text("OK") }
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
                        if (opened) Text(if (item.message.isNotEmpty()) item.message + "\n\n" + item.secretMessage else item.secretMessage)
                        else Text("The countdown reached zero. Open your message when you're ready.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { opened = true }, enabled = !opened) {
                        Text(if (opened) "OPENED" else "OPEN MESSAGE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { secretOf = null }) { Text("CLOSE") }
                }
            )
        }
        alarmOf?.let { item ->
            val msg = if (item.message.trim().isEmpty()) "The day has arrived!" else item.message
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
                text = { Text("Delete '${item.title}'?" + if (item.forPartner) "\n(This removes it on this device only.)" else "") },
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

@Composable
private fun PinGate(pin: PinLock, onUnlock: () -> Unit) {
    var entry by remember { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }
    HeartHushTheme {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("♥", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("HeartHush", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (pin.isDefaultPin()) "First run PIN is 1234 — change it in PIN settings."
                else "Enter your app PIN.",
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                entry, { entry = it.filter { c -> c.isDigit() }.take(8); denied = false },
                placeholder = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            if (denied) Text("Wrong PIN.", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (pin.unlock(entry)) {
                    entry = ""
                    onUnlock()
                } else denied = true
            }) { Text("UNLOCK") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectDialog(
    pair: PairStore,
    engine: SyncEngine,
    onClose: () -> Unit,
    onNotice: (String) -> Unit,
    onSeverPrompt: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var incoming by remember { mutableStateOf(pair.incoming()) }
    var pending by remember { mutableStateOf(pair.pendingCode()) }
    var paired by remember { mutableStateOf(pair.isPaired()) }
    var waiting by remember { mutableStateOf(pair.wantSever()) }

    fun reload() {
        incoming = pair.incoming()
        pending = pair.pendingCode()
        paired = pair.isPaired()
        waiting = pair.wantSever()
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Connect to a partner") },
        text = {
            Column {
                Text("Your code:", fontWeight = FontWeight.Bold)
                Text(pair.myCode, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tell your partner this code. Enter THEIR code below — when you both enter each other's codes, you're connected.",
                    fontSize = 12.sp
                )
                if (paired) {
                    Spacer(Modifier.height(8.dp))
                    Text("✉ Connected to ${pair.partnerCode()}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (waiting) {
                        Text("Waiting for your partner to agree to disconnect…", fontSize = 12.sp)
                    } else {
                        OutlinedButton(onClick = {
                            busy = true
                            scope.launch {
                                val ok = engine.requestSever()
                                busy = false
                                reload()
                                onNotice(if (ok) "Disconnect requested. It ends only if your partner also agrees." else "Offline — request will be retried on next sync.")
                            }
                        }) { Text("DISCONNECT") }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        code, { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6); err = null },
                        label = { Text("Partner's code") },
                        singleLine = true
                    )
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = {
                        val clean = code.trim().uppercase()
                        if (!PairStore.looksLikeCode(clean)) {
                            err = "Enter the 6-letter code."
                            return@Button
                        }
                        if (clean == pair.myCode) {
                            err = "That's your own code."
                            return@Button
                        }
                        busy = true
                        scope.launch {
                            val ok = engine.sendPairRequest(clean)
                            busy = false
                            reload()
                            onNotice(
                                if (ok) "Request sent to $clean. Ask them to enter YOUR code (${pair.myCode}) to complete."
                                else "Offline — couldn't send. Try Sync later."
                            )
                        }
                    }) { Text(if (busy) "…" else "SEND REQUEST") }
                    if (pending.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Waiting on $pending…", fontSize = 12.sp)
                    }
                    if (incoming.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Wants to connect:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        for (req in incoming) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(req.code, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    code = req.code
                                }) { Text("ENTER CODE") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    engine.syncNow()
                    reload()
                    onClose()
                }
            }) { Text("SYNC & CLOSE") }
        }
    )
}

@Composable
private fun PinDialog(pin: PinLock, onClose: () -> Unit) {
    var cur by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("App PIN") },
        text = {
            Column {
                Text("First run PIN is 1234.", fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(cur, { cur = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("Current PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(next, { next = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("New PIN (4+ digits)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(confirm, { confirm = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("Confirm new PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (done) Text("PIN updated.", color = Success, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (next != confirm) {
                    err = "New PINs do not match."
                    return@TextButton
                }
                if (pin.changePin(cur, next)) {
                    err = null
                    done = true
                    cur = ""
                    next = ""
                    confirm = ""
                } else err = "Wrong current PIN, or too short."
            }) { Text("CHANGE") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { pin.lock(); onClose() }) { Text("LOCK NOW") }
                TextButton(onClick = onClose) { Text("CLOSE") }
            }
        }
    )
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
    myId: String,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRing: () -> Unit,
    onDelete: () -> Unit
) {
    val today = now.toLocalDate()
    val due = e.isDueToday(today)
    val accent = e.accentColor(Brand)
    val mine = e.isMine(myId)
    val forMe = e.isForMe(myId)
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
                    if (forMe) bits.add("🎁 FOR YOU")
                    else if (mine && e.forPartner) bits.add(if (e.delivered) "✉ DELIVERED" else "✉ TO PARTNER")
                    if (e.hasSecret() && !forMe) bits.add("SECRET ARMED")
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
            if (forMe && !due) {
                Text("🎁 A surprise from your partner — the message arrives at zero.", fontSize = 13.sp)
            } else {
                val body = if (e.message.isEmpty()) "A special moment is waiting…" else e.message
                Text(body, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { e.progress01(today) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text(if (forMe && !mine) "View" else "Edit") }
                if (mine) {
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
    pair: PairStore,
    onSave: (EventItem, Boolean) -> Unit,
    onDelete: (EventItem) -> Unit,
    onCancel: () -> Unit
) {
    val partnerOpt = if (pair.isPaired()) "To partner (${pair.partnerCode()})" else null
    val audiences = if (partnerOpt != null) listOf("Just me", partnerOpt) else listOf("Just me")
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
    var audience by remember {
        mutableStateOf(if (initial.forPartner && partnerOpt != null) partnerOpt else "Just me")
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
                if (audiences.size > 1) {
                    DropDown("Send to", audiences, audience, { audience = it })
                    Spacer(Modifier.height(6.dp))
                    if (audience != "Just me") {
                        Text("They'll see the countdown; the message arrives at zero.", fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                }
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
                OutlinedTextField(message, { message = it }, label = { Text("Message") })
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
                if (item.senderId.isEmpty()) item.senderId = pair.accountId
                val send = audience != "Just me" && pair.isPaired()
                item.forPartner = send
                if (!send) item.delivered = false
                onSave(item, send)
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
    myId: String,
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
    e.senderId = myId
    e.forPartner = false
    store.addOrUpdate(e)
}
