package countdown;

import java.awt.BorderLayout;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

/** Password prompt that unlocks admin mode. */
public class LoginDialog extends JDialog {

    private boolean ok = false;
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");

    public LoginDialog(Frame owner, AuthService auth) {
        super(owner, "Admin login", true);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Admin login", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(20f).deriveFont(java.awt.Font.BOLD));
        body.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        JLabel hint = new JLabel(
                "<html><center>Enter the admin password to unlock editing.<br>Default password is admin123.</center></html>",
                SwingConstants.CENTER);
        center.add(hint, BorderLayout.NORTH);
        center.add(passwordField, BorderLayout.CENTER);
        errorLabel.setForeground(java.awt.Color.RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(errorLabel, BorderLayout.SOUTH);
        body.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton unlock = new JButton("Unlock admin mode");
        JButton cancel = new JButton("Stay as user");
        buttons.add(unlock);
        buttons.add(cancel);
        body.add(buttons, BorderLayout.SOUTH);

        unlock.addActionListener(e -> {
            if (auth.verify(new String(passwordField.getPassword()))) {
                ok = true;
                dispose();
            } else {
                errorLabel.setText("Wrong password, try again.");
            }
        });
        cancel.addActionListener(e -> dispose());
        passwordField.addActionListener(e -> unlock.doClick());

        setContentPane(body);
        pack();
        setMinimumSize(new java.awt.Dimension(360, 240));
        setLocationRelativeTo(owner);
    }

    public boolean isOk() {
        return ok;
    }
}
