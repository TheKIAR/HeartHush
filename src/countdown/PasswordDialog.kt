package countdown

import java.awt.BorderLayout
import java.awt.Font
import java.awt.Frame
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.SwingConstants

class PasswordDialog(owner: Frame?, private val auth: AuthService) :
    JDialog(owner, "Admin Password Settings", true) {

    init {
        val t = FuturisticUI.theme()
        val root = JPanel(BorderLayout(0, 12))
        root.background = t.bgTop
        root.border = BorderFactory.createEmptyBorder(18, 20, 18, 20)

        val title = FuturisticUI.label("ADMIN PASSWORD", 20f, t.text, Font.BOLD)
        title.horizontalAlignment = SwingConstants.CENTER
        root.add(title, BorderLayout.NORTH)

        val form = JPanel(GridLayout(0, 1, 7, 7))
        form.isOpaque = false
        val current = JPasswordField()
        val next = JPasswordField()
        val confirm = JPasswordField()
        FuturisticUI.field(current)
        FuturisticUI.field(next)
        FuturisticUI.field(confirm)
        form.add(FuturisticUI.label("CURRENT PASSWORD", 11f, t.muted, Font.BOLD))
        form.add(current)
        form.add(FuturisticUI.label("NEW PASSWORD (4+ CHARACTERS)", 11f, t.muted, Font.BOLD))
        form.add(next)
        form.add(FuturisticUI.label("CONFIRM NEW PASSWORD", 11f, t.muted, Font.BOLD))
        form.add(confirm)
        root.add(form, BorderLayout.CENTER)

        val buttons = JPanel()
        buttons.isOpaque = false
        val save = JButton("CHANGE PASSWORD")
        FuturisticUI.button(save, t.accent)
        val cancel = JButton("CANCEL")
        FuturisticUI.ghostButton(cancel)
        buttons.add(save)
        buttons.add(cancel)
        root.add(buttons, BorderLayout.SOUTH)

        save.addActionListener {
            val n = String(next.password)
            val c = String(confirm.password)
            if (n != c) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.")
                return@addActionListener
            }
            if (auth.changePassword(String(current.password), n)) {
                JOptionPane.showMessageDialog(this, "Admin password updated.")
                dispose()
            } else {
                JOptionPane.showMessageDialog(this, "Current password is incorrect, or the new password is too short.")
            }
        }
        cancel.addActionListener { dispose() }
        contentPane = root
        setSize(430, 340)
        setLocationRelativeTo(owner)
    }
}
