package countdown;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

public class LoginDialog extends JDialog {
    private final JPasswordField passwordField = new JPasswordField(20);

    public LoginDialog(Frame owner, AuthService auth) {
        super(owner, "Admin access", true);
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(FuturisticUI.BG);
        body.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        JLabel title = FuturisticUI.label("ADMIN ACCESS", 22f, FuturisticUI.TEXT, Font.BOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        JLabel hint = FuturisticUI.label("Enter the administrator password to unlock controls.", 12f,
                FuturisticUI.MUTED, Font.PLAIN);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(hint, BorderLayout.NORTH);
        FuturisticUI.field(passwordField);
        center.add(passwordField, BorderLayout.CENTER);

        JLabel error = FuturisticUI.label(" ", 12f, FuturisticUI.RED, Font.PLAIN);
        error.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(error, BorderLayout.SOUTH);
        body.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton unlock = new JButton("UNLOCK");
        FuturisticUI.button(unlock, FuturisticUI.PURPLE);
        JButton cancel = new JButton("CANCEL");
        FuturisticUI.ghostButton(cancel);
        buttons.add(unlock); buttons.add(cancel);
        body.add(buttons, BorderLayout.SOUTH);

        unlock.addActionListener(e -> {
            if (auth.verify(new String(passwordField.getPassword()))) dispose();
            else error.setText("Access denied.");
        });
        cancel.addActionListener(e -> dispose());
        passwordField.addActionListener(e -> unlock.doClick());

        setContentPane(body);
        setSize(new Dimension(400, 240));
        setLocationRelativeTo(owner);
    }
}
