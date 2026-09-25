package countdown;

import javax.swing.SwingUtilities;

/** Entry point: load events, open the main window. */
public class Main {

    public static void main(String[] args) {
        EventStore store = new EventStore();
        store.load();
        AuthService auth = new AuthService();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(store, auth);
            frame.setVisible(true);
        });
    }
}
