package com.hearthush.app.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.hearthush.app.R
import com.hearthush.app.logic.EventItem
import com.hearthush.app.logic.EventStore
import java.time.LocalDate

object EditDialog {

    fun show(
        context: Context,
        store: EventStore,
        item: EventItem,
        isNew: Boolean,
        onDone: () -> Unit
    ) {
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_edit, null)
        val fTitle = v.findViewById<EditText>(R.id.fTitle)
        val fCategory = v.findViewById<Spinner>(R.id.fCategory)
        val fIcon = v.findViewById<Spinner>(R.id.fIcon)
        val fDate = v.findViewById<Button>(R.id.fDate)
        val fYearly = v.findViewById<CheckBox>(R.id.fYearly)
        val fFeatured = v.findViewById<CheckBox>(R.id.fFeatured)
        val fSound = v.findViewById<CheckBox>(R.id.fSound)
        val fSecret = v.findViewById<CheckBox>(R.id.fSecret)
        val fMessage = v.findViewById<EditText>(R.id.fMessage)
        val fSecretMsg = v.findViewById<EditText>(R.id.fSecretMsg)

        fTitle.setText(item.title)
        fCategory.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, EventItem.CATEGORY_PRESETS)
        fCategory.setSelection(maxOf(0, EventItem.CATEGORY_PRESETS.indexOf(item.displayCategory())))
        fIcon.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, EventItem.ICON_PRESETS)
        fIcon.setSelection(maxOf(0, EventItem.ICON_PRESETS.indexOf(item.displayIcon())))
        var picked: LocalDate = item.date
        fDate.text = picked.toString()
        fDate.setOnClickListener {
            DatePickerDialog(
                context,
                { _, y, m, d -> picked = LocalDate.of(y, m + 1, d); fDate.text = picked.toString() },
                picked.year, picked.monthValue - 1, picked.dayOfMonth
            ).show()
        }
        fYearly.isChecked = item.repeatYearly
        fFeatured.isChecked = item.featured
        fSound.isChecked = item.soundEnabled
        fSecret.isChecked = item.secretEnabled
        fMessage.setText(item.message)
        fSecretMsg.setText(item.secretMessage)

        val dlg = AlertDialog.Builder(context)
            .setTitle(if (isNew) "Create countdown" else "Edit countdown")
            .setView(v)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
        if (!isNew) dlg.setNeutralButton(R.string.delete, null)
        val d = dlg.create()
        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = fTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(context, R.string.enter_title, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                item.title = title
                item.date = picked
                item.category = (fCategory.selectedItem?.toString() ?: "Countdown").trim()
                    .ifEmpty { "Countdown" }
                item.icon = (fIcon.selectedItem?.toString() ?: "📅").trim().ifEmpty { "📅" }
                item.repeatYearly = fYearly.isChecked
                item.featured = fFeatured.isChecked
                item.soundEnabled = fSound.isChecked
                item.secretEnabled = fSecret.isChecked
                item.message = fMessage.text.toString().trim()
                item.secretMessage = fSecretMsg.text.toString()
                if (!item.secretEnabled) item.secretMessage = ""
                store.addOrUpdate(item)
                d.dismiss()
                onDone()
            }
            if (!isNew) {
                d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    store.delete(item.id)
                    d.dismiss()
                    onDone()
                }
            }
        }
        d.show()
    }
}
