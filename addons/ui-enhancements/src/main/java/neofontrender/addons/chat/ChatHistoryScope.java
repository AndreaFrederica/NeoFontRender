package neofontrender.addons.chat;

import java.util.Locale;

final class ChatHistoryScope {
    private static final String SINGLEPLAYER_PREFIX = "singleplayer:";
    private static final String SERVER_PREFIX = "server:";

    private ChatHistoryScope() {}

    static String singleplayer(String folderName) {
        String folder = clean(folderName);
        return folder.isEmpty() ? null : SINGLEPLAYER_PREFIX + folder;
    }

    static String server(String address) {
        String normalized = clean(address).toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : SERVER_PREFIX + normalized;
    }

    static boolean valid(String scope) {
        return scope != null && (scope.startsWith(SINGLEPLAYER_PREFIX)
                && scope.length() > SINGLEPLAYER_PREFIX.length()
                || scope.startsWith(SERVER_PREFIX) && scope.length() > SERVER_PREFIX.length());
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
