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

/** Add / edit form for one countdown. */
public class EditDialog extends JDialog {

    private boolean saved = false;
    private boolean deleted = false;

    public EditDialog(Frame owner, EventStore store, EventItem item, boolean isNew) {
        super(owner, isNew ? "Add countdown" : "Edit countdown", true);

        JTextField titleField = new JTextField(item.title, 24);
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(
                Date.from(item.date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                null, null, java.util.Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        JCheckBox yearlyBox = new JCheckBox("Repeat every year", item.repeatYearly);
        JCheckBox featuredBox = new JCheckBox("Featured (pin to top)", item.featured);
        JCheckBox soundBox = new JCheckBox("Sound + popup at 12:00 AM", item.soundEnabled);
        JTextArea messageArea = new JTextArea(item.message, 4, 24);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        form.add(new JLabel("Title:"));
        form.add(titleField);
        form.add(new JLabel("Date:"));
        form.add(dateSpinner);
        form.add(yearlyBox);
        form.add(featuredBox);
        form.add(soundBox);
        form.add(new JLabel("Alarm message (shown at midnight):"));
        form.add(new JScrollPane(messageArea));

        JPanel buttons = new JPanel();
        JButton save = new JButton("Save");
        JButton delete = new JButton("Delete");
        JButton cancel = new JButton("Cancel");
        delete.setVisible(!isNew);
        buttons.add(save);
        buttons.add(delete);
        buttons.add(cancel);

        save.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a title for the countdown.",
                        "Missing title", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Date d = (Date) dateSpinner.getValue();
            item.title = title;
            item.date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (item.date.isBefore(LocalDate.of(1900, 1, 1))) {
                item.date = LocalDate.now();
            }
            item.repeatYearly = yearlyBox.isSelected();
            item.featured = featuredBox.isSelected();
            item.soundEnabled = soundBox.isSelected();
            item.message = messageArea.getText().trim();
            store.addOrUpdate(item);
            saved = true;
            dispose();
        });
        delete.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete '" + item.title + "'?", "Delete", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                store.delete(item.id);
                deleted = true;
                dispose();
            }
        });
        cancel.addActionListener(e -> dispose());

        JPanel root = new JPanel(new BorderLayout());
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
