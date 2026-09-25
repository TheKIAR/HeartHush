package countdown

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.util.Random
import java.util.prefs.Preferences
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicButtonUI

/**
 * Theme engine + Material-style widgets.
 * Works for ANY kind of countdown (birthday, exam, wedding, app release...)
 * and any app flavor - pick a theme, cards adapt automatically.
 */
object FuturisticUI {

    // Legacy constants (kept so old code still compiles). New code should use theme().
    val BG = Color(20, 5, 13)
    val PANEL = Color(35, 10, 23)
    val PANEL_2 = Color(54, 15, 34)
    val PINK = Color(255, 93, 151)
    val PURPLE = Color(207, 91, 255)
    val CYAN = Color(255, 139, 190)
    val ROSE = Color(255, 55, 105)
    val GOLD = Color(255, 201, 115)
    val TEXT = Color(255, 242, 247)
    val MUTED = Color(204, 164, 181)
    val GREEN = Color(101, 239, 174)
    val RED = Color(255, 95, 117)

    // ---------------------------------------------------------------- themes
    class Theme(
        val id: String,
        val name: String,
        val bgTop: Color,
        val bgBottom: Color,
        val accent: Color,
        val accent2: Color,
        val gold: Color,
        val text: Color,
        val muted: Color,
        val cardBg: Color,
        val cardBorder: Color,
        val success: Color,
        val danger: Color,
        val fieldBg: Color,
        val light: Boolean
    ) {
        override fun toString(): String = name
    }

    val VALENTINE = Theme(
        "valentine", "Valentine", Color(31, 6, 19), Color(74, 10, 42),
        Color(255, 93, 151), Color(207, 91, 255), Color(255, 201, 115),
        Color(255, 242, 247), Color(204, 164, 181),
        Color(35, 8, 23, 232), Color(255, 93, 151, 65),
        Color(101, 239, 174), Color(255, 95, 117), Color(24, 7, 16), false
    )

    val MIDNIGHT = Theme(
        "midnight", "Midnight Android", Color(8, 12, 28), Color(18, 28, 64),
        Color(88, 140, 255), Color(0, 210, 255), Color(255, 201, 115),
        Color(235, 242, 255), Color(150, 168, 200),
        Color(15, 22, 44, 235), Color(88, 140, 255, 70),
        Color(52, 211, 153), Color(248, 113, 113), Color(10, 15, 32), false
    )

    val OCEAN = Theme(
        "ocean", "Ocean", Color(4, 26, 32), Color(8, 60, 72),
        Color(34, 196, 168), Color(56, 189, 248), Color(253, 224, 71),
        Color(236, 253, 245), Color(153, 200, 190),
        Color(6, 34, 42, 235), Color(34, 196, 168, 70),
        Color(52, 211, 153), Color(251, 113, 133), Color(4, 30, 36), false
    )

    val SUNSET = Theme(
        "sunset", "Sunset", Color(30, 8, 20), Color(88, 24, 40),
        Color(255, 122, 89), Color(255, 180, 84), Color(255, 214, 120),
        Color(255, 243, 235), Color(215, 170, 160),
        Color(42, 12, 24, 235), Color(255, 122, 89, 70),
        Color(101, 239, 174), Color(255, 95, 117), Color(32, 10, 18), false
    )

    val FOREST = Theme(
        "forest", "Forest", Color(6, 22, 16), Color(16, 54, 38),
        Color(74, 222, 128), Color(45, 212, 191), Color(253, 224, 71),
        Color(236, 253, 245), Color(150, 190, 170),
        Color(10, 30, 22, 235), Color(74, 222, 128, 65),
        Color(52, 211, 153), Color(248, 113, 113), Color(6, 26, 18), false
    )

    val LAVENDER = Theme(
        "lavender", "Lavender", Color(20, 10, 36), Color(52, 22, 88),
        Color(167, 139, 250), Color(240, 171, 252), Color(255, 201, 115),
        Color(245, 240, 255), Color(180, 168, 215),
        Color(26, 14, 48, 235), Color(167, 139, 250, 70),
        Color(52, 211, 153), Color(251, 113, 133), Color(22, 12, 40), false
    )

