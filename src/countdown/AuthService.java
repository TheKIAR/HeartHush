package countdown;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.prefs.Preferences;

/**
 * Role gate: the app starts in User (view-only) mode.
 * Admin (full edit) mode is unlocked with a password.
 * Default password: "admin123" - change it from the admin menu.
 */
public class AuthService {

    private static final String HASH_KEY = "admin_password_hash";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final Preferences prefs = Preferences.userNodeForPackage(AuthService.class);
    private boolean admin = false;

    public boolean isAdmin() {
        return admin;
    }

    public boolean verify(String password) {
        String want = prefs.get(HASH_KEY, hash(DEFAULT_PASSWORD));
        boolean ok = MessageDigest.isEqual(bytes(hash(password == null ? "" : password)), bytes(want));
        if (ok) {
            admin = true;
        }
        return ok;
    }

    public void logout() {
        admin = false;
    }

    public boolean changePassword(String current, String next) {
        String want = prefs.get(HASH_KEY, hash(DEFAULT_PASSWORD));
        if (!MessageDigest.isEqual(bytes(hash(current == null ? "" : current)), bytes(want))) {
            return false;
        }
        if (next == null || next.length() < 4) {
            return false;
        }
        prefs.put(HASH_KEY, hash(next));
        return true;
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("countdown-app|" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
