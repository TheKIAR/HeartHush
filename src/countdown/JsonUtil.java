package countdown;

import java.util.ArrayList;
import java.util.List;

/** Minimal JSON helpers: string escaping plus top-level object splitting. */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    public static String unquote(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            StringBuilder out = new StringBuilder();
            String inner = s.substring(1, s.length() - 1);
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '\\' && i + 1 < inner.length()) {
                    char n = inner.charAt(++i);
                    switch (n) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'u':
                            if (i + 4 < inner.length()) {
                                out.append((char) Integer.parseInt(inner.substring(i + 1, i + 5), 16));
                                i += 4;
                            }
                            break;
                        default: out.append(n); break;
                    }
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }
        return s;
    }

    /** Split the inside of a {...} or [...] on top-level commas. */
    public static List<String> splitTopLevel(String inside) {
        String s = inside.trim();
        if (s.startsWith("{") && s.endsWith("}") && isSingleBalanced(s)) {
            s = s.substring(1, s.length() - 1);
        }
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                cur.append(c);
                if (c == '\\' && i + 1 < s.length()) {
                    cur.append(s.charAt(++i));
                } else if (c == '"') {
                    inStr = false;
                }
            } else if (c == '"') {
                inStr = true;
                cur.append(c);
            } else if (c == '{' || c == '[') {
                depth++;
                cur.append(c);
            } else if (c == '}' || c == ']') {
                depth--;
                cur.append(c);
            } else if (c == ',' && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.toString().trim().length() > 0) {
            parts.add(cur.toString());
        }
        return parts;
    }

    /** True if the whole string is one balanced {...} or [...] object. */
    private static boolean isSingleBalanced(String s) {
        boolean inStr = false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inStr = false;
                }
            } else if (c == '"') {
                inStr = true;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0 && i != s.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0;
    }
}
