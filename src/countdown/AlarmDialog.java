package countdown;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Full-screen-ish "It's time!" popup shown when a countdown is due. */
public class AlarmDialog extends JDialog {

    public AlarmDialog(Frame owner, EventItem item, Runnable onSnooze) {
        super(owner, "It's time!", false);
        getContentPane().setBackground(new Color(26, 10, 46));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel bell = label("IT'S TIME!", 40, Font.BOLD, Color.WHITE);
        JLabel open = label("Open the app!", 28, Font.BOLD, Color.WHITE);
        JLabel title = label(item.title, 22, Font.BOLD, new Color(255, 214, 10));
        String msg = (item.message == null || item.message.trim().isEmpty())
                ? "It's " + item.date.format(DateTimeFormatter.ofPattern("dd MMMM"))
                        + ", 12 o'clock! Open the app - your special day has arrived!"
                : item.message;
        JLabel message = label("<html><center>" + escapeHtml(msg) + "</center></html>",
                14, Font.PLAIN, Color.WHITE);
        JLabel time = label(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm:ss")),
                12, Font.PLAIN, new Color(187, 187, 187));

        JButton stop = new JButton("Stop alarm");
        stop.setBackground(new Color(255, 214, 10));
        JButton snooze = new JButton("Snooze 1 min (demo)");
        snooze.setForeground(Color.WHITE);
        snooze.setContentAreaFilled(false);

        stop.addActionListener(e -> {
            SoundHelper.stop();
            dispose();
        });
        snooze.addActionListener(e -> {
            SoundHelper.stop();
            dispose();
            Timer t = new Timer("snooze", true);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (item.soundEnabled) {
                        SoundHelper.playAlarm();
                    }
                    SwingUtilities.invokeLater(() -> onSnooze.run());
                }
            }, 60_000);
        });

        for (JLabel l : new JLabel[]{bell, open, title, message, time}) {
            l.setAlignmentX(CENTER_ALIGNMENT);
            body.add(l);
            body.add(javax.swing.Box.createVerticalStrut(10));
        }
        stop.setAlignmentX(CENTER_ALIGNMENT);
        snooze.setAlignmentX(CENTER_ALIGNMENT);
        body.add(stop);
        body.add(javax.swing.Box.createVerticalStrut(8));
        body.add(snooze);

        setContentPane(new JPanel(new BorderLayout()) {
            {
                setBackground(new Color(26, 10, 46));
                add(body, BorderLayout.CENTER);
            }
        });
        setSize(420, 520);
        setLocationRelativeTo(owner);
    }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font(Font.SANS_SERIF, style, size));
        l.setForeground(color);
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }
}
