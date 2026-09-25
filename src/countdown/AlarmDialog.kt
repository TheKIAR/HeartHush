package countdown

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.Frame
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/** "It's time!" popup shown when any countdown is due. */
class AlarmDialog(owner: Frame?, item: EventItem, private val onSnooze: Runnable) :
    JDialog(owner, "It's time — " + item.title, false) {

    init {
        val t = FuturisticUI.theme()
        val accent = item.accentColor(t.accent)
        contentPane.background = t.bgTop

        val body = JPanel()
        body.isOpaque = false
        body.layout = BoxLayout(body, BoxLayout.Y_AXIS)
        body.border = BorderFactory.createEmptyBorder(30, 30, 30, 30)

        val bell = label(item.displayIcon() + "  IT'S TIME!", 34, Font.BOLD, t.text)
        val title = label(item.title, 22, Font.BOLD, accent)
        val cat = label(
            item.displayCategory().uppercase() + "  •  " + item.dateLabel().uppercase(),
            12, Font.BOLD, t.muted
        )
        val msg = if (item.message.trim().isEmpty()) "The day has arrived - open the app to celebrate!"
        else item.message
        val message = label("<html><center>" + escapeHtml(msg) + "</center></html>", 14, Font.PLAIN, t.text)
        val time = label(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm:ss")),
            12, Font.PLAIN, t.muted
        )

        val stop = JButton("Stop alarm")
        FuturisticUI.button(stop, accent)
        val snooze = JButton("Snooze 1 min (demo)")
        FuturisticUI.ghostButton(snooze)

        stop.addActionListener {
            SoundHelper.stop()
            dispose()
        }
        snooze.addActionListener {
            SoundHelper.stop()
            dispose()
            val timer = Timer("snooze", true)
            timer.schedule(object : TimerTask() {
                override fun run() {
                    if (item.soundEnabled) {
                        SoundHelper.playAlarm()
                    }
                    SwingUtilities.invokeLater { onSnooze.run() }
                }
            }, 60_000)
        }

        for (l in arrayOf(bell, title, cat, message, time)) {
            l.alignmentX = CENTER_ALIGNMENT
            body.add(l)
            body.add(Box.createVerticalStrut(10))
        }
        stop.alignmentX = CENTER_ALIGNMENT
        snooze.alignmentX = CENTER_ALIGNMENT
        body.add(stop)
        body.add(Box.createVerticalStrut(8))
        body.add(snooze)

        val wrap = FuturisticUI.GlassPanel(24)
        wrap.setAccent(accent)
        wrap.layout = BorderLayout()
        wrap.add(body, BorderLayout.CENTER)
        val bg = FuturisticUI.GridBackground()
        bg.layout = BorderLayout()
        bg.add(wrap, BorderLayout.CENTER)
        contentPane = bg
        setSize(460, 540)
        setLocationRelativeTo(owner)
    }

    companion object {
        private fun label(text: String, size: Int, style: Int, color: Color): JLabel {
            val l = JLabel(text, SwingConstants.CENTER)
            l.font = Font(Font.SANS_SERIF, style, size)
            l.foreground = color
            l.alignmentX = CENTER_ALIGNMENT
            return l
        }

        private fun escapeHtml(s: String): String {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>")
        }
    }
}
