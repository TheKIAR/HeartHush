package com.hearthush.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hearthush.app.R
import com.hearthush.app.logic.EventItem
import java.time.LocalDate
import java.time.LocalDateTime

class EventAdapter(
    private val brand: Int,
    private val onEdit: (EventItem) -> Unit,
    private val onDuplicate: (EventItem) -> Unit,
    private val onRing: (EventItem) -> Unit,
    private val onDelete: (EventItem) -> Unit
) : RecyclerView.Adapter<EventAdapter.H>() {

    var items: List<EventItem> = emptyList()
    var admin: Boolean = false

    private val attached = mutableSetOf<H>()

    inner class H(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.icon)
        val title: TextView = v.findViewById(R.id.title)
        val badges: TextView = v.findViewById(R.id.badges)
        val live: TextView = v.findViewById(R.id.live)
        val countdown: TextView = v.findViewById(R.id.countdown)
        val sub: TextView = v.findViewById(R.id.sub)
        val date: TextView = v.findViewById(R.id.date)
        val message: TextView = v.findViewById(R.id.message)
        val progress: ProgressBar = v.findViewById(R.id.progress)
        val adminBar: LinearLayout = v.findViewById(R.id.adminBar)

        fun bindTime(item: EventItem, now: LocalDateTime) {
            val due = item.isDueToday(now.toLocalDate())
            val accent = item.accentInt(brand)
            live.text = if (due) "♥ DAY IS HERE ♥" else "♥ LIVE ♥"
            live.setTextColor(if (due) Color.parseColor("#10B981") else accent)
            countdown.text = item.countdownText(now)
            sub.text = item.shortCountdown(now.toLocalDate())
            val pct = item.progressPercent(now.toLocalDate())
            progress.progress = pct
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return H(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: H, position: Int) {
        val e = items[position]
        val now = LocalDateTime.now()
        val accent = e.accentInt(brand)
        h.icon.text = e.displayIcon()
        h.title.text = if (e.featured) "★ ${e.title}" else e.title
        val bits = mutableListOf(e.displayCategory().uppercase(), e.shortCountdown(LocalDate.now()).uppercase())
        if (e.hasSecret()) bits.add("SECRET ARMED")
        if (e.repeatYearly) bits.add("YEARLY")
        h.badges.text = bits.joinToString(" • ")
        h.badges.setTextColor(accent)
        h.date.text = "📅 ${e.dateLabel()}"
        h.message.text = if (e.message.isEmpty()) "A special moment is waiting…" else e.message
        h.bindTime(e, now)
        h.adminBar.visibility = if (admin) View.VISIBLE else View.GONE
        h.itemView.findViewById<Button>(R.id.btnEdit).setOnClickListener { onEdit(e) }
        h.itemView.findViewById<Button>(R.id.btnDup).setOnClickListener { onDuplicate(e) }
        h.itemView.findViewById<Button>(R.id.btnRing).setOnClickListener { onRing(e) }
        h.itemView.findViewById<Button>(R.id.btnDel).setOnClickListener { onDelete(e) }
    }

    override fun onViewAttachedToWindow(h: H) {
        attached.add(h)
    }

    override fun onViewDetachedFromWindow(h: H) {
        attached.remove(h)
    }

    fun tick() {
        val now = LocalDateTime.now()
        for (h in attached) {
            val pos = h.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                h.bindTime(items[pos], now)
            }
        }
    }

    fun refresh(newItems: List<EventItem>, isAdmin: Boolean) {
        items = newItems
        admin = isAdmin
        notifyDataSetChanged()
    }
}
