package countdown;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Main window. Two modes:
 *  - User: read-only list, no edit buttons.
 *  - Admin (password unlocked): Edit / Ring now / Delete, + Add, test sound.
 */
public class MainFrame extends JFrame {

    private final EventStore store;
    private final AuthService auth;
    private final Preferences fired = Preferences.userNodeForPackage(MainFrame.class);

    private final JLabel subtitleLabel = new JLabel();
    private final JLabel roleLabel = new JLabel();
    private final JButton authButton = new JButton();
    private final JButton passwordButton = new JButton("Change password");
    private final JButton addButton = new JButton("+ Add countdown");
    private final JLabel userHint = new JLabel("Read-only user view. Log in as admin to add countdowns.");
    private final JButton testSoundButton = new JButton("Test sound");
    private final JPanel listPanel = new JPanel();
    private final List<Row> rows = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "alarm-check");
        t.setDaemon(true);
        return t;
    });

    private static class Row {
        EventItem item;
        JLabel countdown;
        JLabel today;
        JPanel adminBar;
    }

    public MainFrame(EventStore store, AuthService auth) {
        super("Countdowns");
        this.store = store;
        this.auth = auth;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 700);
        setMinimumSize(new Dimension(460, 500));
        setLocationRelativeTo(null);

        // ---- top bar ----
        JLabel title = new JLabel("Countdowns");
        title.setFont(title.getFont().deriveFont(24f).deriveFont(Font.BOLD));
        subtitleLabel.setForeground(Color.GRAY);
        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.BOLD));
        JPanel titleBox = new JPanel(new BorderLayout(0, 2));
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitleLabel, BorderLayout.CENTER);
        titleBox.add(roleLabel, BorderLayout.SOUTH);

        authButton.addActionListener(e -> onAuthButton());
        passwordButton.addActionListener(e -> onChangePassword());
        JPanel authBox = new JPanel(new BorderLayout(0, 6));
        authBox.add(authButton, BorderLayout.NORTH);
        authBox.add(passwordButton, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        top.add(titleBox, BorderLayout.CENTER);
        top.add(authBox, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // ---- list ----
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(scroll, BorderLayout.CENTER);

        // ---- bottom bar ----
        addButton.setBackground(new Color(81, 43, 212));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> onAdd());
        testSoundButton.addActionListener(e -> {
            if (requireAdmin()) {
                SoundHelper.playAlarm();
            }
        });
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            checkAlarms();
            refresh();
        });
        userHint.setForeground(Color.GRAY);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bottom.add(addButton);
        bottom.add(userHint);
        bottom.add(testSoundButton);
        bottom.add(refreshButton);
        add(bottom, BorderLayout.SOUTH);

        // tick every second
        new Timer(1000, e -> tick()).start();

        // alarm poller: first run after 2s, then every 15s
        scheduler.scheduleAtFixedRate(this::checkAlarms, 2, 15, TimeUnit.SECONDS);

        refresh();
    }

    // ---------- roles ----------

    private void updateRoleUI() {
        boolean admin = auth.isAdmin();
        roleLabel.setText(admin ? "Admin view (can edit)" : "User view (read-only)");
        authButton.setText(admin ? "Logout" : "Admin login");
        passwordButton.setVisible(admin);
        addButton.setVisible(admin);
        testSoundButton.setVisible(admin);
        userHint.setVisible(!admin);
        for (Row r : rows) {
            r.adminBar.setVisible(admin);
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private boolean requireAdmin() {
        if (auth.isAdmin()) {
            return true;
        }
        JOptionPane.showMessageDialog(this, "Log in as admin to make changes.",
                "Admin only", JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    private void onAuthButton() {
        if (auth.isAdmin()) {
            auth.logout();
            updateRoleUI();
            return;
        }
        LoginDialog dlg = new LoginDialog(this, auth);
        dlg.setVisible(true);
        refresh();
    }

    private void onChangePassword() {
        if (!auth.isAdmin()) {
            return;
        }
        String current = JOptionPane.showInputDialog(this, "Enter current password:");
        if (current == null) {
            return;
        }
        String next = JOptionPane.showInputDialog(this, "Enter new password (min 4 chars):");
        if (next == null) {
            return;
        }
        if (auth.changePassword(current, next)) {
            JOptionPane.showMessageDialog(this, "Admin password changed.");
        } else {
            JOptionPane.showMessageDialog(this, "Wrong current password or new password too short.",
                    "Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- list ----------

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            boolean admin = auth.isAdmin();
            List<EventItem> sorted = store.sortedByNext(now.toLocalDate());
            rows.clear();
            listPanel.removeAll();
            for (EventItem e : sorted) {
                Row row = buildCard(e, admin);
                rows.add(row);
                listPanel.add(rowCard(row));
            }
            if (sorted.isEmpty()) {
                subtitleLabel.setText(admin
                        ? "Admin mode: click + Add to create the first countdown."
                        : "User mode: ask an admin to add countdowns.");
            } else {
                subtitleLabel.setText(sorted.size()
                        + " countdown(s) - rings at 12:00 AM - keep the app open on the day");
            }
            updateRoleUI();
            listPanel.revalidate();
            listPanel.repaint();
        });
    }

    private final java.util.Map<Row, JPanel> cardByRow = new java.util.HashMap<>();

    private Row buildCard(EventItem e, boolean admin) {
        Row r = new Row();
        r.item = e;
        r.countdown = new JLabel();
        r.countdown.setFont(r.countdown.getFont().deriveFont(18f).deriveFont(Font.BOLD));
        r.countdown.setForeground(new Color(81, 43, 212));
        r.today = new JLabel("TODAY");
        r.today.setFont(r.today.getFont().deriveFont(Font.BOLD));
        r.today.setForeground(new Color(193, 18, 31));

        JLabel title = new JLabel("<html><b>" + escapeHtml(e.title) + "</b>"
                + (e.featured ? "  *" : "") + "</html>");
        title.setFont(title.getFont().deriveFont(15f));
        JLabel date = new JLabel(e.dateLabel());
        date.setForeground(Color.GRAY);
        JTextArea message = new JTextArea(e.message == null ? "" : e.message);
        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setBackground(getBackground());
        message.setForeground(Color.GRAY);
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        StringBuilder tags = new StringBuilder();
        if (e.repeatYearly) {
            tags.append("yearly  ");
        }
        if (e.soundEnabled) {
            tags.append("sound  ");
        }
        JLabel tagLabel = new JLabel(tags.toString().trim());
        tagLabel.setForeground(Color.GRAY);

        JButton edit = new JButton("Edit");
        edit.addActionListener(ev -> onEdit(e));
        JButton ring = new JButton("Ring now");
        ring.setBackground(new Color(255, 214, 10));
        ring.addActionListener(ev -> onRing(e));
        JButton del = new JButton("Delete");
        del.setBackground(new Color(193, 18, 31));
        del.setForeground(Color.WHITE);
        del.addActionListener(ev -> onDelete(e));
        r.adminBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        r.adminBar.add(edit);
        r.adminBar.add(ring);
        r.adminBar.add(del);
        r.adminBar.setVisible(admin);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.add(title);
        card.add(date);
        card.add(r.countdown);
        card.add(message);
        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tagRow.add(tagLabel);
        tagRow.add(r.today);
        card.add(tagRow);
        card.add(r.adminBar);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        wrapper.add(card, BorderLayout.CENTER);
        cardByRow.put(r, wrapper);

        tickRow(r, LocalDateTime.now());
        return r;
    }

    private JPanel rowCard(Row r) {
        return cardByRow.get(r);
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        for (Row r : rows) {
            tickRow(r, now);
        }
    }

    private void tickRow(Row r, LocalDateTime now) {
        EventItem live = store.byId(r.item.id);
        if (live != null) {
            r.item = live;
        }
        r.countdown.setText(r.item.countdownText(now));
        r.today.setVisible(r.item.isDueToday(now.toLocalDate()));
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---------- actions ----------

    private void onAdd() {
        if (!requireAdmin()) {
            return;
        }
        EventItem item = new EventItem();
        item.title = "";
        item.date = LocalDate.now().plusDays(7);
        item.message = "";
        item.repeatYearly = true;
        item.soundEnabled = true;
        new EditDialog(this, store, item, true).setVisible(true);
        refresh();
    }

    private void onEdit(EventItem e) {
        if (!requireAdmin()) {
            return;
        }
        EventItem copy = new EventItem();
        copy.id = e.id;
        copy.title = e.title;
        copy.date = e.date;
        copy.message = e.message;
        copy.featured = e.featured;
        copy.repeatYearly = e.repeatYearly;
        copy.soundEnabled = e.soundEnabled;
        copy.createdAt = e.createdAt;
        EditDialog dlg = new EditDialog(this, store, copy, false);
        dlg.setVisible(true);
        refresh();
    }

    private void onDelete(EventItem e) {
        if (!requireAdmin()) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete '" + e.title + "'?", "Delete", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            store.delete(e.id);
            refresh();
        }
    }

    private void onRing(EventItem e) {
        if (!requireAdmin()) {
            return;
        }
        if (e.soundEnabled) {
            SoundHelper.playAlarm();
        }
        new AlarmDialog(this, e, () -> new AlarmDialog(this, e, () -> {
        }).setVisible(true)).setVisible(true);
    }

    // ---------- midnight alarm poller ----------

    private void checkAlarms() {
        try {
            LocalDate today = LocalDate.now();
            List<EventItem> due = new ArrayList<>();
            synchronized (store) {
                for (EventItem e : store.items()) {
                    if (!e.isDueToday(today)) {
                        continue;
                    }
                    String key = "fired_" + e.id + "_" + today;
                    if (fired.getBoolean(key, false)) {
                        continue;
                    }
                    fired.putBoolean(key, true);
                    due.add(e);
                }
            }
            for (EventItem e : due) {
                fire(e);
            }
        } catch (Exception ignored) {
        }
    }

    private void fire(EventItem e) {
        if (e.soundEnabled) {
            SoundHelper.playAlarm();
        }
        SwingUtilities.invokeLater(() ->
                new AlarmDialog(this, e, () -> new AlarmDialog(this, e, () -> {
                }).setVisible(true)).setVisible(true));
    }
}
