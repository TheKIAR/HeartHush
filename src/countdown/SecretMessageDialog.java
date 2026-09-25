package countdown;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

public class SecretMessageDialog extends JDialog {
    public SecretMessageDialog(Frame owner, EventItem item) {
        super(owner, "A secret message is waiting", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(560, 430);
        setMinimumSize(new Dimension(480, 360));
        setLocationRelativeTo(owner);

        FuturisticUI.GridBackground bg = new FuturisticUI.GridBackground();
        bg.setLayout(new BorderLayout());
        JPanel glass = new FuturisticUI.GlassPanel(28);
        glass.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1;
        c.insets = new Insets(6, 10, 6, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel icon = FuturisticUI.label("◆", 34f, FuturisticUI.CYAN, Font.BOLD);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 0; glass.add(icon, c);

        JLabel title = FuturisticUI.label("YOU HAVE A SECRET MESSAGE", 22f, FuturisticUI.TEXT, Font.BOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 1; glass.add(title, c);

        JLabel hint = FuturisticUI.label(
                "The countdown has reached zero. Would you like to open your message?",
                13f, FuturisticUI.MUTED, Font.PLAIN);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 2; glass.add(hint, c);

        JTextArea message = new JTextArea(item.secretMessage == null ? "" : item.secretMessage);
        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setForeground(FuturisticUI.TEXT);
        message.setBackground(new Color(8, 12, 22));
        message.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        message.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        message.setVisible(false);

        JPanel messageBox = new JPanel(new BorderLayout());
        messageBox.setOpaque(false);
        messageBox.add(new JScrollPane(message), BorderLayout.CENTER);
        c.gridy = 3; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        glass.add(messageBox, c);

        JButton open = new JButton("OPEN MESSAGE");
        FuturisticUI.button(open, FuturisticUI.PURPLE);
        open.addActionListener(e -> {
            message.setVisible(true);
            open.setText("MESSAGE OPENED");
            open.setEnabled(false);
            hint.setText("This message was written by the admin for this special day.");
            glass.revalidate();
            glass.repaint();
        });
        c.gridy = 4; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        glass.add(open, c);

        JButton close = new JButton("CLOSE");
        FuturisticUI.ghostButton(close);
        close.addActionListener(e -> dispose());
        c.gridy = 5; glass.add(close, c);

        bg.add(glass, BorderLayout.CENTER);
        setContentPane(bg);
    }
}
