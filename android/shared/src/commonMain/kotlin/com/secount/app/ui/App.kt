package com.secount.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.secount.app.logic.BackupCrypto
import com.secount.app.logic.EventItem
import com.secount.app.logic.EventStore
import com.secount.app.logic.PairStore
import com.secount.app.logic.PhotoLockGuard
import com.secount.app.logic.PinLock
import com.secount.app.logic.SyncEngine
import com.secount.app.logic.alarmBeep
import com.secount.app.logic.alarmStop
import com.secount.app.logic.biometricAuthenticate
import com.secount.app.logic.biometricAvailable
import com.secount.app.logic.copyToClipboard
import com.secount.app.logic.deletePhotoFile
import com.secount.app.logic.getClipboardText
import com.secount.app.logic.loadPhotoBitmap
import com.secount.app.logic.notifySecret
import com.secount.app.logic.nowSec
import com.secount.app.logic.openUrl
import com.secount.app.logic.photoToB64
import com.secount.app.logic.pickPhotoFile
import com.secount.app.logic.platformDataDir
import com.secount.app.logic.prefsGet
import com.secount.app.logic.prefsPut
import com.secount.app.logic.systemLanguage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

private const val NEED_LOCK_KEY = "secount_need_lock"
private const val MUTED_KEY = "secount_muted"
private const val LANG_KEY = "secount_lang"
private const val UPDATE_CHECK_KEY = "secount_update_checked_at"
private const val APP_VERSION = "1.0.0"
private const val RELEASES_URL = "https://github.com/TheKIAR/Secount/releases"

private val LANGS = listOf("System", "en", "de", "fr", "es")

private fun langDisplay(code: String): String = when (code) {
    "en" -> "English"
    "de" -> "Deutsch"
    "fr" -> "Français"
    "es" -> "Español"
    else -> "System"
}

/** Pairing payload for QR / copy-paste. */
private fun pairingText(myCode: String, accountId: String): String = "SECOUNT1:$myCode:$accountId"

/** Accepts a raw 6-letter code or a SECOUNT1:... payload; returns the code or null. */
private fun parsePairCode(input: String): String? {
    val t = input.trim().uppercase()
    if (PairStore.looksLikeCode(t)) return t
    if (t.startsWith("SECOUNT1:")) {
        val parts = t.split(":")
        if (parts.size >= 2 && PairStore.looksLikeCode(parts[1])) return parts[1]
    }
    // Be liberal: find any 6-char token that looks like a code.
    for (tok in t.split(Regex("[^A-Z0-9]+"))) {
        if (PairStore.looksLikeCode(tok)) return tok
    }
    return null
}

private fun httpGetSafe(url: String): String? {
    return try {
        com.secount.app.logic.httpGet(url, 8000)
    } catch (e: Exception) {
        null
    }
}

private fun clearCrashLog() {
    try {
        val f = okio.FileSystem.SYSTEM
        val p = (com.secount.app.logic.platformDataDir() + "/crash_log.txt").toPath()
        if (f.exists(p)) f.delete(p)
    } catch (ignored: Exception) {
    }
}

private fun numVer(v: String): List<Int> {
    return v.trim().trimStart('v', 'V').split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }
}

private fun isNewerVersion(current: String, tag: String): Boolean {
    val c = numVer(current)
    val n = numVer(tag)
    for (i in 0 until maxOf(c.size, n.size)) {
        val a = c.getOrElse(i) { 0 }
        val b = n.getOrElse(i) { 0 }
        if (b > a) return true
        if (b < a) return false
    }
    return false
}

/** Pokes the Android home widget (no-op on desktop). */
private object SecountWidgetPush {
    fun refresh(store: EventStore) {
        try {
            com.secount.app.logic.widgetRefresh(store.exportJson())
        } catch (ignored: Exception) {
        }
    }
}

