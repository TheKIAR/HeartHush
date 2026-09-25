package countdown

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MainFrame(private val store: EventStore, private val auth: AuthService) :
    JFrame("HeartHush • Countdown Studio - any moment, any app") {

    private val fired: Preferences = Preferences.userNodeForPackage(MainFrame::class.java)
    private val clockLabel = JLabel()
    private val statusLabel = JLabel()
    private val statsLabel = JLabel()
    private val authButton = JButton()
    private val addButton = JButton("+ NEW")
    private val testSoundButton = JButton("TEST ALERT")
    private val passwordButton = JButton("PASSWORD")
    private val sampleButton = JButton("+ SAMPLES")
    private val listPanel = JPanel()
    private val rows = mutableListOf<Row>()
    private val secretShownThisSession = mutableSetOf<String>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "alarm-check").apply { isDaemon = true }
    }

    private val searchField: JTextField = FuturisticUI.searchField("Search countdowns...")
    private val filterBox = JComboBox(
        arrayOf(
            "All", "Today — day is here", "Next 7 days", "Next 30 days",
            "Past (one-time)", "Featured ★", "With secret message"
        )
    )
    private val sortBox = JComboBox(
        arrayOf(
            "Sort: happening next", "Sort: name A–Z", "Sort: newest first", "Sort: biggest countdown"
        )
    )
    private val categoryBox = JComboBox<String>()
    private val themeBox = JComboBox(FuturisticUI.THEMES)
    private var root: FuturisticUI.GridBackground

    private class Row {
        lateinit var item: EventItem
        lateinit var countdown: JLabel
        lateinit var badge: JLabel
        lateinit var sub: JLabel
        lateinit var pct: JLabel
        lateinit var adminBar: JPanel
        lateinit var progress: javax.swing.JComponent
        lateinit var progressHolder: JPanel
    }

    private var suppressRefresh = false

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1020, 860)
        minimumSize = Dimension(760, 700)
        setLocationRelativeTo(null)
        FuturisticUI.frame(this)

        root = FuturisticUI.GridBackground()
        root.layout = BorderLayout()
        contentPane = root

        // ---------------- header : brand + clock
        val header = JPanel(BorderLayout(15, 0))
        header.isOpaque = false
        header.border = BorderFactory.createEmptyBorder(20, 26, 8, 26)
        val brand = JPanel()
        brand.isOpaque = false
        brand.layout = BoxLayout(brand, BoxLayout.Y_AXIS)
        val title = FuturisticUI.label("HEARTHUSH", 30f, FuturisticUI.theme().text, Font.BOLD)
        val sub = FuturisticUI.label(
            "COUNTDOWN STUDIO  •  BIRTHDAYS • EXAMS • APP LAUNCHES • ANY MOMENT",
            11f, FuturisticUI.theme().accent, Font.BOLD
        )
        brand.add(title)
        brand.add(Box.createVerticalStrut(4))
        brand.add(sub)
        header.add(brand, BorderLayout.WEST)

        val right = JPanel(BorderLayout(0, 4))
        right.isOpaque = false
        clockLabel.horizontalAlignment = JLabel.RIGHT
        clockLabel.font = clockLabel.font.deriveFont(Font.BOLD, 17f)
        clockLabel.foreground = FuturisticUI.theme().gold
        statusLabel.horizontalAlignment = JLabel.RIGHT
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD, 11f)
        statsLabel.horizontalAlignment = JLabel.RIGHT
        statsLabel.font = statsLabel.font.deriveFont(Font.PLAIN, 11f)
        statsLabel.foreground = FuturisticUI.theme().muted
        right.add(clockLabel, BorderLayout.NORTH)
        right.add(statusLabel, BorderLayout.CENTER)
        right.add(statsLabel, BorderLayout.SOUTH)
        header.add(right, BorderLayout.EAST)

        // ---------------- toolbar : search / filter / sort / category / theme
        val toolbar = JPanel()
        toolbar.isOpaque = false
        toolbar.layout = FlowLayout(FlowLayout.LEFT, 8, 6)
        toolbar.border = BorderFactory.createEmptyBorder(2, 20, 2, 20)

        searchField.preferredSize = Dimension(210, 34)
        searchField.addActionListener { refresh() }
        searchField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                if (searchField.text == "Search countdowns...") searchField.text = ""
                searchField.foreground = FuturisticUI.theme().text
            }
        })
        // live search
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                refresh()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                refresh()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                refresh()
            }
        })

        themeBox.selectedItem = FuturisticUI.theme()
        FuturisticUI.styleCombo(filterBox)
        FuturisticUI.styleCombo(sortBox)
        FuturisticUI.styleCombo(categoryBox)
        FuturisticUI.styleCombo(themeBox)
        rebuildCategoryBox()

        themeBox.addActionListener {
            val t = themeBox.selectedItem as? FuturisticUI.Theme
            if (t != null) {
                FuturisticUI.setTheme(t)
                rebuildChrome()
                refresh()
            }
        }
        filterBox.addActionListener { refresh() }
        sortBox.addActionListener { refresh() }
        categoryBox.addActionListener { refresh() }

        toolbar.add(searchField)
        toolbar.add(filterBox)
        toolbar.add(sortBox)
        toolbar.add(categoryBox)
        val themeLabel = FuturisticUI.label("Theme:", 12f, FuturisticUI.theme().muted, Font.BOLD)
        toolbar.add(themeLabel)
        toolbar.add(themeBox)

        val topWrap = JPanel(BorderLayout())
        topWrap.isOpaque = false
        topWrap.add(header, BorderLayout.NORTH)
        topWrap.add(toolbar, BorderLayout.SOUTH)
        root.add(topWrap, BorderLayout.NORTH)

        listPanel.isOpaque = false
        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)
        val scroll = JScrollPane(listPanel)
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.border = BorderFactory.createEmptyBorder(6, 24, 6, 24)
        scroll.verticalScrollBar.unitIncrement = 18
        root.add(scroll, BorderLayout.CENTER)

        val bottom = JPanel(FlowLayout(FlowLayout.LEFT, 8, 10))
        bottom.isOpaque = false
        FuturisticUI.button(addButton, FuturisticUI.theme().accent)
        FuturisticUI.ghostButton(testSoundButton)
        FuturisticUI.ghostButton(passwordButton)
        FuturisticUI.ghostButton(sampleButton)
        val refreshBtn = JButton("REFRESH")
        FuturisticUI.ghostButton(refreshBtn)
        authButton.addActionListener { onAuth() }
        addButton.addActionListener { onAdd() }
        testSoundButton.addActionListener { if (requireAdmin()) SoundHelper.playAlarm() }
        passwordButton.addActionListener { if (requireAdmin()) PasswordDialog(this, auth).isVisible = true }
        sampleButton.addActionListener { if (requireAdmin()) onSamples() }
        refreshBtn.addActionListener {
            checkAlarms()
            refresh()
        }
        bottom.add(addButton)
        bottom.add(testSoundButton)
        bottom.add(passwordButton)
        bottom.add(sampleButton)
        bottom.add(refreshBtn)
        bottom.add(authButton)
        root.add(bottom, BorderLayout.SOUTH)

        FuturisticUI.onThemeChange { rebuildChrome(); refresh() }

        Timer(1000) { tick() }.start()
        scheduler.scheduleAtFixedRate(this::checkAlarms, 2, 15, TimeUnit.SECONDS)
        refresh()
        SwingUtilities.invokeLater(this::showDueSecretsOnLaunch)
    }

    private fun rebuildChrome() {
        val t = FuturisticUI.theme()
        contentPane.background = t.bgTop
        clockLabel.foreground = t.gold
        statsLabel.foreground = t.muted
        FuturisticUI.button(addButton, t.accent)
        FuturisticUI.ghostButton(testSoundButton)
        FuturisticUI.ghostButton(passwordButton)
        FuturisticUI.ghostButton(sampleButton)
        FuturisticUI.ghostButton(authButton)
        for (r in rows) {
            val accent = r.item.accentColor(t.accent)
            r.badge.foreground = if (r.item.isDueToday(LocalDate.now())) t.success else accent
        }
        root.repaint()
    }

    private fun rebuildCategoryBox() {
        suppressRefresh = true
        try {
            val cats = TreeSet(String.CASE_INSENSITIVE_ORDER)
            cats.add("All categories")
            synchronized(store) {
                for (e in store.items()) cats.add(e.displayCategory())
            }
            for (p in EventItem.CATEGORY_PRESETS) cats.add(p)
            val sel = categoryBox.selectedItem
            categoryBox.removeAllItems()
            for (c in cats) categoryBox.addItem(c)
            if (sel != null) categoryBox.selectedItem = sel
            else categoryBox.selectedItem = "All categories"
        } finally {
            suppressRefresh = false
        }
    }

    // ---------------------------------------------------------------- roles
    private fun onAuth() {
        if (auth.isAdmin()) {
            auth.logout()
            updateRoleUI()
        } else {
            LoginDialog(this, auth).isVisible = true
            refresh()
        }
    }

    private fun requireAdmin(): Boolean {
        if (auth.isAdmin()) return true
        JOptionPane.showMessageDialog(this, "Admin access is required for that action.")
        return false
    }

    private fun updateRoleUI() {
        val admin = auth.isAdmin()
        authButton.text = if (admin) "LOG OUT" else "ADMIN LOGIN"
        FuturisticUI.ghostButton(authButton)
        addButton.isVisible = admin
        testSoundButton.isVisible = admin
        passwordButton.isVisible = admin
        sampleButton.isVisible = admin
        for (r in rows) r.adminBar.isVisible = admin
        statusLabel.text = if (admin) "♥ ADMIN MODE" else "♥ LIVE • READ ONLY"
        statusLabel.foreground = if (admin) FuturisticUI.theme().success else FuturisticUI.theme().muted
        listPanel.revalidate()
        listPanel.repaint()
    }

    // --------------------------------------------------------------- listing
    fun refresh() {
        if (suppressRefresh) return
        SwingUtilities.invokeLater {
            rebuildCategoryBoxQuiet()
            rows.clear()
            listPanel.removeAll()
            val filtered = applyFilter(store.sortedByNext(LocalDate.now()))
            applySort(filtered)
            updateStats(filtered)
            if (filtered.isEmpty()) {
                val empty = JPanel(BorderLayout())
                empty.isOpaque = false
                empty.border = BorderFactory.createEmptyBorder(60, 20, 60, 20)
                empty.add(
                    FuturisticUI.emptyArt(
                        "📅", "No countdowns match",
                        "Try a different search, or ask an admin to add one."
                    ),
                    BorderLayout.CENTER
                )
                listPanel.add(empty)
            } else {
                for (e in filtered) {
                    val r = buildCard(e)
                    rows.add(r)
                    listPanel.add(r.adminBar.parent)
                    listPanel.add(Box.createVerticalStrut(12))
                }
            }
            updateRoleUI()
            listPanel.revalidate()
            listPanel.repaint()
        }
    }

    private fun rebuildCategoryBoxQuiet() {
        // keep selection, just ensure new categories appear
        val sel = categoryBox.selectedItem
        val cats = TreeSet(String.CASE_INSENSITIVE_ORDER)
        cats.add("All categories")
        synchronized(store) {
            for (e in store.items()) cats.add(e.displayCategory())
        }
        for (p in EventItem.CATEGORY_PRESETS) cats.add(p)
        var same = categoryBox.itemCount == cats.size
        if (same) {
            var i = 0
            for (c in cats) {
                if (categoryBox.getItemAt(i++).toString() != c) {
                    same = false
                    break
                }
            }
        }
        if (!same) {
            suppressRefresh = true
            try {
                categoryBox.removeAllItems()
                for (c in cats) categoryBox.addItem(c)
                if (sel != null) categoryBox.selectedItem = sel
            } finally {
                suppressRefresh = false
            }
        }
    }

    private fun applyFilter(input: List<EventItem>): MutableList<EventItem> {
        val today = LocalDate.now()
        var q = searchField.text.trim().lowercase()
        if (q == "search countdowns...") q = ""
        val f = filterBox.selectedItem?.toString() ?: ""
        val cat = categoryBox.selectedItem?.toString() ?: ""
        val out = mutableListOf<EventItem>()
        for (e in input) {
            if (cat != "All categories" && !e.displayCategory().equals(cat, ignoreCase = true)) continue
            if (q.isNotEmpty()) {
                val hay = (e.title + " " + e.message + " " + e.displayCategory()).lowercase()
                if (!hay.contains(q)) continue
            }
            val days = e.daysUntil(today)
            when (f) {
                "Today — day is here" -> if (!e.isDueToday(today)) continue
                "Next 7 days" -> if (e.isPast(today) || days > 7) continue
                "Next 30 days" -> if (e.isPast(today) || days > 30) continue
                "Past (one-time)" -> if (!e.isPast(today)) continue
                "Featured ★" -> if (!e.featured) continue
                "With secret message" -> if (!e.hasSecret()) continue
                else -> {}
            }
            out.add(e)
        }
        return out
    }

    private fun applySort(list: MutableList<EventItem>) {
        val today = LocalDate.now()
        val s = sortBox.selectedItem?.toString() ?: ""
        when {
            s.startsWith("Sort: name") -> list.sortBy { (it.title).lowercase() }
            s.startsWith("Sort: newest") -> list.sortByDescending { it.createdAt }
            s.startsWith("Sort: biggest") -> list.sortByDescending { it.daysUntil(today) }
            else -> list.sortWith(compareBy<EventItem> { !it.featured }.thenBy { it.nextOccurrence(today) })
        }
    }

    private fun updateStats(shown: List<EventItem>) {
        val today = LocalDate.now()
        val total: Int
        synchronized(store) { total = store.items().size }
        var todayN = 0
        var weekN = 0
        synchronized(store) {
            for (e in store.items()) {
                if (e.isDueToday(today)) todayN++
                else if (!e.isPast(today) && e.daysUntil(today) <= 7) weekN++
            }
        }
        val next = if (shown.isEmpty()) "nothing filtered" else ("next: " + shown[0].title)
        statsLabel.text = "$total total  •  $todayN today  •  $weekN this week  •  $next"
    }

    // ----------------------------------------------------------------- cards
    private fun buildCard(e: EventItem): Row {
        val t = FuturisticUI.theme()
        val accent = e.accentColor(t.accent)
        val r = Row()
        r.item = e
        val due = e.isDueToday(LocalDate.now())

        r.countdown = FuturisticUI.label("", 26f, t.text, Font.BOLD)
        r.countdown.font = Font(Font.MONOSPACED, Font.BOLD, 24)
        r.badge = FuturisticUI.label(
            if (due) "♥ DAY IS HERE ♥" else "♥ LIVE ♥",
            11f, if (due) t.success else accent, Font.BOLD
        )

        val card = FuturisticUI.GlassPanel(22)
        card.setAccent(accent)
        card.layout = BorderLayout(16, 8)

        // left: app-style icon
        val iconWrap = JPanel()
        iconWrap.isOpaque = false
        iconWrap.layout = BoxLayout(iconWrap, BoxLayout.Y_AXIS)
        iconWrap.add(FuturisticUI.appIcon(e.displayIcon(), accent, 54))
        iconWrap.add(Box.createVerticalStrut(6))
        card.add(iconWrap, BorderLayout.WEST)

        // center: title + badges + message + progress
        val center = JPanel()
        center.isOpaque = false
        center.layout = BoxLayout(center, BoxLayout.Y_AXIS)
        val titleRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        titleRow.isOpaque = false
        val titleL = FuturisticUI.label(if (e.featured) "★ " + e.title else e.title, 18f, t.text, Font.BOLD)
        titleRow.add(titleL)
        center.add(titleRow)
        val badgeRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        badgeRow.isOpaque = false
        badgeRow.add(
            FuturisticUI.badge(
                e.displayCategory().uppercase(), t.text,
                Color(accent.red, accent.green, accent.blue, if (t.light) 30 else 60)
            )
        )
        val shortUp = e.shortCountdown(LocalDate.now()).uppercase()
        val pillFg = if (due) t.success else accent
        badgeRow.add(
            FuturisticUI.badge(
                shortUp, pillFg,
                Color(
                    if (due) t.success.red else accent.red,
                    if (due) t.success.green else accent.green,
                    if (due) t.success.blue else accent.blue, 22
                )
            )
        )
        if (e.hasSecret()) badgeRow.add(
            FuturisticUI.badge(
                "SECRET ARMED", t.gold,
                Color(t.gold.red, t.gold.green, t.gold.blue, 24)
            )
        )
        if (e.repeatYearly) badgeRow.add(
            FuturisticUI.badge(
                "YEARLY", t.muted,
                Color(t.muted.red, t.muted.green, t.muted.blue, 20)
            )
        )
        center.add(badgeRow)
        val date = FuturisticUI.label("📅 " + e.dateLabel(), 12f, t.muted, Font.PLAIN)
        center.add(date)
        val msg = if (e.message.isEmpty()) "A special moment is waiting…" else e.message
        val msgL = FuturisticUI.label(trim(msg, 140), 12f, t.muted, Font.PLAIN)
        center.add(Box.createVerticalStrut(4))
        center.add(msgL)
        center.add(Box.createVerticalStrut(8))
        r.progressHolder = JPanel(BorderLayout(8, 0))
        r.progressHolder.isOpaque = false
        r.progress = FuturisticUI.progressBar(e.progress(LocalDate.now()), accent)
        r.pct = FuturisticUI.label(Math.round(e.progress(LocalDate.now()) * 100).toString() + "%", 10f, t.muted, Font.BOLD)
        r.progressHolder.add(r.progress, BorderLayout.CENTER)
        r.progressHolder.add(r.pct, BorderLayout.EAST)
        r.progressHolder.maximumSize = Dimension(Int.MAX_VALUE, 12)
        center.add(r.progressHolder)
        card.add(center, BorderLayout.CENTER)

        // right: big countdown
        val right = JPanel()
        right.isOpaque = false
        right.layout = BoxLayout(right, BoxLayout.Y_AXIS)
        r.badge.horizontalAlignment = JLabel.RIGHT
        r.badge.alignmentX = Component.RIGHT_ALIGNMENT
        r.countdown.horizontalAlignment = JLabel.RIGHT
        r.countdown.alignmentX = Component.RIGHT_ALIGNMENT
        right.add(r.badge)
        right.add(Box.createVerticalStrut(8))
        right.add(r.countdown)
        r.sub = FuturisticUI.label(e.shortCountdown(LocalDate.now()), 11f, t.muted, Font.BOLD)
        r.sub.horizontalAlignment = JLabel.RIGHT
        r.sub.alignmentX = Component.RIGHT_ALIGNMENT
        right.add(Box.createVerticalStrut(4))
        right.add(r.sub)
        card.add(right, BorderLayout.EAST)

        r.adminBar = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 4))
        r.adminBar.isOpaque = false
        val edit = JButton("EDIT")
        val dup = JButton("DUPLICATE")
        val ring = JButton("RING")
        val del = JButton("DELETE")
        FuturisticUI.ghostButton(edit)
        FuturisticUI.ghostButton(dup)
        FuturisticUI.ghostButton(ring)
        FuturisticUI.button(del, t.danger)
        edit.addActionListener { onEdit(e) }
        dup.addActionListener { onDuplicate(e) }
        ring.addActionListener { onRing(e) }
        del.addActionListener { onDelete(e) }
        r.adminBar.add(edit)
        r.adminBar.add(dup)
        r.adminBar.add(ring)
        r.adminBar.add(del)
        val outer = JPanel(BorderLayout())
        outer.isOpaque = false
        outer.add(card, BorderLayout.CENTER)
        outer.add(r.adminBar, BorderLayout.SOUTH)
        r.adminBar.isVisible = auth.isAdmin()
        tickRow(r, LocalDateTime.now())
        return r
    }

    private fun tick() {
        val now = LocalDateTime.now()
        clockLabel.text = now.format(DateTimeFormatter.ofPattern("EEE • dd MMM • HH:mm:ss"))
        for (r in rows) tickRow(r, now)
    }

    private fun tickRow(r: Row, now: LocalDateTime) {
        val live = store.byId(r.item.id)
        if (live != null) r.item = live
        val due = r.item.isDueToday(now.toLocalDate())
        val t = FuturisticUI.theme()
        val accent = r.item.accentColor(t.accent)
        r.countdown.text = r.item.countdownText(now)
        r.badge.text = if (due) "♥ DAY IS HERE ♥" else "♥ LIVE ♥"
        r.badge.foreground = if (due) t.success else accent
        r.sub.text = r.item.shortCountdown(now.toLocalDate())
        // refresh progress only when the percent actually changes to avoid flicker.
        val pctVal = Math.round(r.item.progress(now.toLocalDate()) * 100).toInt()
        if (r.pct.text != "$pctVal%") {
            r.progressHolder.remove(r.progress)
            r.progress = FuturisticUI.progressBar(r.item.progress(now.toLocalDate()), accent)
            r.progressHolder.add(r.progress, BorderLayout.CENTER)
            r.pct.text = "$pctVal%"
            r.progressHolder.revalidate()
            r.progressHolder.repaint()
        }
    }

    // --------------------------------------------------------------- actions
    private fun onAdd() {
        if (!requireAdmin()) return
        val item = EventItem()
        item.date = LocalDate.now().plusDays(7)
        item.repeatYearly = true
        item.soundEnabled = true
        item.icon = "📅"
        item.category = "Countdown"
        EditDialog(this, store, item, true).isVisible = true
        refresh()
    }

    private fun onEdit(e: EventItem) {
        if (!requireAdmin()) return
        val copy = EventItem.fromJson(e.toJson())
        EditDialog(this, store, copy, false).isVisible = true
        refresh()
    }

    private fun onDuplicate(e: EventItem) {
        if (!requireAdmin()) return
        val copy = EventItem.fromJson(e.toJson())
        copy.id = UUID.randomUUID().toString().replace("-", "")
        copy.title = e.title + " (copy)"
        copy.createdAt = LocalDateTime.now()
        store.addOrUpdate(copy)
        refresh()
    }

    private fun onDelete(e: EventItem) {
        if (!requireAdmin()) return
        val ok = JOptionPane.showConfirmDialog(
            this, "Delete '" + e.title + "'?",
            "Delete countdown", JOptionPane.YES_NO_OPTION
        )
        if (ok == JOptionPane.YES_OPTION) {
            store.delete(e.id)
            refresh()
        }
    }

    private fun onSamples() {
        if (!requireAdmin()) return
        val ok = JOptionPane.showConfirmDialog(
            this,
            "Add sample countdowns for birthdays, exams, trips and app launches?",
            "Add samples", JOptionPane.YES_NO_OPTION
        )
        if (ok != JOptionPane.YES_OPTION) return
        addSample(
            "Birthday 🎂", LocalDate.now().plusDays(14), "Birthday",
            "🎂", "#FFB020", "Cake, friends and music!", true
        )
        addSample(
            "Final Exams 🎓", LocalDate.now().plusDays(30), "Exam",
            "🎓", "#22C4A8", "One chapter a day keeps stress away.", true
        )
        addSample(
            "Android App Launch 🚀", LocalDate.now().plusDays(60), "App Release",
            "🚀", "#7C6CFF", "Release v2.0 to the Play Store.", false
        )
        addSample(
            "Beach Trip ✈", LocalDate.now().plusDays(90), "Trip",
            "✈", "#38BDF8", "Sunscreen, playlists, passports.", false
        )
        addSample(
            "Wedding Day 💖", LocalDate.now().plusDays(120), "Wedding",
            "💖", "#F472B6", "The big day - don't forget the rings!", true
        )
        refresh()
    }

    private fun addSample(
        title: String,
        date: LocalDate,
        category: String,
        icon: String,
        accent: String,
        msg: String,
        yearly: Boolean
    ) {
        val e = EventItem()
        e.title = title
        e.date = date
        e.category = category
        e.icon = icon
        e.accentHex = accent
        e.message = msg
        e.repeatYearly = yearly
        e.soundEnabled = true
        store.addOrUpdate(e)
    }

    private fun onRing(e: EventItem) {
        if (!requireAdmin()) return
        if (e.soundEnabled) SoundHelper.playAlarm()
        if (e.hasSecret()) SecretMessageDialog(this, e).isVisible = true
        else AlarmDialog(this, e, Runnable {}).isVisible = true
    }

    private fun showDueSecretsOnLaunch() {
        val today = LocalDate.now()
        for (e in store.items()) {
            if (e.isDueToday(today) && e.hasSecret() && !secretShownThisSession.contains(e.id)) {
                secretShownThisSession.add(e.id)
                SecretMessageDialog(this, e).isVisible = true
            }
        }
    }

    private fun checkAlarms() {
        try {
            val today = LocalDate.now()
            val due = mutableListOf<EventItem>()
            synchronized(store) {
                for (e in store.items()) {
                    if (!e.isDueToday(today)) continue
                    val key = "fired_" + e.id + "_" + today
                    if (fired.getBoolean(key, false)) continue
                    fired.putBoolean(key, true)
                    due.add(e)
                }
            }
            for (e in due) fire(e)
        } catch (ignored: Exception) {
        }
    }

    private fun fire(e: EventItem) {
        if (e.hasSecret()) {
            SwingUtilities.invokeLater {
                if (!secretShownThisSession.contains(e.id)) {
                    secretShownThisSession.add(e.id)
                    SecretMessageDialog(this, e).isVisible = true
                }
            }
        } else {
            if (e.soundEnabled) SoundHelper.playAlarm()
            SwingUtilities.invokeLater { AlarmDialog(this, e, Runnable {}).isVisible = true }
        }
    }

    companion object {
        private fun trim(s: String?, max: Int): String {
            if (s == null) return ""
            val oneLine = s.replace('\n', ' ').replace('\r', ' ').trim().replace("\\s+".toRegex(), " ")
            return if (oneLine.length <= max) oneLine else oneLine.substring(0, max - 1) + "…"
        }
    }
}