    val PORCELAIN = Theme(
        "porcelain", "Porcelain Light", Color(255, 247, 248), Color(255, 228, 235),
        Color(225, 29, 99), Color(190, 60, 150), Color(160, 110, 20),
        Color(46, 24, 34), Color(130, 100, 114),
        Color(255, 255, 255, 242), Color(225, 29, 99, 45),
        Color(5, 150, 105), Color(220, 38, 76), Color(255, 255, 255), true
    )

    val THEMES = arrayOf(VALENTINE, MIDNIGHT, OCEAN, SUNSET, FOREST, LAVENDER, PORCELAIN)

    private var current: Theme = VALENTINE
    private val listeners = mutableListOf<Runnable>()
    private val PREFS: Preferences = Preferences.userNodeForPackage(FuturisticUI::class.java)

    init {
        try {
            val id = PREFS.get("theme", VALENTINE.id)
            for (t in THEMES) if (t.id == id) {
                current = t
                break
            }
        } catch (ignored: Exception) {
        }
    }

    fun theme(): Theme = current

    fun setTheme(t: Theme?) {
        if (t == null) return
        current = t
        try {
            PREFS.put("theme", t.id)
        } catch (ignored: Exception) {
        }
        val copy: List<Runnable>
        synchronized(listeners) { copy = ArrayList(listeners) }
        for (r in copy) {
            try {
                r.run()
            } catch (ignored: Exception) {
            }
        }
    }

    fun byId(id: String?): Theme {
        if (id == null) return VALENTINE
        for (t in THEMES) if (t.id == id) return t
        return VALENTINE
    }

    fun onThemeChange(r: Runnable) {
        synchronized(listeners) { listeners.add(r) }
    }

    // ---------------------------------------------------------------- basics
    fun frame(frame: JFrame) {
        frame.contentPane.background = current.bgTop
    }

    fun label(text: String, size: Float, color: Color, style: Int): JLabel {
        val l = JLabel(text)
        l.foreground = color
        l.font = l.font.deriveFont(style, size)
        return l
    }

    fun themedLabel(text: String, size: Float, style: Int): JLabel {
        return label(text, size, current.text, style)
    }

    fun mutedLabel(text: String, size: Float): JLabel {
        return label(text, size, current.muted, Font.PLAIN)
    }

    // --------------------------------------------------------------- buttons
    private class RoundedButtonUI(private val fill: Color?, private val ghost: Boolean) : BasicButtonUI() {
        override fun installUI(c: JComponent) {
            super.installUI(c)
            styleFlatButton(c as JButton)
        }

        override fun paint(g: Graphics, c: JComponent) {
            val b = c as JButton
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val w = c.width
            val h = c.height
            val hover = b.model.isRollover
            val press = b.model.isPressed
            if (ghost) {
                g2.color = if (current.light) Color(255, 255, 255, 230) else Color(255, 255, 255, 14)
                if (hover) g2.color = if (current.light) Color(255, 235, 240) else Color(255, 255, 255, 30)
                g2.fill(RoundRectangle2D.Double(0.0, 0.0, (w - 1).toDouble(), (h - 1).toDouble(), 14.0, 14.0))
                g2.color = Color(current.accent.red, current.accent.green, current.accent.blue, 110)
                g2.stroke = BasicStroke(1.2f)
                g2.draw(RoundRectangle2D.Double(0.5, 0.5, (w - 2).toDouble(), (h - 2).toDouble(), 14.0, 14.0))
            } else {
                var base = fill ?: current.accent
                if (!b.isEnabled) base = Color(130, 130, 130)
                else if (press) base = base.darker()
                else if (hover) base = base.brighter()
                // soft shadow
                g2.color = Color(0, 0, 0, if (current.light) 30 else 70)
                g2.fill(RoundRectangle2D.Double(1.0, 3.0, (w - 2).toDouble(), (h - 2).toDouble(), 14.0, 14.0))
                val gp = GradientPaint(0f, 0f, base.brighter(), 0f, h.toFloat(), base.darker())
                g2.paint = gp
                g2.fill(RoundRectangle2D.Double(0.0, 0.0, (w - 1).toDouble(), (h - 2).toDouble(), 14.0, 14.0))
            }
            g2.dispose()
            super.paint(g, c)
        }