private val FILTERS = listOf("All", "Today", "Next 7 days", "Featured", "With secret", "Past", "To partner")
private val SORTS = listOf("Happening next", "Name A–Z", "Biggest countdown", "Newest first")
private val REPEATS = listOf("One-time", "Yearly", "Monthly", "Weekly")
private val SOUNDS = listOf("Chime", "Soft", "Silent")
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

    var secTick by remember { mutableStateOf(0) }
    var storeVer by remember { mutableStateOf(0) }
    var unlocked by remember { mutableStateOf(pin.isUnlocked()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FILTERS[0]) }
    var sort by remember { mutableStateOf(SORTS[0]) }
    var viewMode by remember { mutableStateOf("List") }
    var calMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var calDay by remember { mutableStateOf<LocalDate?>(null) }
    var editing by remember { mutableStateOf<EventItem?>(null) }
    var editIsNew by remember { mutableStateOf(false) }
    var showConnect by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var secretOf by remember { mutableStateOf<EventItem?>(null) }
    var alarmOf by remember { mutableStateOf<EventItem?>(null) }
    var confirmDelete by remember { mutableStateOf<EventItem?>(null) }
    var undoItem by remember { mutableStateOf<EventItem?>(null) }
    var severPrompt by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var themeName by remember { mutableStateOf(prefsGet("secount_theme") ?: THEMES[0].name) }
    var darkMode by remember { mutableStateOf(prefsGet("secount_darkmode") ?: "System") }
    var muted by remember { mutableStateOf(prefsGet(MUTED_KEY) == "1") }
    var langPref by remember { mutableStateOf(prefsGet(LANG_KEY) ?: "System") }
    var crashReport by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var mainTab by remember { mutableStateOf("Mine") }
    val shownSecrets = remember { mutableSetOf<String>() }

    val effLang = if (langPref == "System") systemLanguage() else langPref
    Lang.code = effLang

    fun refresh() {
        storeVer++
        try {
            if (platformDataDir().isNotEmpty()) SecountWidgetPush.refresh(store)
        } catch (ignored: Exception) {
        }
    }

    fun isMuted(): Boolean = muted

    LaunchedEffect(Unit) {
        try {
            val f = okio.FileSystem.SYSTEM
            val p = (platformDataDir() + "/crash_log.txt").toPath()
            if (f.exists(p)) {
                val txt = f.read(p) { readUtf8() }
                if (txt.isNotBlank()) crashReport = txt.take(4000)
            }
        } catch (ignored: Exception) {
        }
        try {
            val last = prefsGet(UPDATE_CHECK_KEY)?.toLongOrNull() ?: 0L
            if (nowSec() - last > 86400) {
                prefsPut(UPDATE_CHECK_KEY, nowSec().toString())
                val json = httpGetSafe("https://api.github.com/TheKIAR/Secount/releases/latest")
                if (json != null) {
                    val tag = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
                    val url = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
                    if (tag != null && url != null && isNewerVersion(APP_VERSION, tag)) {
                        updateInfo = tag to url
                    }
                }
            }
        } catch (ignored: Exception) {
        }
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
                if (res.replyReceived) {
                    val target = res.replyId?.let { store.byId(it) }
                    val title = target?.title?.takeIf { it.isNotBlank() } ?: "a secret message"
                    if (target != null) {
                        val t = LocalDate.now()
                        val syncMyId = try { pair.accountId } catch (e: Exception) { "" }
                        val canShow = if (target.isForMe(syncMyId)) target.isDueToday(t) else true
                        if (canShow) secretOf = target
                    }
                    notice = "💬 Partner replied to '$title'. Open it to read & reply."
                    if (!isMuted()) {
                        try {
                            notifySecret("Secount reply 💬", "Partner replied to '$title'. Tap to open Secount and read it.")
                        } catch (ignored: Exception) {
                        }
                    }
                }
                if (res.deleteId != null) {
                    try {
                        if (secretOf?.id == res.deleteId) secretOf = null
                        if (alarmOf?.id == res.deleteId) alarmOf = null
                        if (editing?.id == res.deleteId) editing = null
                    } catch (ignored: Exception) {
                    }
                }
                if (res.seenId != null) {
                }
                if (res.offline) notice = "Offline — will retry automatically."
            } finally {
                syncing = false
                refresh()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secTick++
            try {
                if (prefsGet(NEED_LOCK_KEY) == "1") {
                    if (PhotoLockGuard.picking) {
                        prefsPut(NEED_LOCK_KEY, "")
                    } else {
                        prefsPut(NEED_LOCK_KEY, "")
                        pin.lockOnHome()
                        unlocked = false
                    }
                }
            } catch (ignored: Exception) {
            }
            try {
                if (prefsGet("secount_need_sync") == "1") {
                    prefsPut("secount_need_sync", "")
                    doSync()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        doSync()
        while (true) {
            delay(if (pair.isPaired()) 15_000 else 8_000)
            doSync()
        }
    }

    val today = LocalDate.now()
    @Suppress("UNUSED_EXPRESSION")
    secTick
    val now = LocalDateTime.now()
    val myId = pair.accountId
    @Suppress("UNUSED_EXPRESSION")
    storeVer
    val inboxList = remember(storeVer) {
        store.items().filter { it.isForMe(myId) && it.isDueToday(today) }
            .sortedBy { it.nextOccurrence(today) }
    }
    val shown = remember(storeVer, query, filter, sort, mainTab) {
        val q = query.trim().lowercase()
        var list = if (mainTab == "Inbox") inboxList else store.sortedFor(today, myId)
        list = list.filter { e ->
            if (q.isNotEmpty() && !e.title.lowercase().contains(q) && !e.message.lowercase().contains(q)) return@filter false
            when (filter) {
                "Today" -> e.isDueToday(today)
                "Next 7 days" -> !e.nextOccurrence(today).isAfter(today.plusDays(7))
                "Featured" -> e.featured
                "With secret" -> e.hasSecret()
                "Past" -> e.nextOccurrence(today).isBefore(today)
                "To partner" -> e.forPartner
                else -> true
            }
        }
        when (sort) {
            "Name A–Z" -> list.sortedBy { it.title.lowercase() }
            "Biggest countdown" -> list.sortedByDescending { it.countdownSeconds(LocalDateTime.now()) }
            "Newest first" -> list.sortedByDescending { it.createdAt }
            else -> list.sortedBy { it.nextOccurrence(today) }
        }
    }

    LaunchedEffect(secTick) {
        if (!unlocked) return@LaunchedEffect
        if (secTick % 5 != 0) return@LaunchedEffect
        val t = LocalDate.now()
        for (e in store.items()) {
            if (e.isForMe(myId) && !e.isDueToday(t)) continue
            val mine = e.isMine(myId)
            val forMe = e.isForMe(myId)
            if (e.isDueToday(t) && !shownSecrets.contains(e.id)) {
                if (!mine && !forMe) continue
                if (mine && e.forPartner) continue
                shownSecrets.add(e.id)
                if (e.hasSecret()) secretOf = e
                else if (!isMuted() && e.soundEnabled && e.soundName != "Silent") {
                    try { alarmBeep() } catch (ignored: Exception) { }
                    alarmOf = e
                }
            }
        }
    }

    SecountTheme(themeName, darkMode) {
        Box(Modifier.fillMaxSize()) {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            fun closeDrawer() { scope.launch { try { drawerState.close() } catch (ignored: Exception) { } } }
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Text("♥ Secount", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(if (pair.isPaired()) "✉ Connected to ${pair.partnerCode()}" else "Not connected", fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                            Text(Lang.t("secCountdowns"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            NavigationDrawerItem(label = { Text(Lang.t("newCountdown")) }, selected = false, onClick = {
                                val item = EventItem(); item.date = LocalDate.now().plusDays(7); item.senderId = myId; item.forPartner = pair.isPaired(); editing = item; editIsNew = true; closeDrawer()
                            })
                            Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                            Text(Lang.t("secConnection"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            NavigationDrawerItem(label = { Text(if (syncing) Lang.t("syncing") else Lang.t("syncNow")) }, selected = false, onClick = { doSync(); closeDrawer() })
                            NavigationDrawerItem(label = { Text(if (pair.isPaired()) "✉ ${pair.partnerCode()}" else Lang.t("connectPartner")) }, selected = false, onClick = { showConnect = true; closeDrawer() })
                            NavigationDrawerItem(label = { Text(Lang.t("appPin")) }, selected = false, onClick = { showPin = true; closeDrawer() })
                            NavigationDrawerItem(label = { Text(if (muted) Lang.t("unmute") else Lang.t("mute")) }, selected = false, onClick = {
                                muted = !muted
                                try { prefsPut(MUTED_KEY, if (muted) "1" else "") } catch (ignored: Exception) { }
                                if (muted) try { alarmStop() } catch (ignored: Exception) { }
                            })
                            Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                            Text(Lang.t("secBackup"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            NavigationDrawerItem(label = { Text(Lang.t("exportBackup")) }, selected = false, onClick = { showExport = true; closeDrawer() })
                            NavigationDrawerItem(label = { Text(Lang.t("importBackup")) }, selected = false, onClick = { showImport = true; closeDrawer() })
                            Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                            Text(Lang.t("secLanguage"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            for (l in LANGS) NavigationDrawerItem(label = { Text((if (l == langPref) "● " else "○ ") + langDisplay(l)) }, selected = l == langPref, onClick = { langPref = l; try { prefsPut(LANG_KEY, l) } catch (ignored: Exception) { } })
                            Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                            Text(Lang.t("secAppearance"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            for (m in listOf("System", "Light", "Dark")) NavigationDrawerItem(label = { Text((if (m == darkMode) "● " else "○ ") + m) }, selected = m == darkMode, onClick = { darkMode = m; try { prefsPut("secount_darkmode", m) } catch (ignored: Exception) { } })
                            Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                            Text(Lang.t("secTheme"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            for (t in THEMES) NavigationDrawerItem(label = { Text((if (t.name == themeName) "● " else "○ ") + t.name) }, selected = t.name == themeName, onClick = { themeName = t.name; try { prefsPut("secount_theme", t.name) } catch (ignored: Exception) { } })
                        }
                    }
                }
            ) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(Lang.t("appTitle"), fontWeight = FontWeight.SemiBold)
                                Text("Private countdowns. Thoughtful surprises.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        navigationIcon = {
                            TextButton(onClick = { scope.launch { try { drawerState.open() } catch (ignored: Exception) { } } }) {
                                Text("☰", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 1.dp
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${store.items().size} countdowns", fontWeight = FontWeight.SemiBold)
                                Text("$todayN today • $weekN this week", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(if (pair.isPaired()) "✉ ${pair.partnerCode()}" else "Offline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { mainTab = "Mine" }, modifier = Modifier.weight(1f)) { Text((if (mainTab == "Mine") "● " else "○ ") + "♥ Mine") }
                        OutlinedButton(onClick = { mainTab = "Inbox" }, modifier = Modifier.weight(1f)) {
                            val n = inboxList.size
                            Text((if (mainTab == "Inbox") "● " else "○ ") + if (n > 0) "💌 Inbox ($n)" else "💌 Inbox")
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewMode = "List"; calDay = null }, modifier = Modifier.weight(1f)) { Text((if (viewMode == "List") "● " else "○ ") + Lang.t("viewList")) }
                        OutlinedButton(onClick = { viewMode = "Calendar" }, modifier = Modifier.weight(1f)) { Text((if (viewMode == "Calendar") "● " else "○ ") + Lang.t("viewCalendar")) }
                    }
                    OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), placeholder = { Text(Lang.t("search")) }, singleLine = true)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MappedDropDown(Lang.t("filter"), FILTERS, filter, { filter = it }, { Lang.filterLabel(it) }, Modifier.weight(1f))
                        MappedDropDown(Lang.t("sort"), SORTS, sort, { sort = it }, { Lang.sortLabel(it) }, Modifier.weight(1f))
                    }
                    if (viewMode == "Calendar") {
                        CalendarView(month = calMonth, today = today, store = store, myId = myId, selected = calDay, onMonth = { calMonth = it }, onDay = { calDay = it; refresh() })
                    }
                    undoItem?.let { u ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${Lang.t("deleted")} '${u.title}'", fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { store.addOrUpdate(u); undoItem = null; refresh() }) { Text(Lang.t("undo")) }
                            TextButton(onClick = { undoItem = null }) { Text(Lang.t("dismiss")) }
                        }
                    }
                    if (shown.isEmpty()) {
                        Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(if (mainTab == "Inbox") "💌" else "♥", fontSize = 44.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (mainTab == "Inbox") "No messages today. Partner surprises appear here on D-day, then vanish after the day ends. Yearly surprises return each year." else if (query.isNotBlank() || filter != "All") Lang.t("emptyNomatch") else Lang.t("emptyNew"), fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            if (mainTab != "Inbox") Button(onClick = { val item = EventItem(); item.date = LocalDate.now().plusDays(7); item.senderId = myId; item.forPartner = pair.isPaired(); editing = item; editIsNew = true }) { Text(Lang.t("newBtn")) }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            items(shown, key = { it.id }) { e ->
                                if (e.isForMe(myId)) SecretInboxCard(e, onOpen = { secretOf = e }) else EventCard(e, now, myId,
                                    onEdit = { editing = e.copyFromJson(); editIsNew = false },
                                    onDuplicate = { val copy = e.copyFromJson(); copy.id = UUID.randomUUID().toString().replace("-", ""); copy.title = e.title + " (copy)"; copy.createdAt = LocalDateTime.now(); copy.senderId = myId; store.addOrUpdate(copy); if (copy.forPartner) scope.launch { engine.sendCountdown(copy) }; refresh() },
                                    onRing = { if (!isMuted() && e.soundEnabled && e.soundName != "Silent") try { alarmBeep() } catch (ignored: Exception) { }; val hasThread = e.threadEntries().isNotEmpty(); if (e.hasSecret() || hasThread || e.forPartner) secretOf = e else alarmOf = e },
                                    onMessages = { secretOf = e },
                                    onDelete = { confirmDelete = e }
                                )
                            }
                        }
                    }
                    FloatingActionButton(onClick = { val item = EventItem(); item.date = LocalDate.now().plusDays(7); item.senderId = myId; item.forPartner = pair.isPaired(); editing = item; editIsNew = true }, modifier = Modifier.align(Alignment.End).padding(16.dp)) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
            }

            editing?.let { item ->
                if (item.isForMe(myId)) {
                    LaunchedEffect(item.id) { secretOf = store.byId(item.id) ?: item; editing = null }
                } else {
                    EditDialog(item, editIsNew, pair, onSave = { saved, send -> store.addOrUpdate(saved); editing = null; if (send) scope.launch { engine.sendCountdown(saved) }; refresh() }, onDelete = { deleted -> store.delete(deleted.id); editing = null; undoItem = deleted; if (deleted.forPartner) scope.launch { engine.sendDelete(deleted.id) }; refresh() }, onCancel = { editing = null })
                }
            }
            showConnect?.let { }
            showPin?.let { }
            showExport?.let { }
            showImport?.let { }
            secretOf?.let { SecretDialog(it, myId, onClose = { secretOf = null }, onChanged = { refresh() }) }
            alarmOf?.let { AlarmDialog(it, onClose = { alarmOf = null }) }
            confirmDelete?.let { e ->
                AlertDialog(onDismissRequest = { confirmDelete = null }, title = { Text(Lang.t("deleteTitle")) }, text = { Text("Delete '${e.title}'?") }, confirmButton = { TextButton(onClick = { store.delete(e.id); confirmDelete = null; undoItem = e; if (e.forPartner) scope.launch { engine.sendDelete(e.id) }; refresh() }) { Text(Lang.t("delete")) } }, dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(Lang.t("cancel")) } })
            }
            if (!unlocked) PinGate(pin, onUnlocked = { unlocked = true })
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
    onDelete: () -> Unit,
    onMessages: () -> Unit = onRing
) {
    val today = now.toLocalDate()
    val due = e.isDueToday(today)
    val accent = e.accentColor(MaterialTheme.colorScheme.primary)
    val mine = e.isMine(myId)
    val forMe = e.isForMe(myId)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(5.dp, 56.dp),
                    shape = MaterialTheme.shapes.small,
                    color = accent
                ) {}
                Spacer(Modifier.width(10.dp))
                Text(e.displayIcon(), fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text((if (e.featured) "★ " else "") + e.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    val bits = mutableListOf(e.displayCategory().uppercase(), e.shortCountdown(today).uppercase())
                    if (forMe) bits.add("🎁 FOR YOU")
                    else if (mine && e.forPartner) {
                        if (e.seenAtSec > 0) {
                            val s = try { e.receiptLabel(e.seenAtSec) } catch (ignored: Exception) { "" }
                            bits.add(if (s.isNotEmpty()) "👁 SEEN • $s" else "👁 SEEN")
                        } else if (e.delivered) {
                            val d = try { e.receiptLabel(e.deliveredAtSec) } catch (ignored: Exception) { "" }
                            bits.add(if (d.isNotEmpty()) "✉ DELIVERED • $d" else "✉ DELIVERED")
                        } else bits.add("✉ TO PARTNER")
                    }
                    if (e.hasSecret() && !forMe) bits.add("SECRET ARMED")
                    try { val rc = e.threadEntries().size; if (rc > 0) bits.add("💬 $rc ${if (rc == 1) "REPLY" else "REPLIES"})" } catch (ignored: Exception) { }
                    if (e.effectiveRepeat() != "once") bits.add(e.repeatLabel().uppercase())
                    if (e.soundName == "Silent") bits.add("MUTED")
                    Text(bits.joinToString(" • "), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = MaterialTheme.shapes.medium, color = if (due) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
                        Text(if (due) "♥ TODAY" else "♥ LIVE", color = if (due) Success else accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(e.countdownText(now), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(e.shortCountdown(today), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text("📅 ${e.dateLabel()}" + if (e.photoUri.isNotEmpty()) " • 📷" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            if (forMe && !due) Text("🎁 A surprise from your partner — the message arrives at zero.", fontSize = 13.sp)
            else Text(if (e.message.isEmpty()) "A special moment is waiting…" else e.message, fontSize = 13.sp)
            if (e.photoUri.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val bmp = remember(e.photoUri) { try { loadPhotoBitmap(e.photoUri) } catch (ignored: Exception) { null } }
                if (bmp != null) Image(bmp, contentDescription = Lang.t("photo"), modifier = Modifier.fillMaxWidth().height(160.dp))
            }
            val threadPreview = try { e.threadEntries() } catch (ignored: Exception) { emptyList() }
            if (threadPreview.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val lastPreview = threadPreview.last()
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text("💬 ${threadPreview.size} ${if (threadPreview.size == 1) "reply" else "replies"} — \"${lastPreview.third.take(60)}\" — tap MESSAGES to read & reply.", modifier = Modifier.padding(10.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { e.progress01(today) }, modifier = Modifier.fillMaxWidth(), color = accent, trackColor = accent.copy(alpha = 0.14f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text(if (forMe && !mine) "View" else "Edit") }
                if (mine) {
                    TextButton(onClick = onDuplicate) { Text("Copy") }
                    TextButton(onClick = onRing) { Text("Ring") }
                    TextButton(onClick = onMessages) { Text("Messages") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                } else if (!mine && !forMe) TextButton(onClick = onMessages) { Text("View") }
            }
        }
    }
}
