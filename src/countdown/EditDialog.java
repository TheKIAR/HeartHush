package countdown;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

public class EditDialog extends JDialog {
    public EditDialog(Frame owner, EventStore store, EventItem item, boolean isNew) {
        super(owner, isNew ? "Create countdown" : "Edit countdown", true);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(FuturisticUI.BG);

        JTextField titleField = new JTextField(item.title, 24);
        FuturisticUI.field(titleField);
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(
                Date.from(item.date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                null, null, java.util.Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));

        JCheckBox yearlyBox = new JCheckBox("Repeat every year", item.repeatYearly);
        JCheckBox featuredBox = new JCheckBox("Feature this countdown", item.featured);
        JCheckBox soundBox = new JCheckBox("Sound alert at midnight", item.soundEnabled);
        JCheckBox secretBox = new JCheckBox("Enable secret message at zero", item.secretEnabled);
        styleCheck(yearlyBox); styleCheck(featuredBox); styleCheck(soundBox); styleCheck(secretBox);

        JTextArea publicArea = area(item.message, 3);
        JTextArea secretArea = area(item.secretMessage, 7);

        JPanel form = new JPanel(new GridLayout(0, 1, 7, 7));
        form.setBackground(FuturisticUI.PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 10, 18));
        form.add(FuturisticUI.label("COUNTDOWN TITLE", 11f, FuturisticUI.MUTED, java.awt.Font.BOLD));
        form.add(titleField);
        form.add(FuturisticUI.label("TARGET DATE", 11f, FuturisticUI.MUTED, java.awt.Font.BOLD));
        form.add(dateSpinner);
        form.add(yearlyBox);
        form.add(featuredBox);
        form.add(soundBox);
        form.add(secretBox);
        form.add(FuturisticUI.label("PUBLIC / ALARM MESSAGE", 11f, FuturisticUI.MUTED, java.awt.Font.BOLD));
        form.add(new JScrollPane(publicArea));
        form.add(FuturisticUI.label("SECRET MESSAGE (ADMIN ONLY)", 11f, FuturisticUI.CYAN, java.awt.Font.BOLD));
        form.add(new JScrollPane(secretArea));

        JPanel buttons = new JPanel();
        buttons.setBackground(FuturisticUI.PANEL);
        JButton save = new JButton("SAVE COUNTDOWN");
        FuturisticUI.button(save, FuturisticUI.PURPLE);
        JButton delete = new JButton("DELETE");
        FuturisticUI.button(delete, FuturisticUI.RED);
        JButton cancel = new JButton("CANCEL");
        FuturisticUI.ghostButton(cancel);
        delete.setVisible(!isNew);
        buttons.add(save); buttons.add(delete); buttons.add(cancel);

        save.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a countdown title.");
                return;
            }
            Date d = (Date) dateSpinner.getValue();
            item.title = title;
            item.date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (item.date.isBefore(LocalDate.of(1900, 1, 1))) item.date = LocalDate.now();
            item.repeatYearly = yearlyBox.isSelected();
            item.featured = featuredBox.isSelected();
            item.soundEnabled = soundBox.isSelected();
            item.secretEnabled = secretBox.isSelected();
            item.message = publicArea.getText().trim();
            item.secretMessage = secretArea.getText();
            if (!item.secretEnabled) item.secretMessage = "";
            store.addOrUpdate(item);
            dispose();
        });

        delete.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Delete '" + item.title + "'?",
                    "Delete countdown", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                store.delete(item.id);
                dispose();
            }
        });
        cancel.addActionListener(e -> dispose());

        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(520, 680);
        setLocationRelativeTo(owner);
    }

    private static JTextArea area(String value, int rows) {
        JTextArea a = new JTextArea(value == null ? "" : value, rows, 24);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setForeground(FuturisticUI.TEXT);
        a.setBackground(new java.awt.Color(8, 12, 22));
        a.setCaretColor(FuturisticUI.CYAN);
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return a;
    }

    private static void styleCheck(JCheckBox b) {
        b.setOpaque(false);
        b.setForeground(FuturisticUI.TEXT);
        b.setFocusPainted(false);
    }
}
