package countdown;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public final class FuturisticUI {
    public static final Color BG = new Color(20, 5, 13);
    public static final Color PANEL = new Color(35, 10, 23);
    public static final Color PANEL_2 = new Color(54, 15, 34);
    public static final Color PINK = new Color(255, 93, 151);
    public static final Color PURPLE = new Color(207, 91, 255);
    public static final Color CYAN = new Color(255, 139, 190);
    public static final Color ROSE = new Color(255, 55, 105);
    public static final Color GOLD = new Color(255, 201, 115);
    public static final Color TEXT = new Color(255, 242, 247);
    public static final Color MUTED = new Color(204, 164, 181);
    public static final Color GREEN = new Color(101, 239, 174);
    public static final Color RED = new Color(255, 95, 117);

    private FuturisticUI() {}

    public static void frame(javax.swing.JFrame frame) {
        frame.getContentPane().setBackground(BG);
    }

    public static void button(JButton b, Color accent) {
        b.setForeground(Color.WHITE);
        b.setBackground(accent);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        b.setOpaque(true);
        b.setFont(b.getFont().deriveFont(java.awt.Font.BOLD, 12f));
    }

    public static void ghostButton(JButton b) {
        b.setForeground(TEXT);
        b.setBackground(PANEL_2);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(119, 48, 76)));
        b.setFont(b.getFont().deriveFont(java.awt.Font.BOLD, 12f));
    }

    public static void field(JTextField f) {
        f.setForeground(TEXT);
        f.setBackground(new Color(24, 7, 16));
        f.setCaretColor(PINK);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(128, 49, 81)),
                new EmptyBorder(8, 10, 8, 10)));
    }

    public static JLabel label(String text, float size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont(style, size));
        return l;
    }

    public static class GridBackground extends JPanel {
        private final Random random = new Random(7);
        private final Heart[] hearts = new Heart[28];
        private double phase = 0;

        private static class Heart {
            float x, y, speed, size, alpha, drift;
            Heart(float x, float y, float speed, float size, float alpha, float drift) {
                this.x=x; this.y=y; this.speed=speed; this.size=size; this.alpha=alpha; this.drift=drift;
            }
        }

        public GridBackground() {
            setOpaque(false);
            for (int i=0;i<hearts.length;i++) {
                hearts[i] = new Heart(random.nextFloat(), random.nextFloat(),
                        .00035f + random.nextFloat()*.00065f, 8 + random.nextFloat()*17,
                        .18f + random.nextFloat()*.45f, random.nextFloat()*2f-1f);
            }
            new javax.swing.Timer(35, e -> { phase += .035; repaint(); }).start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g2.setPaint(new GradientPaint(0,0,new Color(31,6,19),w,h,new Color(74,10,42)));
            g2.fillRect(0,0,w,h);

            int spacing=48;
            for(int x=0;x<w;x+=spacing){ g2.setColor(new Color(255,120,175,10)); g2.drawLine(x,0,x,h); }
            for(int y=0;y<h;y+=spacing){ g2.setColor(new Color(255,190,210,8)); g2.drawLine(0,y,w,y); }

            int glowX=(int)(w*.78+Math.sin(phase)*30), glowY=(int)(h*.18+Math.cos(phase*.8)*18);
            for(int r=220;r>20;r-=25){
                g2.setColor(new Color(255,55,105,Math.max(2,22-r/12)));
                g2.fillOval(glowX-r,glowY-r,r*2,r*2);
            }

            for(Heart heart:hearts){
                heart.y -= heart.speed;
                heart.x += Math.sin(phase + heart.drift*4)*.00015f;
                if(heart.y < -.05f){ heart.y=1.05f; heart.x=random.nextFloat(); }
                drawHeart(g2,(int)(heart.x*w),(int)(heart.y*h),heart.size,
                        new Color(255,110,160,(int)(heart.alpha*255)));
            }
            g2.dispose();
        }

        private static void drawHeart(Graphics2D g2,int cx,int cy,float s,Color c){
            g2.setColor(c);
            java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
            p.moveTo(cx,cy+s*.9);
            p.curveTo(cx-s*1.15f,cy+s*.05f,cx-s*.75f,cy-s*.8f,cx,cy-s*.25f);
            p.curveTo(cx+s*.75f,cy-s*.8f,cx+s*1.15f,cy+.05f,cx,cy+s*.9);
            p.closePath();
            g2.fill(p);
        }
    }

    public static class GlassPanel extends JPanel {
        private final int arc;
        public GlassPanel(int arc){this.arc=arc;setOpaque(false);setBorder(BorderFactory.createEmptyBorder(16,18,16,18));}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth()-1,h=getHeight()-1;
            g2.setColor(new Color(35,8,23,232));
            g2.fill(new RoundRectangle2D.Double(0,0,w,h,arc,arc));
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255,93,151,65));
            g2.draw(new RoundRectangle2D.Double(.5,.5,w-1,h-1,arc,arc));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
