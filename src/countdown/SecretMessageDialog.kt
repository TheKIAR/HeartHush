package countdown

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingConstants

class SecretMessageDialog(owner: Frame?, item: EventItem) :
    JDialog(owner, "A message is waiting — " + item.title, true) {

    init {
        val t = FuturisticUI.theme()
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(580, 450)
        minimumSize = Dimension(480, 360)
        setLocationRelativeTo(owner)

        val bg = FuturisticUI.GridBackground()
        bg.layout = BorderLayout()
        val glass = FuturisticUI.GlassPanel(28)
        glass.setAccent(item.accentColor(t.accent))
        glass.layout = GridBagLayout()

        val c = GridBagConstraints()
        c.gridx = 0
        c.weightx = 1.0
        c.insets = Insets(6, 10, 6, 10)
        c.fill = GridBagConstraints.HORIZONTAL

        val icon = FuturisticUI.label(item.displayIcon(), 38f, t.accent, Font.PLAIN)
        icon.horizontalAlignment = SwingConstants.CENTER
        c.gridy = 0
        glass.add(icon, c)

        val title = FuturisticUI.label("YOU HAVE A MESSAGE", 22f, t.text, Font.BOLD)
        title.horizontalAlignment = SwingConstants.CENTER
        c.gridy = 1
        glass.add(title, c)

        val forTitle = FuturisticUI.label(
            item.title + " • " + item.displayCategory() + " — the day is here!",
            12f, t.muted, Font.BOLD
        )
        forTitle.horizontalAlignment = SwingConstants.CENTER
        c.gridy = 2
        glass.add(forTitle, c)

        val hint = FuturisticUI.label(
            "The countdown reached zero. Open your message when you're ready.",
            13f, t.muted, Font.PLAIN
        )
        hint.horizontalAlignment = SwingConstants.CENTER
        c.gridy = 3
        glass.add(hint, c)

        val message = JTextArea(if (item.secretMessage.isEmpty()) "" else item.secretMessage)
        message.isEditable = false
        message.lineWrap = true
        message.wrapStyleWord = true
        message.foreground = t.text
        message.background = t.fieldBg
        message.font = Font(Font.SANS_SERIF, Font.PLAIN, 15)
        message.border = BorderFactory.createEmptyBorder(14, 16, 14, 16)
        message.isVisible = false

        val messageBox = JPanel(BorderLayout())
        messageBox.isOpaque = false
        messageBox.add(JScrollPane(message), BorderLayout.CENTER)
        c.gridy = 4
        c.weighty = 1.0
        c.fill = GridBagConstraints.BOTH
        glass.add(messageBox, c)

        val open = JButton("OPEN MESSAGE")
        FuturisticUI.button(open, item.accentColor(t.accent))
        open.addActionListener {
            message.isVisible = true
            open.text = "MESSAGE OPENED"
            open.isEnabled = false
            hint.text = "Kept safe until this exact day."
            glass.revalidate()
            glass.repaint()
        }
        c.gridy = 5
        c.weighty = 0.0
        c.fill = GridBagConstraints.HORIZONTAL
        glass.add(open, c)

        val close = JButton("CLOSE")
        FuturisticUI.ghostButton(close)
        close.addActionListener { dispose() }
        c.gridy = 6
        glass.add(close, c)

        bg.add(glass, BorderLayout.CENTER)
        contentPane = bg
    }
}
