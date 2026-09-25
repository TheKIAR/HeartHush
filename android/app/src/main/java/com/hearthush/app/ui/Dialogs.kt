package com.hearthush.app.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.hearthush.app.R
import com.hearthush.app.logic.EventItem
import com.hearthush.app.logic.SoundHelper

object Dialogs {

    fun secret(context: Context, item: EventItem) {
        val msg = TextView(context).apply {
            textSize = 16f
            setPadding(48, 24, 48, 24)
            visibility = android.view.View.GONE
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(TextView(context).apply {
                text = "${item.title} • ${item.displayCategory()} — the day is here!"
                textSize = 13f
            })
            addView(TextView(context).apply {
                text = "The countdown reached zero. Open your message when you're ready."
                textSize = 13f
            })
            addView(msg)
        }
        val scroll = ScrollView(context).apply { addView(box) }
        val d = AlertDialog.Builder(context)
            .setTitle("\uD83D\uDC8C You have a message")
            .setView(scroll)
            .setPositiveButton(R.string.open_message, null)
            .setNegativeButton(R.string.close, null)
            .create()
        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                msg.text = item.secretMessage
                msg.visibility = android.view.View.VISIBLE
                d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            }
        }
        d.show()
    }

    fun alarm(context: Context, item: EventItem, onSnooze: () -> Unit) {
        val msg = if (item.message.trim().isEmpty()) "The day has arrived - open the app to celebrate!"
        else item.message
        val d = AlertDialog.Builder(context)
            .setTitle("${item.displayIcon()} It's time — ${item.title}")
            .setMessage("${item.displayCategory()} • ${item.dateLabel()}\n\n$msg")
            .setPositiveButton(R.string.stop, null)
            .setNegativeButton(R.string.snooze, null)
            .create()
        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                SoundHelper.stop()
                d.dismiss()
            }
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                SoundHelper.stop()
                d.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({
                    if (item.soundEnabled) SoundHelper.playAlarm()
                    onSnooze()
                }, 60_000)
            }
        }
        d.show()
    }

    fun login(context: Context, onOk: (String) -> Unit) {
        val input = EditText(context).apply {
            hint = "Password (first run: admin123)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(context)
            .setTitle("Admin access")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ -> onOk(input.text.toString()) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun passwordChange(context: Context, onOk: (cur: String, next: String) -> Boolean) {
        val cur = EditText(context).apply { hint = "Current password" }
        val next = EditText(context).apply { hint = "New password (4+ chars)" }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            addView(cur)
            addView(next)
        }
        AlertDialog.Builder(context)
            .setTitle("Admin password")
            .setView(box)
            .setPositiveButton("Change") { d, _ ->
                if (!onOk(cur.text.toString(), next.text.toString())) {
                    android.widget.Toast.makeText(context, R.string.access_denied, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    (d as? AlertDialog)?.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun confirm(context: Context, title: String, message: String, onYes: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onYes() }
            .setNegativeButton("No", null)
            .show()
    }
}
