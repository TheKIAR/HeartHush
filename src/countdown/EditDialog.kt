package countdown

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Frame
import java.awt.GridLayout
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerDateModel

class EditDialog(owner: Frame?, private val store: EventStore, private val item: EventItem, isNew: Boolean) :
    JDialog(owner, if (isNew) "Create countdown — any occasion" else "Edit countdown", true) {

    private var chosenAccent: String = item.accentHex

    init {
        val t = FuturisticUI.theme()

        val root = JPanel(BorderLayout())
        root.background = t.bgTop

        val titleField = JTextField(item.title, 24)
        FuturisticUI.field(titleField)

        val categoryBox = JComboBox(EventItem.CATEGORY_PRESETS)
        categoryBox.isEditable = true
        categoryBox.selectedItem = item.displayCategory()
        FuturisticUI.styleCombo(categoryBox)

        val iconBox = JComboBox(EventItem.ICON_PRESETS)
        iconBox.isEditable = true
        iconBox.selectedItem = item.displayIcon()
        FuturisticUI.styleCombo(iconBox)

        val dateSpinner = JSpinner(
            SpinnerDateModel(
                Date.from(item.date.atStartOfDay(ZoneId.systemDefault()).toInstant()), null, null,
                Calendar.DAY_OF_MONTH
            )
        )
        dateSpinner.editor = JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd")

        val yearlyBox = JCheckBox("Repeat every year", item.repeatYearly)
        val featuredBox = JCheckBox("Feature this countdown (★ pinned first)", item.featured)
        val soundBox = JCheckBox("Sound alert at midnight", item.soundEnabled)
        val secretBox = JCheckBox("Enable secret message at zero", item.secretEnabled)
        styleCheck(yearlyBox)
        styleCheck(featuredBox)
        styleCheck(soundBox)
        styleCheck(secretBox)

        // accent swatches
        val swatches = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        swatches.isOpaque = false
        val swatchSel = FuturisticUI.label("Accent: auto (theme)", 11f, t.muted, Font.BOLD)
        for (hex in ACCENTS) {
            val s = object : JButton(if (hex.isEmpty()) "AUTO" else " ") {
                override fun getPreferredSize(): Dimension = Dimension(44, 26)
            }
            if (hex.isEmpty()) {
                FuturisticUI.ghostButton(s)
                s.text = "AUTO"
            } else {
                val c = EventItem.parseHex(hex)
                s.background = c
                s.isOpaque = true
                s.border = BorderFactory.createLineBorder(
                    if (hex.equals(chosenAccent, ignoreCase = true)) Color.WHITE else Color(255, 255, 255, 60), 2
                )
                s.cursor = Cursor(Cursor.HAND_CURSOR)
            }
            s.toolTipText = if (hex.isEmpty()) "Use theme accent" else hex
            s.addActionListener {
                chosenAccent = hex
                swatchSel.text = if (hex.isEmpty()) "Accent: auto (theme)" else "Accent: $hex"
                swatches.repaint()
            }
            swatches.add(s)
        }
        if (chosenAccent.isNotEmpty()) swatchSel.text = "Accent: $chosenAccent"

        val publicArea = area(item.message, 3)
        val secretArea = area(item.secretMessage, 5)

        val form = JPanel(GridLayout(0, 1, 7, 7))
        form.background = t.bgTop
        form.border = BorderFactory.createEmptyBorder(18, 18, 10, 18)
        form.add(FuturisticUI.label("TITLE — birthday, exam, wedding, app launch, anything", 11f, t.muted, Font.BOLD))
        form.add(titleField)
        val row2 = JPanel(GridLayout(1, 2, 10, 0))
        row2.isOpaque = false
        val c1 = JPanel(BorderLayout(0, 4))
        c1.isOpaque = false
        c1.add(FuturisticUI.label("CATEGORY", 11f, t.muted, Font.BOLD), BorderLayout.NORTH)
        c1.add(categoryBox, BorderLayout.CENTER)
        val c2 = JPanel(BorderLayout(0, 4))
        c2.isOpaque = false
        c2.add(FuturisticUI.label("ICON (EMOJI)", 11f, t.muted, Font.BOLD), BorderLayout.NORTH)
        c2.add(iconBox, BorderLayout.CENTER)
        row2.add(c1)
        row2.add(c2)
        form.add(row2)
        form.add(FuturisticUI.label("ACCENT COLOR — per-countdown, works in every theme", 11f, t.accent, Font.BOLD))
        form.add(swatches)
        form.add(swatchSel)
        form.add(FuturisticUI.label("TARGET DATE — HITS ZERO AT 00:00", 11f, t.accent, Font.BOLD))
        form.add(dateSpinner)
        form.add(yearlyBox)
        form.add(featuredBox)
        form.add(soundBox)
        form.add(secretBox)
        form.add(FuturisticUI.label("PUBLIC MESSAGE", 11f, t.muted, Font.BOLD))
        form.add(JScrollPane(publicArea))
        form.add(FuturisticUI.label("SECRET MESSAGE — REVEALED ONLY AT ZERO", 11f, t.gold, Font.BOLD))
        form.add(JScrollPane(secretArea))

        val scroll = JScrollPane(form)
        scroll.border = null
        scroll.verticalScrollBar.unitIncrement = 16

        val buttons = JPanel()
        buttons.background = t.bgTop
        buttons.border = BorderFactory.createEmptyBorder(8, 8, 12, 8)
        val save = JButton(if (isNew) "SAVE COUNTDOWN" else "SAVE CHANGES")
        FuturisticUI.button(save, t.accent)
        val delete = JButton("DELETE")
        FuturisticUI.button(delete, t.danger)
        val cancel = JButton("CANCEL")
        FuturisticUI.ghostButton(cancel)
        delete.isVisible = !isNew
        buttons.add(save)
        buttons.add(delete)
        buttons.add(cancel)

        save.addActionListener {
            val title = titleField.text.trim()
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a countdown title.")
                return@addActionListener
            }
            val d = dateSpinner.value as Date
            item.title = title
            item.date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            if (item.date.isBefore(LocalDate.of(1900, 1, 1))) item.date = LocalDate.now()
            val cat = categoryBox.selectedItem
            item.category = if (cat == null) "Countdown" else cat.toString().trim()
            if (item.category.isEmpty()) item.category = "Countdown"
            val ic = iconBox.selectedItem
            item.icon = if (ic == null) "📅" else ic.toString().trim()
            if (item.icon.isEmpty()) item.icon = "📅"
            item.accentHex = chosenAccent
            item.repeatYearly = yearlyBox.isSelected
            item.featured = featuredBox.isSelected
            item.soundEnabled = soundBox.isSelected
            item.secretEnabled = secretBox.isSelected
            item.message = publicArea.text.trim()
            item.secretMessage = secretArea.text
            if (!item.secretEnabled) item.secretMessage = ""
            store.addOrUpdate(item)
            dispose()
        }
        delete.addActionListener {
            val ok = JOptionPane.showConfirmDialog(
                this, "Delete '" + item.title + "'?",
                "Delete countdown", JOptionPane.YES_NO_OPTION
            )
            if (ok == JOptionPane.YES_OPTION) {
                store.delete(item.id)
                dispose()
            }
        }
        cancel.addActionListener { dispose() }

        root.add(scroll, BorderLayout.CENTER)
        root.add(buttons, BorderLayout.SOUTH)
        contentPane = root
        setSize(560, 760)
        setLocationRelativeTo(owner)
    }

    companion object {
        private val ACCENTS = arrayOf(
            "", "#FF5D97", "#7C6CFF", "#22C4A8", "#FFB020",
            "#38BDF8", "#F472B6", "#4ADE80", "#FB7185", "#A78BFA"
        )

        private fun area(value: String?, rows: Int): JTextArea {
            val t = FuturisticUI.theme()
            val a = JTextArea(value ?: "", rows, 24)
            a.lineWrap = true
            a.wrapStyleWord = true
            a.foreground = t.text
            a.background = t.fieldBg
            a.caretColor = t.accent
            a.border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
            return a
        }

        private fun styleCheck(b: JCheckBox) {
            b.isOpaque = false
            b.foreground = FuturisticUI.theme().text
            b.isFocusPainted = false
        }
    }
}
