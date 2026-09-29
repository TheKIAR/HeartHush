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
import com.secount.app.logic.EventItem
import com.secount.app.logic.EventStore
import com.secount.app.logic.PairStore
import com.secount.app.logic.PhotoLockGuard
import com.secount.app.logic.PinLock
import com.secount.app.logic.SyncEngine
import com.secount.app.logic.alarmBeep
import com.secount.app.logic.alarmStop
import com.secount.app.logic.copyToClipboard
import com.secount.app.logic.deletePhotoFile
import com.secount.app.logic.getClipboardText
import com.secount.app.logic.loadPhotoBitmap
import com.secount.app.logic.notifySecret
import com.secount.app.logic.nowSec
import com.secount.app.logic.openUrl
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

    // Crash report + update check, once per launch.
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
                    // Auto-open the conversation so the reply is visible right away.
                    // Only when the item is visible to this device (sender always;
                    // receiver only at zero) — otherwise keep it for D-day.
                    if (target != null) {
                        val t = LocalDate.now()
                        // isForMe items hidden until due; sender items always visible.
                        val syncMyId = try { pair.accountId } catch (e: Exception) { "" }
                        val canShow = if (target.isForMe(syncMyId)) target.isDueToday(t) else true
                        if (canShow) {
                            secretOf = target
                        }
                    }
                    notice = "💬 Partner replied to '$title'. Open it to read & reply."
                    if (!isMuted()) {
                        try {
                            notifySecret(
                                "Secount reply 💬",
                                "Partner replied to '$title'. Tap to open Secount and read it."
                            )
                        } catch (ignored: Exception) {
                        }
                    }
                }
                if (res.offline) notice = "Offline — will retry automatically."
            } finally {
                syncing = false
                refresh()
            }
        }
    }

    // 1s ticker drives only the live timer text; list ordering uses storeVer
    // so the whole list is not resorted/recomposed every second.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secTick++
            // Lock only when the OS sent us Home/background (native sets the
            // flag). No timer auto-lock while you stay in the app.
            // While the photo picker is open the OS also backgrounds us —
            // that must NOT lock, or unsaved editor text is lost.
            try {
                if (prefsGet(NEED_LOCK_KEY) == "1") {
                    if (PhotoLockGuard.picking) {
                        // Consume the flag, stay unlocked during picking.
                        prefsPut(NEED_LOCK_KEY, "")
                    } else {
                        prefsPut(NEED_LOCK_KEY, "")
                        pin.lockOnHome()
                        unlocked = false
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }
    // online sync every 25s when linked, every 8s while waiting to pair
    LaunchedEffect(Unit) {
        doSync()
        while (true) {
            delay(if (pair.isPaired()) 25_000 else 8_000)
            doSync()
        }
    }

    // No early return here on purpose: the main UI stays composed under the
    // PIN overlay so unsaved editor text / photo state survives a lock.
    // When locked, PinGate is drawn full-screen on top at the end of this
    // composable (see bottom of SecountTheme block).
    val today = LocalDate.now()
    @Suppress("UNUSED_EXPRESSION")
    secTick
    val now = LocalDateTime.now()
    val myId = pair.accountId
    @Suppress("UNUSED_EXPRESSION")
    storeVer
    val shown = remember(storeVer, query, filter, sort) {
        val t = LocalDate.now()
        var list = store.sortedByNext(t).filter { e ->
            // Partner secrets stay hidden on the receiver until D-day: the
            // receiver cannot see, open or edit them before zero.
            if (e.isForMe(myId) && !e.isDueToday(t)) return@filter false
            if (calDay != null && viewMode == "Calendar") {
                val match = if (e.effectiveRepeat() == "once") e.date == calDay
                else e.nextOccurrence(t) == calDay || e.isDueToday(calDay!!)
                if (!match) return@filter false
            }
            (query.isBlank() || (e.title + " " + e.message + " " + e.displayCategory())
                .contains(query.trim(), ignoreCase = true)) &&
                when (filter) {
                    "Today" -> e.isDueToday(t)
                    "Next 7 days" -> !e.isPast(t) && e.daysUntil(t) <= 7
                    "Featured" -> e.featured
                    "With secret" -> e.hasSecret() || e.threadEntries().isNotEmpty()
                    "Past" -> e.isPast(t)
                    "To partner" -> e.forPartner && e.isMine(myId)
                    else -> true
                }
        }
        list = when (sort) {
            "Name A–Z" -> list.sortedBy { it.title.lowercase() }
            "Biggest countdown" -> list.sortedByDescending { it.daysUntil(t) }
            "Newest first" -> list.sortedByDescending { it.createdAt }
            else -> list
        }
        list
    }
    var todayN = 0
    var weekN = 0
    for (e in store.items()) {
        if (e.isForMe(myId) && !e.isDueToday(today)) continue
        if (e.isDueToday(today)) todayN++
        else if (!e.isPast(today) && e.daysUntil(today) <= 7) weekN++
    }

    // due-today reveals: personal items and partner-sent items open here;
    // items I sent are revealed on the partner's device instead.
    // Partner items also fire the "You Have a Secret Message" notification.
    // Plus 1-day / 7-day pre-reminders (once per day per event).
    // Never auto-open secrets while the PIN overlay is up.
    LaunchedEffect(secTick, unlocked) {
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
                if (!isMuted() && e.soundEnabled && e.soundName != "Silent") {
                    try {
                        alarmBeep()
                    } catch (ignored: Exception) {
                    }
                }
                if (forMe && !isMuted()) {
                    try {
                        notifySecret(
                            "You Have a Secret Message Open it",
                            "Open Secount to read your new secret message."
                        )
                    } catch (ignored: Exception) {
                    }
                }
                if (e.hasSecret() || forMe) secretOf = e else alarmOf = e
                if (forMe) {
                    scope.launch { engine.sendDelivered(e.id) }
                }
            } else if (!e.isPast(t) && !isMuted()) {
                val d = e.daysUntil(t)
                val keyDay = t.toString()
                if (d == 1L && e.remind1d) {
                    val k = "reminded_${e.id}_1_$keyDay"
                    if (prefsGet(k) == null) {
                        prefsPut(k, "1")
                        try {
                            notifySecret("Tomorrow: ${e.title}", "${e.shortCountdown(t)} • ${e.timeLabel()}")
                        } catch (ignored: Exception) {
                        }
                    }
                }
                if (d == 7L && e.remind7d) {
                    val k = "reminded_${e.id}_7_$keyDay"
                    if (prefsGet(k) == null) {
                        prefsPut(k, "1")
                        try {
                            notifySecret("In a week: ${e.title}", "${e.shortCountdown(t)} • ${e.dateLabel()}")
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        }
    }

    SecountTheme(themeName, darkMode) {
      Box(Modifier.fillMaxSize()) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        fun closeDrawer() {
            scope.launch { try {
                drawerState.close()
            } catch (ignored: Exception) {
            } }
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                    ) {
                        Text("♥ Secount", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            if (pair.isPaired()) "✉ Connected to ${pair.partnerCode()}"
                            else "Not connected",
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(Lang.t("secCountdowns"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        NavigationDrawerItem(
                            label = { Text(Lang.t("newCountdown")) },
                            selected = false,
                            onClick = {
                                val item = EventItem()
                                item.date = LocalDate.now().plusDays(7)
                                item.senderId = myId
                                editing = item
                                editIsNew = true
                                closeDrawer()
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text(Lang.t("addSamples")) },
                            selected = false,
                            onClick = {
                                sample(store, myId, "Birthday 🎂", 14, "Birthday", "🎂", "#FFB020", "Cake, friends and music!", true)
                                sample(store, myId, "Final Exams 🎓", 30, "Exam", "🎓", "#22C4A8", "One chapter a day keeps stress away.", true)
                                sample(store, myId, "Android App Launch 🚀", 60, "App Release", "🚀", "#7C6CFF", "Release v2.0 to the Play Store.", false)
                                sample(store, myId, "Beach Trip ✈", 90, "Trip", "✈", "#38BDF8", "Sunscreen, playlists, passports.", false)
                                sample(store, myId, "Wedding Day 💖", 120, "Wedding", "💖", "#F472B6", "The big day!", true)
                                refresh()
                                closeDrawer()
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(Lang.t("secConnection"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        NavigationDrawerItem(
                            label = { Text(if (syncing) Lang.t("syncing") else Lang.t("syncNow")) },
                            selected = false,
                            onClick = { doSync(); closeDrawer() }
                        )
                        NavigationDrawerItem(
                            label = { Text(if (pair.isPaired()) "✉ ${pair.partnerCode()}" else Lang.t("connectPartner")) },
                            selected = false,
                            onClick = { showConnect = true; closeDrawer() }
                        )
                        NavigationDrawerItem(
                            label = { Text(Lang.t("appPin")) },
                            selected = false,
                            onClick = { showPin = true; closeDrawer() }
                        )
                        NavigationDrawerItem(
                            label = { Text(if (muted) Lang.t("unmute") else Lang.t("mute")) },
                            selected = false,
                            onClick = {
                                muted = !muted
                                try {
                                    prefsPut(MUTED_KEY, if (muted) "1" else "")
                                } catch (ignored: Exception) {
                                }
                                if (muted) {
                                    try {
                                        alarmStop()
                                    } catch (ignored: Exception) {
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(Lang.t("secBackup"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        NavigationDrawerItem(
                            label = { Text(Lang.t("exportBackup")) },
                            selected = false,
                            onClick = { showExport = true; closeDrawer() }
                        )
                        NavigationDrawerItem(
                            label = { Text(Lang.t("importBackup")) },
                            selected = false,
                            onClick = { showImport = true; closeDrawer() }
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(Lang.t("secLanguage"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        for (l in LANGS) {
                            NavigationDrawerItem(
                                label = { Text((if (l == langPref) "● " else "○ ") + langDisplay(l)) },
                                selected = l == langPref,
                                onClick = {
                                    langPref = l
                                    try {
                                        prefsPut(LANG_KEY, l)
                                    } catch (ignored: Exception) {
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(Lang.t("secAppearance"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        for (m in listOf("System", "Light", "Dark")) {
                            NavigationDrawerItem(
                                label = { Text((if (m == darkMode) "● " else "○ ") + m) },
                                selected = m == darkMode,
                                onClick = {
                                    darkMode = m
                                    try {
                                        prefsPut("secount_darkmode", m)
                                    } catch (ignored: Exception) {
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(Lang.t("secTheme"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        for (t in THEMES) {
                            NavigationDrawerItem(
                                label = { Text((if (t.name == themeName) "● " else "○ ") + t.name) },
                                selected = t.name == themeName,
                                onClick = {
                                    themeName = t.name
                                    try {
                                        prefsPut("secount_theme", t.name)
                                    } catch (ignored: Exception) {
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) {
            Column(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TopAppBar(
                    title = { Text(Lang.t("appTitle")) },
                    navigationIcon = {
                        TextButton(onClick = {
                            scope.launch { try {
                                drawerState.open()
                            } catch (ignored: Exception) {
                            } }
                        }) { Text("☰", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimary) }
                    },
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
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewMode = "List"; calDay = null },
                        modifier = Modifier.weight(1f)
                    ) { Text((if (viewMode == "List") "● " else "○ ") + Lang.t("viewList")) }
                    OutlinedButton(
                        onClick = { viewMode = "Calendar" },
                        modifier = Modifier.weight(1f)
                    ) { Text((if (viewMode == "Calendar") "● " else "○ ") + Lang.t("viewCalendar")) }
                }
                OutlinedTextField(
                    query, { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    placeholder = { Text(Lang.t("search")) },
                    singleLine = true
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MappedDropDown(
                        Lang.t("filter"), FILTERS, filter, { filter = it },
                        { Lang.filterLabel(it) }, Modifier.weight(1f)
                    )
                    MappedDropDown(
                        Lang.t("sort"), SORTS, sort, { sort = it },
                        { Lang.sortLabel(it) }, Modifier.weight(1f)
                    )
                }
                if (viewMode == "Calendar") {
                    CalendarView(
                        month = calMonth,
                        today = today,
                        store = store,
                        myId = myId,
                        selected = calDay,
                        onMonth = { calMonth = it },
                        onDay = { calDay = it; refresh() }
                    )
                }
                undoItem?.let { u ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${Lang.t("deleted")} '${u.title}'", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            store.addOrUpdate(u)
                            undoItem = null
                            refresh()
                        }) { Text(Lang.t("undo")) }
                        TextButton(onClick = { undoItem = null }) { Text(Lang.t("dismiss")) }
                    }
                }
                if (shown.isEmpty()) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("♥", fontSize = 44.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (query.isNotBlank() || filter != "All") Lang.t("emptyNomatch")
                            else Lang.t("emptyNew"),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            val item = EventItem()
                            item.date = LocalDate.now().plusDays(7)
                            item.senderId = myId
                            editing = item
                            editIsNew = true
                        }) { Text(Lang.t("newBtn")) }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            sample(store, myId, "Birthday 🎂", 14, "Birthday", "🎂", "#FFB020", "Cake, friends and music!", true)
                            sample(store, myId, "Final Exams 🎓", 30, "Exam", "🎓", "#22C4A8", "One chapter a day keeps stress away.", true)
                            sample(store, myId, "Android App Launch 🚀", 60, "App Release", "🚀", "#7C6CFF", "Release v2.0 to the Play Store.", false)
                            sample(store, myId, "Beach Trip ✈", 90, "Trip", "✈", "#38BDF8", "Sunscreen, playlists, passports.", false)
                            sample(store, myId, "Wedding Day 💖", 120, "Wedding", "💖", "#F472B6", "The big day!", true)
                            refresh()
                        }) { Text(Lang.t("samplesBtn")) }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        items(shown, key = { it.id }) { e ->
                            if (e.isForMe(myId)) {
                                // Receiver view: sealed card only, no details, no edit.
                                // The message + reply open through OPEN MESSAGE.
                                SecretInboxCard(
                                    e,
                                    onOpen = { secretOf = e }
                                )
                            } else {
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
                                        if (!isMuted() && e.soundEnabled && e.soundName != "Silent") {
                                            try {
                                                alarmBeep()
                                            } catch (ignored: Exception) {
                                            }
                                        }
                                        // Open the message view when there is anything
                                        // conversation-like: secret, replies, or a shared
                                        // partner countdown. Otherwise plain alarm.
                                        val hasThread = e.threadEntries().isNotEmpty()
                                        if (e.hasSecret() || hasThread || e.forPartner) secretOf = e else alarmOf = e
                                    },
                                    onMessages = { secretOf = e },
                                    onDelete = { confirmDelete = e }
                                )
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        val item = EventItem()
                        item.date = LocalDate.now().plusDays(7)
                        item.senderId = myId
                        editing = item
                        editIsNew = true
                    },
                    modifier = Modifier.align(Alignment.End).padding(16.dp)
                ) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
        }

        editing?.let { item ->
            // Safety net: partner-sent items are never editable, even if an
            // old code path tries to open the editor for them.
            if (item.isForMe(myId)) {
                LaunchedEffect(item.id) {
                    secretOf = store.byId(item.id) ?: item
                    editing = null
                }
            } else {
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
            PinDialog(pin, onClose = { showPin = false; unlocked = pin.isUnlocked(); refresh() })
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
                    }) { Text(Lang.t("agree")) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        severPrompt = false
                        scope.launch {
                            engine.declineSever()
                            refresh()
                        }
                    }) { Text(Lang.t("keepConn")) }
                }
            )
        }
        notice?.let { msg ->
            AlertDialog(
                onDismissRequest = { notice = null },
                title = { Text("Secount") },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { notice = null }) { Text(Lang.t("ok")) }
                }
            )
        }
        crashReport?.let { report ->
            AlertDialog(
                onDismissRequest = { },
                title = { Text(Lang.t("crashTitle")) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(report.take(1200), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            copyToClipboard(report)
                        } catch (ignored: Exception) {
                        }
                        clearCrashLog()
                        crashReport = null
                    }) { Text(Lang.t("copyReport")) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        clearCrashLog()
                        crashReport = null
                    }) { Text(Lang.t("discard")) }
                }
            )
        }
        updateInfo?.let { (tag, url) ->
            AlertDialog(
                onDismissRequest = { updateInfo = null },
                title = { Text(Lang.t("updateTitle") + " ($tag)") },
                text = { Text(RELEASES_URL, fontSize = 12.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            openUrl(url)
                        } catch (ignored: Exception) {
                        }
                        updateInfo = null
                    }) { Text(Lang.t("download")) }
                },
                dismissButton = {
                    TextButton(onClick = { updateInfo = null }) { Text(Lang.t("updateLater")) }
                }
            )
        }
        if (showExport) {
            val json = remember(showExport, storeVer) { store.exportJson() }
            AlertDialog(
                onDismissRequest = { showExport = false },
                title = { Text(Lang.t("exportBackup")) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(json, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            copyToClipboard(json)
                        } catch (ignored: Exception) {
                        }
                        showExport = false
                    }) { Text(Lang.t("copy")) }
                },
                dismissButton = {
                    TextButton(onClick = { showExport = false }) { Text(Lang.t("close")) }
                }
            )
        }
        if (showImport) {
            var pasted by remember { mutableStateOf("") }
            var imported by remember { mutableStateOf<Int?>(null) }
            AlertDialog(
                onDismissRequest = { showImport = false; refresh() },
                title = { Text("Import backup") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("Paste a backup JSON array exported from Secount.", fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(pasted, { pasted = it; imported = null }, label = { Text("Backup JSON") })
                        if (imported != null) Text("Imported $imported countdown(s).", fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        imported = store.importJson(pasted)
                        refresh()
                    }) { Text(Lang.t("importBackup")) }
                },
                dismissButton = {
                    TextButton(onClick = { showImport = false; refresh() }) { Text(Lang.t("close")) }
                }
            )
        }
        secretOf?.let { item ->
            val live = store.byId(item.id) ?: item
            val initialThread = (store.byId(item.id)?.threadEntries() ?: item.threadEntries())
            // If a conversation already exists, show it immediately — no extra OPEN tap
            // needed to discover the partner's reply.
            var opened by remember(item.id) { mutableStateOf(initialThread.isNotEmpty()) }
            var reply by remember(item.id) { mutableStateOf("") }
            var sending by remember(item.id) { mutableStateOf(false) }
            val forMe = live.isForMe(myId)
            AlertDialog(
                onDismissRequest = { secretOf = null },
                title = {
                    Text(
                        if (forMe) "🎁 ${live.title.ifEmpty { "You have a secret message" }}"
                        else "💌 ${live.title.ifEmpty { "You have a message" }}"
                    )
                },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("${live.displayCategory()} • ${live.dateLabel()}", fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        if (!opened) {
                            Text(
                                if (forMe) "Your partner sent you a surprise. It arrived at zero — open it when you're ready."
                                else "The countdown reached zero. Open your message when you're ready."
                            )
                            Spacer(Modifier.height(8.dp))
                            val pendingThread = (store.byId(live.id)?.threadEntries() ?: live.threadEntries())
                            if (pendingThread.isNotEmpty()) {
                                Text("💬 ${pendingThread.size} repl${if (pendingThread.size == 1) "y" else "ies"} — open to read.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                        } else {
                            // Normal message section (always shown, labelled).
                            if (live.message.isNotBlank()) {
                                Text("✉ Message:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(live.message)
                                Spacer(Modifier.height(8.dp))
                            }
                            // Secret section — shown even if secretEnabled flag is off,
                            // as long as text exists (prevents "only normal visible" bug).
                            if (live.secretMessage.isNotBlank()) {
                                Text("🎁 Secret message:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(live.secretMessage)
                                Spacer(Modifier.height(8.dp))
                            }
                            if (live.message.isBlank() && live.secretMessage.isBlank()) {
                                Text("The day has arrived! ♥")
                                Spacer(Modifier.height(8.dp))
                            }
                            if (live.photoUri.isNotEmpty()) {
                                val bmp = remember(live.photoUri) {
                                    try {
                                        loadPhotoBitmap(live.photoUri)
                                    } catch (ignored: Exception) {
                                        null
                                    }
                                }
                                if (bmp != null) {
                                    Image(
                                        bmp, contentDescription = Lang.t("photo"),
                                        modifier = Modifier.fillMaxWidth().height(160.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            val thread = (store.byId(live.id)?.threadEntries() ?: live.threadEntries())
                            if (thread.isNotEmpty()) {
                                Text("💬 Conversation (${thread.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                for ((ts, who, text) in thread) {
                                    val whenS = if (ts > 0) {
                                        try {
                                            java.time.Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDateTime().toString().take(16).replace("T", " ")
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    } else ""
                                    // Normalize: own messages -> You, partner's -> Partner.
                                    val whoLabel = when (who) {
                                        "me", "sender" -> "You"
                                        "partner" -> "Partner"
                                        "" -> if (forMe) "Partner" else "You"
                                        else -> who
                                    }
                                    Text(
                                        "$whoLabel: $text" + (if (whenS.isNotEmpty()) "  ($whenS)" else ""),
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                            if (pair.isPaired() && (forMe || live.forPartner)) {
                                OutlinedTextField(
                                    reply, { reply = it },
                                    label = { Text(if (thread.isEmpty()) "Write a reply…" else "Reply…") }
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        val text = reply.trim()
                                        if (text.isEmpty() || sending) return@Button
                                        sending = true
                                        scope.launch {
                                            try {
                                                val cur = store.byId(live.id)
                                                if (cur != null) {
                                                    cur.appendReply(if (forMe) "me" else "sender", text, nowSec())
                                                    store.addOrUpdate(cur)
                                                }
                                                engine.sendReply(live.id, text)
                                                reply = ""
                                                refresh()
                                            } finally {
                                                sending = false
                                            }
                                        }
                                    },
                                    enabled = reply.trim().isNotEmpty() && !sending
                                ) { Text(if (sending) "SENDING…" else "SEND REPLY") }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { opened = true }, enabled = !opened) {
                        Text(if (opened) Lang.t("opened") else Lang.t("openMsg"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { secretOf = null }) { Text(Lang.t("close")) }
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
                    TextButton(onClick = { alarmStop(); alarmOf = null }) { Text(Lang.t("stop")) }
                },
                dismissButton = {
                    TextButton(onClick = { alarmStop(); alarmOf = null }) { Text(Lang.t("snooze")) }
                }
            )
        }
        confirmDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text(Lang.t("delete")) },
                text = { Text("Delete '${item.title}'?" + if (item.forPartner) "\n(This removes it on this device only.)" else "") },
                confirmButton = {
                    TextButton(onClick = {
                        undoItem = item.copyFromJson()
                        if (item.photoUri.isNotEmpty()) {
                            try {
                                deletePhotoFile(item.photoUri)
                            } catch (ignored: Exception) {
                            }
                        }
                        store.delete(item.id)
                        confirmDelete = null
                        refresh()
                    }) { Text(Lang.t("yes")) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text(Lang.t("no")) }
                }
            )
        }
        // PIN overlay on top: keeps editor/dialog state composed underneath,
        // so a lock never clears unsaved text or photo choice.
        if (!unlocked) {
            PinGate(pin, themeName, darkMode, onUnlock = { unlocked = pin.isUnlocked(); refresh() })
        }
      }
    }
}

@Composable
private fun PinGate(pin: PinLock, themeName: String, darkMode: String, onUnlock: () -> Unit) {
    var entry by remember { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }
    var tickLock by remember { mutableStateOf(0) }
    fun tryUnlock() {
        if (!pin.canAttempt()) {
            denied = true
            return
        }
        if (pin.unlock(entry)) {
            entry = ""
            denied = false
            onUnlock()
        } else denied = true
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tickLock++
        }
    }
    @Suppress("UNUSED_EXPRESSION")
    tickLock
    val lockedSecs = pin.lockoutRemainingSec()
    SecountTheme(themeName, darkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("♥", fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("Secount", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (pin.isDefaultPin()) Lang.t("firstPin")
                    else Lang.t("enterPin"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    entry, { entry = it.filter { c -> c.isDigit() }.take(8); denied = false },
                    placeholder = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { tryUnlock() }),
                    enabled = lockedSecs <= 0
                )
                if (lockedSecs > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("${Lang.t("waitLock")} ${lockedSecs}s.", color = MaterialTheme.colorScheme.error)
                } else {
                    if (denied) {
                        val left = pin.attemptsLeft()
                        Text(
                            if (left > 0) "${Lang.t("wrongPin")} $left"
                            else Lang.t("wrongPin"),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { tryUnlock() }, enabled = lockedSecs <= 0) { Text(Lang.t("unlock")) }
            }
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

    // Auto-sync every 5s while the dialog is open so the second device
    // pairs without having to press SYNC. Stops once linked (background
    // 25s loop takes over), but keeps watching for incoming requests.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            try {
                val res = engine.syncNow()
                reload()
                if (res.justPaired) onNotice("Connected! You can now send countdowns to each other.")
                if (res.severAsked) onSeverPrompt()
            } catch (ignored: Exception) {
            }
        }
    }

    var showQr by remember { mutableStateOf(false) }
    var copiedTick by remember { mutableStateOf(0) }
    val myPairText = remember(pair.myCode, pair.accountId) { pairingText(pair.myCode, pair.accountId) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(Lang.t("connTitle")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(Lang.t("yourCode"), fontWeight = FontWeight.Bold)
                Text(pair.myCode, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text(
                    Lang.t("connHint"),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        try {
                            copyToClipboard(myPairText)
                        } catch (ignored: Exception) {
                        }
                        copiedTick++
                    }) { Text(Lang.t("copy")) }
                    OutlinedButton(onClick = { showQr = !showQr }) {
                        Text(if (showQr) Lang.t("hideQr") else Lang.t("showQr"))
                    }
                }
                if (copiedTick > 0) Text(Lang.t("copied"), fontSize = 12.sp, color = Success)
                if (showQr) {
                    Spacer(Modifier.height(6.dp))
                    QrCode(myPairText)
                    Spacer(Modifier.height(4.dp))
                    Text(myPairText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(2.dp))
                    Text(Lang.t("scanHint"), fontSize = 12.sp)
                }
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
                        code, {
                            // Accept pasted SECOUNT1:... payloads or plain codes.
                            val parsed = parsePairCode(it)
                            code = if (parsed != null && it.contains(":")) parsed
                            else it.uppercase().filter { c -> c.isLetterOrDigit() || c == ':' }.take(32)
                            err = null
                        },
                        label = { Text(Lang.t("pairText")) },
                        placeholder = { Text(Lang.t("partnerCode")) },
                        singleLine = true
                    )
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val parsed = parsePairCode(code)
                            if (parsed == null) {
                                err = Lang.t("partnerCode")
                                return@Button
                            }
                            if (parsed == pair.myCode) {
                                err = Lang.t("partnerCode")
                                return@Button
                            }
                            code = parsed
                            busy = true
                            scope.launch {
                                val ok = engine.sendPairRequest(parsed)
                                busy = false
                                reload()
                                onNotice(
                                    if (ok) "Request sent to $parsed. Ask them to enter YOUR code (${pair.myCode}) to complete."
                                    else "Offline — couldn't send. Try Sync later."
                                )
                            }
                        }) { Text(if (busy) "…" else Lang.t("sendReq")) }
                        OutlinedButton(onClick = {
                            try {
                                val clip = getClipboardText() ?: ""
                                val parsed = parsePairCode(clip)
                                if (parsed != null) {
                                    code = parsed
                                    err = null
                                } else {
                                    code = clip.uppercase().take(32)
                                }
                            } catch (ignored: Exception) {
                            }
                        }) { Text(Lang.t("paste")) }
                    }
                    if (pending.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Waiting on $pending… auto-retrying every few seconds. Keep this open.", fontSize = 12.sp)
                    }
                    if (incoming.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Wants to connect:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        for (req in incoming) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(req.code, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    code = req.code
                                }) { Text("→") }
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
            }) { Text(Lang.t("syncClose")) }
        }
    )
}

/** QR code rendered with zxing + Canvas (no camera permission needed). */
@Composable
private fun QrCode(content: String) {
    val matrix = remember(content) {
        try {
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 33, 33)
        } catch (e: Exception) {
            null
        }
    }
    if (matrix == null) {
        Text(content, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        return
    }
    val n = matrix.width
    Canvas(Modifier.size(220.dp).background(Color.White).padding(8.dp)) {
        val cell = size.minDimension / n
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (matrix.get(x, y)) {
                    drawRect(
                        Color.Black,
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell + 0.5f, cell + 0.5f)
                    )
                }
            }
        }
    }
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
        title = { Text(Lang.t("pinTitle")) },
        text = {
            Column {
                Text(Lang.t("pinFirst"), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(cur, { cur = it.filter { c -> c.isDigit() }.take(8) }, label = { Text(Lang.t("curPin")) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(next, { next = it.filter { c -> c.isDigit() }.take(8) }, label = { Text(Lang.t("newPin")) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(confirm, { confirm = it.filter { c -> c.isDigit() }.take(8) }, label = { Text(Lang.t("confirmPin")) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (done) Text(Lang.t("ok"), color = Success, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (next != confirm) {
                    err = Lang.t("confirmPin")
                    return@TextButton
                }
                if (pin.changePin(cur, next)) {
                    err = null
                    done = true
                    cur = ""
                    next = ""
                    confirm = ""
                } else err = Lang.t("wrongPin")
            }) { Text(Lang.t("change")) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { pin.lock(); onClose() }) { Text(Lang.t("lockNow")) }
                TextButton(onClick = onClose) { Text(Lang.t("close")) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MappedDropDown(
    label: String,
    ids: List<String>,
    selectedId: String,
    onSelectId: (String) -> Unit,
    display: (String) -> String,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }, modifier = modifier) {
        OutlinedTextField(
            display(selectedId), {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(open, { open = false }) {
            for (id in ids) {
                DropdownMenuItem(
                    { Text(display(id)) },
                    onClick = { onSelectId(id); open = false }
                )
            }
        }
    }
}

@Composable
private fun SecretInboxCard(
    e: EventItem,
    onOpen: () -> Unit
) {
    val accent = e.accentColor(Brand)
    val replies = try { e.threadEntries().size } catch (ignored: Exception) { 0 }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎁", fontSize = 30.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (e.title.isNotBlank()) e.title else "You have a new secret message — open it",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "🎁 FOR YOU • ${e.dateLabel()}".uppercase(),
                        color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Your partner's surprise arrived at zero. Nothing was visible before today.", fontSize = 13.sp)
            if (replies > 0) {
                Spacer(Modifier.height(4.dp))
                Text("💬 $replies repl${if (replies == 1) "y" else "ies"} — open to read & reply.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onOpen) { Text(Lang.t("openMsg")) }
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
    onDelete: () -> Unit,
    onMessages: () -> Unit = onRing
) {
    val today = now.toLocalDate()
    val due = e.isDueToday(today)
    val accent = e.accentColor(MaterialTheme.colorScheme.primary)
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
                    try {
                        val rc = e.threadEntries().size
                        if (rc > 0) bits.add("💬 $rc ${if (rc == 1) "REPLY" else "REPLIES"}")
                    } catch (ignored: Exception) {
                    }
                    if (e.effectiveRepeat() != "once") bits.add(e.repeatLabel().uppercase())
                    if (e.soundName == "Silent") bits.add("MUTED")
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
            Text("📅 ${e.dateLabel()}" + if (e.photoUri.isNotEmpty()) " • 📷" else "", fontSize = 12.sp)
            if (forMe && !due) {
                Text("🎁 A surprise from your partner — the message arrives at zero.", fontSize = 13.sp)
            } else {
                val body = if (e.message.isEmpty()) "A special moment is waiting…" else e.message
                Text(body, fontSize = 13.sp)
            }
            if (e.photoUri.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                val bmp = remember(e.photoUri) {
                    try {
                        loadPhotoBitmap(e.photoUri)
                    } catch (ignored: Exception) {
                        null
                    }
                }
                if (bmp != null) {
                    Image(
                        bmp, contentDescription = Lang.t("photo"),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            }
            val threadPreview = try { e.threadEntries() } catch (ignored: Exception) { emptyList() }
            if (threadPreview.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                val lastPreview = threadPreview.last()
                Text(
                    "💬 ${threadPreview.size} ${if (threadPreview.size == 1) "reply" else "replies"} — \"${lastPreview.third.take(60)}\" — tap MESSAGES to read & reply.",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
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
                    // Separate entry to the full message + conversation view
                    // (secret + replies). This is how a sender sees a reply.
                    TextButton(onClick = onMessages) { Text("Messages") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                } else if (!mine && !forMe) {
                    TextButton(onClick = onMessages) { Text("View") }
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
    var hourS by remember { mutableStateOf(initial.hour.coerceIn(0, 23).toString()) }
    var minS by remember { mutableStateOf(initial.minute.coerceIn(0, 59).toString().padStart(2, '0')) }
    var repeatSel by remember {
        mutableStateOf(
            when (initial.effectiveRepeat()) {
                "yearly" -> "Yearly"
                "monthly" -> "Monthly"
                "weekly" -> "Weekly"
                else -> "One-time"
            }
        )
    }
    var featured by remember { mutableStateOf(initial.featured) }
    var sound by remember { mutableStateOf(initial.soundEnabled) }
    var soundSel by remember { mutableStateOf(if (initial.soundName in SOUNDS) initial.soundName else "Chime") }
    var remind1 by remember { mutableStateOf(initial.remind1d) }
    var remind7 by remember { mutableStateOf(initial.remind7d) }
    var secretOn by remember { mutableStateOf(initial.secretEnabled) }
    var message by remember { mutableStateOf(initial.message) }
    var secretMsg by remember { mutableStateOf(initial.secretMessage) }
    var accentIdx by remember {
        mutableStateOf(maxOf(0, ACCENTS.indexOfFirst { it.second == initial.accentHex }))
    }
    var customHex by remember {
        mutableStateOf(
            if (ACCENTS.none { it.second == initial.accentHex } && initial.accentHex.isNotEmpty()) initial.accentHex else ""
        )
    }
    var audience by remember {
        mutableStateOf(if (initial.forPartner && partnerOpt != null) partnerOpt else "Just me")
    }
    var showDate by remember { mutableStateOf(false) }
    var titleErr by remember { mutableStateOf(false) }
    var timeErr by remember { mutableStateOf<String?>(null) }
    var photo by remember { mutableStateOf(initial.photoUri) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (isNew) Lang.t("dlgCreate") else Lang.t("dlgEdit")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    title, { title = it; titleErr = false },
                    label = { Text(Lang.t("title")) },
                    singleLine = true,
                    isError = titleErr
                )
                Spacer(Modifier.height(6.dp))
                if (audiences.size > 1) {
                    DropDown(Lang.t("sendTo"), audiences, audience, { audience = it })
                    Spacer(Modifier.height(6.dp))
                }
                DropDown(Lang.t("category"), EventItem.CATEGORY_PRESETS, category, { category = it })
                Spacer(Modifier.height(6.dp))
                DropDown(Lang.t("icon"), EventItem.ICON_PRESETS, icon, { icon = it })
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${Lang.t("targetDate")}: $date • ${hourS.padStart(2, '0')}:${minS.padStart(2, '0')}")
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
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        hourS, { hourS = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text(Lang.t("hour")) }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        minS, { minS = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text(Lang.t("min")) }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                if (timeErr != null) Text(timeErr!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                MappedDropDown(Lang.t("repeat"), REPEATS, repeatSel, { repeatSel = it }, { Lang.repeatDisplay(it) })
                Spacer(Modifier.height(6.dp))
                CheckRow(Lang.t("featured"), featured) { featured = it }
                CheckRow(Lang.t("soundAlert"), sound) { sound = it }
                MappedDropDown(Lang.t("soundStyle"), SOUNDS, soundSel, { soundSel = it }, { Lang.soundDisplay(it) })
                CheckRow(Lang.t("remind1"), remind1) { remind1 = it }
                CheckRow(Lang.t("remind7"), remind7) { remind7 = it }
                CheckRow(Lang.t("secretAtZero"), secretOn) { secretOn = it }
                DropDown(Lang.t("accent"), ACCENTS.map { it.first }, ACCENTS[accentIdx].first, {
                    accentIdx = ACCENTS.indexOfFirst { a -> a.first == it }
                    if (ACCENTS[accentIdx].second.isNotEmpty()) customHex = ""
                })
                OutlinedTextField(
                    customHex, { customHex = it.take(7) },
                    label = { Text(Lang.t("customHex")) }, singleLine = true
                )
                OutlinedTextField(message, { message = it }, label = { Text(Lang.t("message")) })
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    secretMsg,
                    {
                        secretMsg = it
                        // Typing a secret auto-arms it — prevents the
                        // "typed secret but forgot the toggle, only normal shows" bug.
                        if (it.trim().isNotEmpty() && !secretOn) secretOn = true
                    },
                    label = { Text(Lang.t("secretMsg")) }
                )
                Spacer(Modifier.height(6.dp))
                Text(Lang.t("photo"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (photo.isNotEmpty()) {
                    val prev = remember(photo) {
                        try {
                            loadPhotoBitmap(photo)
                        } catch (ignored: Exception) {
                            null
                        }
                    }
                    if (prev != null) {
                        Image(prev, contentDescription = Lang.t("photo"), modifier = Modifier.fillMaxWidth().height(140.dp))
                        Spacer(Modifier.height(4.dp))
                    } else {
                        Text(photo, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                deletePhotoFile(photo)
                            } catch (ignored: Exception) {
                            }
                            photo = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(Lang.t("removePhoto")) }
                } else {
                    OutlinedButton(
                        onClick = {
                            if (picking) return@OutlinedButton
                            picking = true
                            // Suppress Home-lock while the picker backgrounds us.
                            PhotoLockGuard.picking = true
                            try {
                                pickPhotoFile { name ->
                                    if (name != null) photo = name
                                    picking = false
                                    PhotoLockGuard.picking = false
                                }
                            } catch (ignored: Exception) {
                                picking = false
                                PhotoLockGuard.picking = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (picking) "…" else Lang.t("attachPhoto")) }
                }
                if (titleErr) Text(Lang.t("title"), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.trim().isEmpty()) {
                    titleErr = true
                    return@TextButton
                }
                val h = hourS.toIntOrNull()
                val m = minS.toIntOrNull()
                if (h == null || h !in 0..23 || m == null || m !in 0..59) {
                    timeErr = "Hour 0–23, minute 0–59."
                    return@TextButton
                }
                var hex = customHex.trim()
                if (hex.isNotEmpty()) {
                    if (!hex.startsWith("#")) hex = "#$hex"
                    if (!Regex("^#[0-9a-fA-F]{6}$").matches(hex)) {
                        timeErr = "Custom color must be #RRGGBB."
                        return@TextButton
                    }
                }
                val item = initial.copyFromJson()
                item.title = title.trim()
                item.date = date
                item.hour = h
                item.minute = m
                item.category = category.trim().ifEmpty { "Countdown" }
                item.icon = icon.trim().ifEmpty { "📅" }
                item.accentHex = if (hex.isNotEmpty()) hex.uppercase() else ACCENTS[accentIdx].second
                item.setRepeat(
                    when (repeatSel) {
                        "Yearly" -> "yearly"
                        "Monthly" -> "monthly"
                        "Weekly" -> "weekly"
                        else -> "once"
                    }
                )
                item.featured = featured
                item.soundEnabled = sound
                item.soundName = soundSel
                item.remind1d = remind1
                item.remind7d = remind7
                // Auto-arm secret when text exists (toggle forgotten case).
                val effectiveSecretOn = secretOn || secretMsg.trim().isNotEmpty()
                item.secretEnabled = effectiveSecretOn
                item.message = message.trim()
                item.secretMessage = secretMsg.trim()
                if (!item.secretEnabled) item.secretMessage = ""
                if (item.photoUri != photo && item.photoUri.isNotEmpty() && photo.isEmpty()) {
                    try {
                        deletePhotoFile(item.photoUri)
                    } catch (ignored: Exception) {
                    }
                }
                item.photoUri = photo
                if (item.senderId.isEmpty()) item.senderId = pair.accountId
                val send = audience != "Just me" && pair.isPaired()
                item.forPartner = send
                if (!send) item.delivered = false
                onSave(item, send)
            }) { Text(Lang.t("save")) }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = { onDelete(initial) }) { Text(Lang.t("delete")) }
                TextButton(onClick = onCancel) { Text(Lang.t("cancel")) }
            }
        }
    )
}

@Composable
private fun CalendarView(
    month: LocalDate,
    today: LocalDate,
    store: EventStore,
    myId: String,
    selected: LocalDate?,
    onMonth: (LocalDate) -> Unit,
    onDay: (LocalDate?) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onMonth(month.minusMonths(1)); onDay(null) }) { Text("‹") }
            Text(
                month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${month.year}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onMonth(LocalDate.now().withDayOfMonth(1)) }) { Text("Today") }
            TextButton(onClick = { onMonth(month.plusMonths(1)); onDay(null) }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            for (d in listOf("M", "T", "W", "T", "F", "S", "S")) {
                Text(d, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        val first = month.withDayOfMonth(1)
        // Monday-first offset
        val offset = (first.dayOfWeek.value - 1) % 7
        val len = month.lengthOfMonth()
        var day = 1 - offset
        repeat(6) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) {
                    if (day in 1..len) {
                        val d = LocalDate.of(month.year, month.month, day)
                        var n = 0
                        for (e in store.items()) {
                            if (e.isForMe(myId) && !e.isDueToday(today)) continue
                            if (e.effectiveRepeat() == "once") {
                                if (e.date == d) n++
                            } else if (e.nextOccurrence(today) == d || e.isDueToday(d)) n++
                        }
                        val sel = selected == d
                        OutlinedButton(
                            onClick = { onDay(if (sel) null else d) },
                            modifier = Modifier.weight(1f).padding(1.dp)
                        ) {
                            Text(
                                if (n > 0) "$day•$n" else "$day",
                                fontSize = 11.sp,
                                fontWeight = if (d == today || sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (d == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    day++
                }
            }
            if (day > len) return@repeat
        }
        if (selected != null) {
            Text("Showing $selected — tap again to clear.", fontSize = 12.sp)
        }
    }
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
    e.hour = 9
    e.minute = 0
    e.category = category
    e.icon = icon
    e.accentHex = accent
    e.message = msg
    e.setRepeat(if (yearly) "yearly" else "once")
    e.soundEnabled = true
    e.soundName = "Chime"
    e.remind1d = true
    e.senderId = myId
    e.forPartner = false
    store.addOrUpdate(e)
}