        override fun getPreferredSize(c: JComponent): Dimension {
            val d = super.getPreferredSize(c)
            d.width = maxOf(d.width + 14, 90)
            d.height = maxOf(d.height + 6, 34)
            return d
        }
    }

    private fun styleFlatButton(b: JButton) {
        b.isOpaque = false
        b.isContentAreaFilled = false
        b.isBorderPainted = false
        b.isFocusPainted = false
        b.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        b.foreground = Color.WHITE
        b.font = b.font.deriveFont(Font.BOLD, 12f)
        b.border = EmptyBorder(9, 18, 9, 18)
        b.isRolloverEnabled = true
    }

    fun button(b: JButton, accent: Color?) {
        b.setUI(RoundedButtonUI(accent, false))
        b.foreground = Color.WHITE
        hoverBrighten(b)
    }

    fun ghostButton(b: JButton) {
        b.setUI(RoundedButtonUI(null, true))
        b.foreground = if (current.light) current.text else Color.WHITE
        b.font = b.font.deriveFont(Font.BOLD, 12f)
    }

    fun primaryButton(text: String, accent: Color?): JButton {
        val b = JButton(text)
        button(b, accent)
        return b
    }

    fun ghost(text: String): JButton {
        val b = JButton(text)
        ghostButton(b)
        return b
    }

    private fun hoverBrighten(b: JButton) {
        b.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                b.repaint()
            }

