package neofontrender.addons.chat.network;

/** Shared, conservative chat character policy. The server is authoritative for section signs. */
public final class ChatCharacterPolicy {
    private static volatile boolean serverAllowsSectionSign;
    private ChatCharacterPolicy() {}
    public static void configureServer(boolean allow) { serverAllowsSectionSign = allow; }
    public static boolean serverAllowsSectionSign() { return serverAllowsSectionSign; }
    public static boolean isAllowed(char value, boolean sectionSignAllowed) {
        return value == '\u00A7' ? sectionSignAllowed : value >= 32 && value != 127;
    }
    public static boolean containsSectionSign(String value) { return value != null && value.indexOf('\u00A7') >= 0; }

    public static boolean isValid(String value, boolean sectionSignAllowed) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!isAllowed(value.charAt(i), sectionSignAllowed)) return false;
        }
        return true;
    }

    public static String filter(String value, boolean sectionSignAllowed) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (isAllowed(c, sectionSignAllowed)) result.append(c);
        }
        return result.toString();
    }
}
