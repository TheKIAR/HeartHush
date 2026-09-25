package countdown;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

public class PasswordDialog extends JDialog {
    public PasswordDialog(Frame owner, AuthService auth) {
        super(owner,"Admin Password Settings",true);
        JPanel root=new JPanel(new BorderLayout(0,12));
        root.setBackground(FuturisticUI.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18,20,18,20));

        JLabel title=FuturisticUI.label("ADMIN PASSWORD",20f,FuturisticUI.TEXT,java.awt.Font.BOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(title,BorderLayout.NORTH);

        JPanel form=new JPanel(new GridLayout(0,1,7,7));
        form.setOpaque(false);
        JPasswordField current=new JPasswordField();
        JPasswordField next=new JPasswordField();
        JPasswordField confirm=new JPasswordField();
        FuturisticUI.field(current);FuturisticUI.field(next);FuturisticUI.field(confirm);
        form.add(FuturisticUI.label("CURRENT PASSWORD",11f,FuturisticUI.MUTED,java.awt.Font.BOLD));form.add(current);
        form.add(FuturisticUI.label("NEW PASSWORD (4+ CHARACTERS)",11f,FuturisticUI.MUTED,java.awt.Font.BOLD));form.add(next);
        form.add(FuturisticUI.label("CONFIRM NEW PASSWORD",11f,FuturisticUI.MUTED,java.awt.Font.BOLD));form.add(confirm);
        root.add(form,BorderLayout.CENTER);

        JPanel buttons=new JPanel();
        buttons.setOpaque(false);
        JButton save=new JButton("CHANGE PASSWORD");FuturisticUI.button(save,FuturisticUI.ROSE);
        JButton cancel=new JButton("CANCEL");FuturisticUI.ghostButton(cancel);
        buttons.add(save);buttons.add(cancel);root.add(buttons,BorderLayout.SOUTH);

        save.addActionListener(e->{
            String n=new String(next.getPassword()), c=new String(confirm.getPassword());
            if(!n.equals(c)){JOptionPane.showMessageDialog(this,"New passwords do not match.");return;}
            if(auth.changePassword(new String(current.getPassword()),n)){
                JOptionPane.showMessageDialog(this,"Admin password updated.");
                dispose();
            }else JOptionPane.showMessageDialog(this,"Current password is incorrect, or the new password is too short.");
        });
        cancel.addActionListener(e->dispose());
        setContentPane(root);setSize(430,330);setLocationRelativeTo(owner);
    }
}
