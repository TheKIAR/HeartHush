package countdown

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Frame
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.SwingConstants

class LoginDialog(owner: Frame?, private val auth: AuthService) : JDialog(owner, "Admin access", true) {
    private val passwordField = JPasswordField(20)

    init {
        val t = FuturisticUI.theme()
        val body = JPanel(BorderLayout(0, 14))
        body.background = t.bgTop
        body.border = BorderFactory.createEmptyBorder(22, 22, 22, 22)

        val title = FuturisticUI.label("ADMIN ACCESS", 22f, t.text, Font.BOLD)
        title.horizontalAlignment = SwingConstants.CENTER
        body.add(title, BorderLayout.NORTH)

        val center = JPanel(BorderLayout(0, 10))
        center.isOpaque = false
        val hint = FuturisticUI.label(
            "<html><center>Unlock create / edit / delete for<br>birthdays, exams, launches - anything.<br>First run password: admin123</center></html>",
            12f, t.muted, Font.PLAIN
        )
        hint.horizontalAlignment = SwingConstants.CENTER
        center.add(hint, BorderLayout.NORTH)
        FuturisticUI.field(passwordField)
        center.add(passwordField, BorderLayout.CENTER)

        val error = FuturisticUI.label(" ", 12f, t.danger, Font.PLAIN)
        error.horizontalAlignment = SwingConstants.CENTER
        center.add(error, BorderLayout.SOUTH)
        body.add(center, BorderLayout.CENTER)

        val buttons = JPanel()
        buttons.isOpaque = false
        val unlock = JButton("UNLOCK")
        FuturisticUI.button(unlock, t.accent)
        val cancel = JButton("CANCEL")
        FuturisticUI.ghostButton(cancel)
        buttons.add(unlock)
        buttons.add(cancel)
        body.add(buttons, BorderLayout.SOUTH)

        unlock.addActionListener {
            if (auth.verify(String(passwordField.password))) dispose()
            else error.text = "Access denied."
        }
        cancel.addActionListener { dispose() }
        passwordField.addActionListener { unlock.doClick() }

        contentPane = body
        size = Dimension(420, 280)
        setLocationRelativeTo(owner)
    }
}
