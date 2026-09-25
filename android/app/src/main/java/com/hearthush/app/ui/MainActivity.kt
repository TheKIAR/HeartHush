package com.hearthush.app.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hearthush.app.R
import com.hearthush.app.logic.AuthService
import com.hearthush.app.logic.EventItem
import com.hearthush.app.logic.EventStore
import com.hearthush.app.logic.SoundHelper
import java.io.File
import java.time.LocalDate
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var store: EventStore
    private lateinit var auth: AuthService
    private lateinit var adapter: EventAdapter
    private lateinit var stats: TextView
    private lateinit var search: EditText
    private lateinit var filter: Spinner
    private lateinit var sort: Spinner
    private lateinit var btnNew: Button
    private lateinit var btnTest: Button
    private lateinit var btnPassword: Button
    private lateinit var btnSamples: Button
    private lateinit var btnAuth: Button

    private val brand = Color.parseColor("#FF5D97")
    private val shownSecrets = mutableSetOf<String>()
    private val ticker = Handler(Looper.getMainLooper())
    private val tickTask = object : Runnable {
        override fun run() {
            adapter.tick()
            ticker.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = EventStore(File(filesDir, "events.json"))
        store.load()
        auth = AuthService(getSharedPreferences("hearthush", MODE_PRIVATE))

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { onMenu(it) }

        stats = findViewById(R.id.stats)
        search = findViewById(R.id.search)
        filter = findViewById(R.id.filter)
        sort = findViewById(R.id.sort)
        btnNew = findViewById(R.id.btnNew)
        btnTest = findViewById(R.id.btnTest)
        btnPassword = findViewById(R.id.btnPassword)
        btnSamples = findViewById(R.id.btnSamples)
        btnAuth = findViewById(R.id.btnAuth)

        filter.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("All", "Today", "Next 7 days", "Featured", "With secret")
        )
        sort.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Happening next", "Name A–Z", "Biggest countdown")
        )
        val onPick = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = refresh()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        filter.onItemSelectedListener = onPick
        sort.onItemSelectedListener = onPick
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = refresh()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        adapter = EventAdapter(
            brand,
            onEdit = { e ->
                if (!requireAdmin()) return@EventAdapter
                EditDialog.show(this, store, EventItem.fromJson(e.toJson()), false) { refresh() }
            },
            onDuplicate = { e ->
                if (!requireAdmin()) return@EventAdapter
                val copy = EventItem.fromJson(e.toJson())
                copy.id = UUID.randomUUID().toString().replace("-", "")
                copy.title = e.title + " (copy)"
                copy.createdAt = java.time.LocalDateTime.now()
                store.addOrUpdate(copy)
                refresh()
            },
            onRing = { e ->
                if (!requireAdmin()) return@EventAdapter
                ring(e)
            },
            onDelete = { e ->
                if (!requireAdmin()) return@EventAdapter
                Dialogs.confirm(this, "Delete countdown", "Delete '${e.title}'?") {
                    store.delete(e.id)
                    refresh()
                }
            }
        )
        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        btnNew.setOnClickListener {
            if (!requireAdmin()) return@setOnClickListener
            val item = EventItem()
            item.date = LocalDate.now().plusDays(7)
            EditDialog.show(this, store, item, true) { refresh() }
        }
        btnTest.setOnClickListener { if (requireAdmin()) SoundHelper.playAlarm() }
        btnPassword.setOnClickListener {
            if (!requireAdmin()) return@setOnClickListener
            Dialogs.passwordChange(this) { cur, next -> auth.changePassword(cur, next) }
        }
        btnSamples.setOnClickListener {
            if (!requireAdmin()) return@setOnClickListener
            addSamples()
            refresh()
        }
        btnAuth.setOnClickListener { onAuth() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        ticker.post(tickTask)
        refresh()
        showDueSecrets()
    }

    override fun onPause() {
        ticker.removeCallbacks(tickTask)
        super.onPause()
    }

    private fun onMenu(item: MenuItem): Boolean {
        if (item.itemId == R.id.mRefresh) {
            refresh()
            return true
        }
        return false
    }

    private fun onAuth() {
        if (auth.isAdmin()) {
            auth.logout()
            refresh()
        } else {
            Dialogs.login(this) { pw ->
                if (auth.verify(pw)) refresh()
                else Toast.makeText(this, R.string.access_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requireAdmin(): Boolean {
        if (auth.isAdmin()) return true
        Toast.makeText(this, R.string.admin_required, Toast.LENGTH_SHORT).show()
        return false
    }

    private fun updateRoleUI() {
        val admin = auth.isAdmin()
        btnNew.visibility = if (admin) View.VISIBLE else View.GONE
        btnTest.visibility = if (admin) View.VISIBLE else View.GONE
        btnPassword.visibility = if (admin) View.VISIBLE else View.GONE
        btnSamples.visibility = if (admin) View.VISIBLE else View.GONE
        btnAuth.text = if (admin) getString(R.string.logout) else getString(R.string.login)
    }

    private fun refresh() {
        val today = LocalDate.now()
        val q = search.text.toString().trim().lowercase()
        val f = filter.selectedItem?.toString() ?: "All"
        var list = store.sortedByNext(today).filter { e ->
            if (q.isNotEmpty() && !(e.title + " " + e.message + " " + e.displayCategory()).lowercase().contains(q)) return@filter false
            when (f) {
                "Today" -> e.isDueToday(today)
                "Next 7 days" -> !e.isPast(today) && e.daysUntil(today) <= 7
                "Featured" -> e.featured
                "With secret" -> e.hasSecret()
                else -> true
            }
        }
        list = when (sort.selectedItem?.toString()) {
            "Name A–Z" -> list.sortedBy { it.title.lowercase() }
            "Biggest countdown" -> list.sortedByDescending { it.daysUntil(today) }
            else -> list
        }
        var todayN = 0
        var weekN = 0
        for (e in store.items()) {
            if (e.isDueToday(today)) todayN++
            else if (!e.isPast(today) && e.daysUntil(today) <= 7) weekN++
        }
        val next = if (list.isEmpty()) "nothing" else "next: ${list[0].title}"
        stats.text = "${store.items().size} total • $todayN today • $weekN this week • $next"
        adapter.refresh(list, auth.isAdmin())
        updateRoleUI()
    }

    private fun ring(e: EventItem) {
        if (e.soundEnabled) SoundHelper.playAlarm()
        if (e.hasSecret()) Dialogs.secret(this, e)
        else Dialogs.alarm(this, e) { refresh() }
    }

    private fun showDueSecrets() {
        val today = LocalDate.now()
        for (e in store.items()) {
            if (e.isDueToday(today) && e.hasSecret() && !shownSecrets.contains(e.id)) {
                shownSecrets.add(e.id)
                Dialogs.secret(this, e)
            } else if (e.isDueToday(today) && !e.hasSecret() && !shownSecrets.contains(e.id)) {
                shownSecrets.add(e.id)
                if (e.soundEnabled) SoundHelper.playAlarm()
                Dialogs.alarm(this, e) { refresh() }
            }
        }
    }

    private fun addSamples() {
        sample("Birthday 🎂", LocalDate.now().plusDays(14), "Birthday", "🎂", "#FFB020", "Cake, friends and music!", true)
        sample("Final Exams 🎓", LocalDate.now().plusDays(30), "Exam", "🎓", "#22C4A8", "One chapter a day keeps stress away.", true)
        sample("Android App Launch 🚀", LocalDate.now().plusDays(60), "App Release", "🚀", "#7C6CFF", "Release v2.0 to the Play Store.", false)
        sample("Beach Trip ✈", LocalDate.now().plusDays(90), "Trip", "✈", "#38BDF8", "Sunscreen, playlists, passports.", false)
        sample("Wedding Day 💖", LocalDate.now().plusDays(120), "Wedding", "💖", "#F472B6", "The big day!", true)
    }

    private fun sample(title: String, date: LocalDate, category: String, icon: String, accent: String, msg: String, yearly: Boolean) {
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
}
