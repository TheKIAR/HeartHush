package countdown;

import java.awt.BorderLayout;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class MainFrame extends JFrame {
    private final EventStore store;
    private final AuthService auth;
    private final Preferences fired = Preferences.userNodeForPackage(MainFrame.class);
    private final JLabel clockLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JButton authButton = new JButton();
    private final JButton addButton = new JButton("+ NEW COUNTDOWN");
    private final JButton testSoundButton = new JButton("TEST ALERT");
    private final JPanel listPanel = new JPanel();
    private final List<Row> rows = new ArrayList<>();
    private final java.util.Set<String> secretShownThisSession = new java.util.HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "alarm-check");
        t.setDaemon(true);
        return t;
    });

    private static class Row {
        EventItem item;
        JLabel countdown;
        JLabel badge;
        JPanel adminBar;
    }

    public MainFrame(EventStore store, AuthService auth) {
        super("Special Count • Countdown");
        this.store = store;
        this.auth = auth;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(760, 760);
        setMinimumSize(new Dimension(620, 620));
        setLocationRelativeTo(null);
        FuturisticUI.frame(this);

        FuturisticUI.GridBackground root = new FuturisticUI.GridBackground();
        root.setLayout(new BorderLayout());
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(22, 24, 12, 24));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel title = FuturisticUI.label("SPECIAL COUNT", 28f, FuturisticUI.TEXT, Font.BOLD);
        JLabel sub = FuturisticUI.label("A smart countdown for moments that matter.", 12f, FuturisticUI.MUTED, Font.PLAIN);
        brand.add(title); brand.add(Box.createVerticalStrut(4)); brand.add(sub);
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(0, 5));
        right.setOpaque(false);
        clockLabel.setHorizontalAlignment(JLabel.RIGHT);
        clockLabel.setFont(clockLabel.getFont().deriveFont(Font.BOLD, 18f));
        clockLabel.setForeground(FuturisticUI.CYAN);
        statusLabel.setHorizontalAlignment(JLabel.RIGHT);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 11f));
        right.add(clockLabel, BorderLayout.NORTH);
        right.add(statusLabel, BorderLayout.SOUTH);
        header.add(right, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(6, 22, 6, 22));
        root.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 10));
        bottom.setOpaque(false);
        FuturisticUI.button(addButton, FuturisticUI.PURPLE);
        FuturisticUI.ghostButton(testSoundButton);
        JButton refresh = new JButton("REFRESH");
        FuturisticUI.ghostButton(refresh);
        authButton.addActionListener(e -> onAuth());
        addButton.addActionListener(e -> onAdd());
        testSoundButton.addActionListener(e -> { if (requireAdmin()) SoundHelper.playAlarm(); });
        refresh.addActionListener(e -> { checkAlarms(); refresh(); });
        bottom.add(addButton); bottom.add(testSoundButton); bottom.add(refresh); bottom.add(authButton);
        root.add(bottom, BorderLayout.SOUTH);

        new Timer(1000, e -> tick()).start();
        scheduler.scheduleAtFixedRate(this::checkAlarms, 2, 15, TimeUnit.SECONDS);
        refresh();
        SwingUtilities.invokeLater(this::showDueSecretsOnLaunch);
    }

    private void onAuth() {
        if (auth.isAdmin()) {
            auth.logout();
            updateRoleUI();
        } else {
            new LoginDialog(this, auth).setVisible(true);
            refresh();
        }
    }

    private boolean requireAdmin() {
        if (auth.isAdmin()) return true;
        javax.swing.JOptionPane.showMessageDialog(this, "Admin access is required for that action.");
        return false;
    }

    private void updateRoleUI() {
        boolean admin = auth.isAdmin();
        authButton.setText(admin ? "LOG OUT" : "ADMIN LOGIN");
        FuturisticUI.ghostButton(authButton);
        addButton.setVisible(admin);
        testSoundButton.setVisible(admin);
        for (Row r : rows) r.adminBar.setVisible(admin);
        statusLabel.setText(admin ? "● ADMIN MODE" : "● LIVE • READ ONLY");
        statusLabel.setForeground(admin ? FuturisticUI.GREEN : FuturisticUI.MUTED);
        listPanel.revalidate();
        listPanel.repaint();
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            rows.clear();
            listPanel.removeAll();
            List<EventItem> sorted = store.sortedByNext(LocalDate.now());
            for (EventItem e : sorted) {
                Row r = buildCard(e);
                rows.add(r);
                listPanel.add(r.adminBar.getParent());
                listPanel.add(Box.createVerticalStrut(10));
            }
            updateRoleUI();
            listPanel.revalidate();
            listPanel.repaint();
        });
    }

    private Row buildCard(EventItem e) {
        Row r = new Row();
        r.item = e;
        r.countdown = FuturisticUI.label("", 26f, FuturisticUI.CYAN, Font.BOLD);
        r.badge = FuturisticUI.label(e.isDueToday(LocalDate.now()) ? "  COUNTDOWN COMPLETE  " : "  COUNTING DOWN  ",
                10f, e.isDueToday(LocalDate.now()) ? FuturisticUI.GREEN : FuturisticUI.MUTED, Font.BOLD);

        FuturisticUI.GlassPanel card = new FuturisticUI.GlassPanel(24);
        card.setLayout(new BorderLayout(12, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = FuturisticUI.label(e.title, 18f, FuturisticUI.TEXT, Font.BOLD);
        JLabel date = FuturisticUI.label(e.dateLabel(), 11f, FuturisticUI.MUTED, Font.PLAIN);
        JLabel msg = FuturisticUI.label(e.message == null || e.message.isEmpty() ? "No public message" : e.message,
                11f, FuturisticUI.MUTED, Font.PLAIN);
        left.add(title); left.add(Box.createVerticalStrut(5)); left.add(date);
        left.add(Box.createVerticalStrut(9)); left.add(msg);
        if (e.hasSecret()) {
            JLabel secret = FuturisticUI.label("◆ SECRET MESSAGE ARMED", 10f, FuturisticUI.PURPLE, Font.BOLD);
            left.add(Box.createVerticalStrut(8)); left.add(secret);
        }
        card.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        r.countdown.setHorizontalAlignment(JLabel.RIGHT);
        r.badge.setHorizontalAlignment(JLabel.RIGHT);
        right.add(r.badge); right.add(Box.createVerticalStrut(7)); right.add(r.countdown);

        r.adminBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 4));
        r.adminBar.setOpaque(false);
        JButton edit = new JButton("EDIT");
        JButton ring = new JButton("RING");
        JButton del = new JButton("DELETE");
        FuturisticUI.ghostButton(edit); FuturisticUI.ghostButton(ring); FuturisticUI.button(del, FuturisticUI.RED);
        edit.addActionListener(x -> onEdit(e));
        ring.addActionListener(x -> onRing(e));
        del.addActionListener(x -> onDelete(e));
        r.adminBar.add(edit); r.adminBar.add(ring); r.adminBar.add(del);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
        outer.add(card, BorderLayout.CENTER);
        outer.add(r.adminBar, BorderLayout.SOUTH);
        r.adminBar.setVisible(auth.isAdmin());

        tickRow(r, LocalDateTime.now());
        return r;
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        clockLabel.setText(now.format(java.time.format.DateTimeFormatter.ofPattern("EEE • dd MMM • HH:mm:ss")));
        for (Row r : rows) tickRow(r, now);
    }

    private void tickRow(Row r, LocalDateTime now) {
        EventItem live = store.byId(r.item.id);
        if (live != null) r.item = live;
        boolean due = r.item.isDueToday(now.toLocalDate());
        r.countdown.setText(r.item.countdownText(now));
        r.badge.setText(due ? "  COUNTDOWN COMPLETE  " : "  COUNTING DOWN  ");
        r.badge.setForeground(due ? FuturisticUI.GREEN : FuturisticUI.MUTED);
    }

    private void onAdd() {
        if (!requireAdmin()) return;
        EventItem item = new EventItem();
        item.date = LocalDate.now().plusDays(7);
        item.repeatYearly = true;
        item.soundEnabled = true;
        new EditDialog(this, store, item, true).setVisible(true);
        refresh();
    }

    private void onEdit(EventItem e) {
        if (!requireAdmin()) return;
        EventItem copy = EventItem.fromJson(e.toJson());
        new EditDialog(this, store, copy, false).setVisible(true);
        refresh();
    }

    private void onDelete(EventItem e) {
        if (!requireAdmin()) return;
        int ok = javax.swing.JOptionPane.showConfirmDialog(this, "Delete '" + e.title + "'?",
                "Delete countdown", javax.swing.JOptionPane.YES_NO_OPTION);
        if (ok == javax.swing.JOptionPane.YES_OPTION) {
            store.delete(e.id);
            refresh();
        }
    }

    private void onRing(EventItem e) {
        if (!requireAdmin()) return;
        if (e.soundEnabled) SoundHelper.playAlarm();
        if (e.hasSecret()) new SecretMessageDialog(this, e).setVisible(true);
        else new AlarmDialog(this, e, () -> {}).setVisible(true);
    }

    private void showDueSecretsOnLaunch() {
        LocalDate today = LocalDate.now();
        for (EventItem e : store.items()) {
            if (e.isDueToday(today) && e.hasSecret() && !secretShownThisSession.contains(e.id)) {
                secretShownThisSession.add(e.id);
                new SecretMessageDialog(this, e).setVisible(true);
            }
        }
    }

    private void checkAlarms() {
        try {
            LocalDate today = LocalDate.now();
            List<EventItem> due = new ArrayList<>();
            synchronized (store) {
                for (EventItem e : store.items()) {
                    if (!e.isDueToday(today)) continue;
                    String key = "fired_" + e.id + "_" + today;
                    if (fired.getBoolean(key, false)) continue;
                    fired.putBoolean(key, true);
                    due.add(e);
                }
            }
            for (EventItem e : due) fire(e);
        } catch (Exception ignored) {}
    }

    private void fire(EventItem e) {
        if (e.hasSecret()) {
            SwingUtilities.invokeLater(() -> {
                if (!secretShownThisSession.contains(e.id)) {
                    secretShownThisSession.add(e.id);
                    new SecretMessageDialog(this, e).setVisible(true);
                }
            });
        } else {
            if (e.soundEnabled) SoundHelper.playAlarm();
            SwingUtilities.invokeLater(() -> new AlarmDialog(this, e, () -> {}).setVisible(true));
        }
    }
}