            override fun mouseExited(e: MouseEvent?) {
                b.repaint()
            }
        })
    }

    // ---------------------------------------------------------------- inputs
    fun field(f: JTextField) {
        f.foreground = current.text
        f.background = current.fieldBg
        f.caretColor = current.accent
        f.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                Color(current.accent.red, current.accent.green, current.accent.blue, 120), 1, true
            ),
            EmptyBorder(9, 12, 9, 12)
        )
    }

    fun styleCombo(c: JComboBox<*>) {
        c.foreground = current.text
        c.background = current.fieldBg
        c.border = BorderFactory.createLineBorder(
            Color(current.accent.red, current.accent.green, current.accent.blue, 120), 1, true
        )
    }

    fun searchField(hint: String): JTextField {
        val f = JTextField(hint, 18)
        f.foreground = current.muted
        f.background = current.fieldBg
        f.caretColor = current.accent
        f.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                Color(current.accent.red, current.accent.green, current.accent.blue, 90), 1, true
            ),
            EmptyBorder(8, 14, 8, 14)
        )
        return f
    }

    // ----------------------------------------------------------------- misc
    /** Small pill badge, e.g. "BIRTHDAY", "TODAY", "12 DAYS LEFT". */
    fun badge(text: String, fg: Color, bg: Color): JLabel {
        val l = JLabel(text)
        l.isOpaque = true
        l.background = bg
        l.foreground = fg
        l.font = l.font.deriveFont(Font.BOLD, 10.5f)
        l.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(fg.red, fg.green, fg.blue, 80), 1, true),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)
        )
        return l
    }

    /** Icon in a tinted circle - makes every card feel like a modern app icon. */
    fun appIcon(emoji: String?, accent: Color?, size: Int): JComponent {
        val p = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val s = minOf(width, height)
                val base = accent ?: current.accent
                g2.color = Color(base.red, base.green, base.blue, if (current.light) 28 else 48)
                g2.fillOval(0, 0, s - 1, s - 1)
                val gp = GradientPaint(
                    0f, 0f, Color(base.red, base.green, base.blue, 200),
                    0f, s.toFloat(), base.darker()
                )
                g2.paint = gp
                g2.stroke = BasicStroke(1.4f)
                g2.drawOval(1, 1, s - 3, s - 3)
                g2.dispose()
                super.paintComponent(g)
            }
        }
        p.isOpaque = false
        p.preferredSize = Dimension(size, size)
        p.maximumSize = Dimension(size, size)
        p.layout = java.awt.GridBagLayout()
        val e = JLabel(emoji ?: "📅")
        e.font = e.font.deriveFont(Font.PLAIN, size * 0.48f)
        p.add(e)
        return p
    }

    fun progressBar(value01: Double, accent: Color?): JComponent {
        val v = value01.coerceIn(0.0, 1.0)
        val a = accent ?: current.accent
        val bar = object : JComponent() {
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val w = width
                val h = height
                g2.color = if (current.light) Color(0, 0, 0, 18) else Color(255, 255, 255, 22)
                g2.fill(RoundRectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble(), h.toDouble(), h.toDouble()))
                val fw = (w * v).toInt()
                if (fw > 0) {
                    val gp = GradientPaint(0f, 0f, a.brighter(), fw.toFloat(), 0f, a.darker())
                    g2.paint = gp
                    g2.fill(RoundRectangle2D.Double(0.0, 0.0, fw.toDouble(), h.toDouble(), h.toDouble(), h.toDouble()))
                }
                g2.dispose()
            }

            override fun getPreferredSize(): Dimension = Dimension(120, 7)
        }
        bar.isOpaque = false
        return bar
    }

    // ------------------------------------------------------------ background
    class GridBackground : JPanel() {
        private val random = Random(7)
        private val parts = Array(30) { Particle(0f, 0f, 0f, 0f, 0f, 0f, false) }
        private var phase = 0.0
        private var fxEnabled = true

        private class Particle(
            var x: Float,
            var y: Float,
            var speed: Float,
            var size: Float,
            var alpha: Float,
            var drift: Float,
            var heart: Boolean
        )

        init {
            isOpaque = false
            val romantic = current.id == "valentine" || current.id == "sunset"
            for (i in parts.indices) {
                parts[i] = Particle(
                    random.nextFloat(), random.nextFloat(),
                    0.00035f + random.nextFloat() * 0.00065f, 8 + random.nextFloat() * 17,
                    0.18f + random.nextFloat() * 0.45f, random.nextFloat() * 2f - 1f,
                    romantic && random.nextBoolean()
                )
            }
            Timer(35) { phase += 0.035; repaint() }.start()
        }

        fun setFxEnabled(v: Boolean) {
            fxEnabled = v
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val w = width
            val h = height
            val t = current
            g2.paint = GradientPaint(0f, 0f, t.bgTop, w.toFloat(), h.toFloat(), t.bgBottom)
            g2.fillRect(0, 0, w, h)

            val spacing = 48
            g2.color = Color(t.accent.red, t.accent.green, t.accent.blue, if (t.light) 14 else 10)
            for (x in 0 until w step spacing) g2.drawLine(x, 0, x, h)
            for (y in 0 until h step spacing) g2.drawLine(0, y, w, y)

            // ambient glows
            val glowX = (w * 0.78 + Math.sin(phase) * 30).toInt()
            val glowY = (h * 0.18 + Math.cos(phase * 0.8) * 18).toInt()
            for (r in 220 downTo 21 step 25) {
                g2.color = Color(
                    t.accent.red, t.accent.green, t.accent.blue,
                    maxOf(2, 22 - r / 12)
                )
                g2.fillOval(glowX - r, glowY - r, r * 2, r * 2)
            }
            val glow2X = (w * 0.12 + Math.cos(phase * 0.6) * 24).toInt()
            val glow2Y = (h * 0.85 + Math.sin(phase * 0.7) * 16).toInt()
            for (r in 160 downTo 21 step 30) {
                g2.color = Color(
                    t.accent2.red, t.accent2.green, t.accent2.blue,
                    maxOf(2, 16 - r / 14)
                )
                g2.fillOval(glow2X - r, glow2Y - r, r * 2, r * 2)
            }

            if (fxEnabled) {
                for (p in parts) {
                    p.y -= p.speed
                    p.x += (Math.sin(phase + p.drift * 4) * 0.00015f).toFloat()
                    if (p.y < -0.05f) {
                        p.y = 1.05f
                        p.x = random.nextFloat()
                    }
                    val c = Color(
                        t.accent.red, t.accent.green, t.accent.blue,
                        (p.alpha * 255).toInt()
                    )
                    val cx = (p.x * w).toInt()
                    val cy = (p.y * h).toInt()
                    if (p.heart) drawHeart(g2, cx, cy, p.size, c)
                    else {
                        g2.color = c
                        g2.fillOval(cx, cy, p.size.toInt() / 2, p.size.toInt() / 2)
                    }
                }
            }
            g2.dispose()
        }

        companion object {
            private fun drawHeart(g2: Graphics2D, cx: Int, cy: Int, s: Float, c: Color) {
                g2.color = c
                val p = java.awt.geom.Path2D.Float()
                p.moveTo(cx.toDouble(), (cy + s * 0.9f).toDouble())
                p.curveTo(
                    (cx - s * 1.15f).toDouble(), (cy + s * 0.05f).toDouble(),
                    (cx - s * 0.75f).toDouble(), (cy - s * 0.8f).toDouble(),
                    cx.toDouble(), (cy - s * 0.25f).toDouble()
                )
                p.curveTo(
                    (cx + s * 0.75f).toDouble(), (cy - s * 0.8f).toDouble(),
                    (cx + s * 1.15f).toDouble(), (cy + 0.05f).toDouble(),
                    cx.toDouble(), (cy + s * 0.9f).toDouble()
                )
                p.closePath()
                g2.fill(p)
            }
        }
    }

    // ------------------------------------------------------------------ card
    class GlassPanel(private val arc: Int) : JPanel() {
        private var accent: Color? = null

        init {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(16, 18, 16, 18)
        }

        fun setAccent(a: Color?) {
            accent = a
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val w = width - 1
            val h = height - 1
            val t = current
            // shadow
            g2.color = Color(0, 0, 0, if (t.light) 22 else 60)
            g2.fill(RoundRectangle2D.Double(0.0, 3.0, w.toDouble(), (h - 2).toDouble(), arc.toDouble(), arc.toDouble()))
            g2.color = t.cardBg
            g2.fill(RoundRectangle2D.Double(0.0, 0.0, w.toDouble(), (h - 2).toDouble(), arc.toDouble(), arc.toDouble()))
            g2.stroke = BasicStroke(1.1f)
            val border = accent?.let { a -> Color(a.red, a.green, a.blue, 90) } ?: t.cardBorder
            g2.color = border
            g2.draw(RoundRectangle2D.Double(0.5, 0.5, (w - 1).toDouble(), (h - 3).toDouble(), arc.toDouble(), arc.toDouble()))
            // accent strip on the left
            accent?.let { a ->
                g2.color = a
                g2.fill(RoundRectangle2D.Double(0.0, 8.0, 5.0, (h - 18).toDouble(), 4.0, 4.0))
            }
            g2.dispose()
            super.paintComponent(g)
        }
    }

    // ------------------------------------------------------- empty-state art
    fun emptyArt(emoji: String, title: String, hint: String): JComponent {
        val p = JPanel()
        p.isOpaque = false
        p.layout = BoxLayout(p, BoxLayout.Y_AXIS)
        val e = JLabel(emoji, JLabel.CENTER)
        e.font = e.font.deriveFont(Font.PLAIN, 44f)
        e.alignmentX = Component.CENTER_ALIGNMENT
        val t = label(title, 17f, current.text, Font.BOLD)
        t.alignmentX = Component.CENTER_ALIGNMENT
        val s = label(hint, 12f, current.muted, Font.PLAIN)
        s.alignmentX = Component.CENTER_ALIGNMENT
        p.add(e)
        p.add(Box.createVerticalStrut(8))
        p.add(t)
        p.add(Box.createVerticalStrut(4))
        p.add(s)
        return p
    }
}
