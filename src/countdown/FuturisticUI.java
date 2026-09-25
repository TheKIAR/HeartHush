package countdown;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public final class FuturisticUI {
    public static final Color BG = new Color(7, 10, 18);
    public static final Color PANEL = new Color(15, 20, 32);
    public static final Color PANEL_2 = new Color(20, 27, 43);
    public static final Color CYAN = new Color(75, 224, 255);
    public static final Color PURPLE = new Color(154, 112, 255);
    public static final Color TEXT = new Color(236, 242, 255);
    public static final Color MUTED = new Color(151, 164, 188);
    public static final Color GREEN = new Color(77, 235, 166);
    public static final Color RED = new Color(255, 99, 132);

    private FuturisticUI() {}

    public static void frame(javax.swing.JFrame frame) {
        frame.getContentPane().setBackground(BG);
    }

    public static void button(JButton b, Color accent) {
        b.setForeground(TEXT);
        b.setBackground(accent);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        b.setOpaque(true);
        b.setFont(b.getFont().deriveFont(java.awt.Font.BOLD, 12f));
    }

    public static void ghostButton(JButton b) {
        b.setForeground(MUTED);
        b.setBackground(PANEL_2);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(55, 68, 92)));
        b.setFont(b.getFont().deriveFont(java.awt.Font.BOLD, 12f));
    }

    public static void field(JTextField f) {
        f.setForeground(TEXT);
        f.setBackground(new Color(10, 14, 24));
        f.setCaretColor(CYAN);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(53, 69, 96)),
                new EmptyBorder(8, 10, 8, 10)));
    }

    public static JLabel label(String text, float size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont(style, size));
        return l;
    }

    public static class GridBackground extends JPanel {
        private double pulse = 0;
        public GridBackground() {
            setOpaque(false);
            new javax.swing.Timer(60, e -> {
                pulse += 0.025;
                repaint();
            }).start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(8, 12, 23), w, h, new Color(18, 12, 34)));
            g2.fillRect(0, 0, w, h);
            int spacing = 42;
            for (int x = 0; x < w; x += spacing) {
                g2.setColor(new Color(75, 224, 255, 13));
                g2.drawLine(x, 0, x, h);
            }
            for (int y = 0; y < h; y += spacing) {
                g2.setColor(new Color(154, 112, 255, 11));
                g2.drawLine(0, y, w, y);
            }
            int cx = (int) (w * 0.78 + Math.sin(pulse) * 18);
            int cy = (int) (h * 0.18 + Math.cos(pulse * 0.7) * 12);
            for (int r = 150; r > 20; r -= 25) {
                int alpha = Math.max(3, 28 - r / 7);
                g2.setColor(new Color(75, 224, 255, alpha));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            }
            g2.dispose();
        }
    }

    public static class GlassPanel extends JPanel {
        private final int arc;
        public GlassPanel(int arc) {
            this.arc = arc;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            g2.setColor(new Color(10, 15, 27, 225));
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(75, 224, 255, 45));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
