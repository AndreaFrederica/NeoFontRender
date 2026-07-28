package neofontrender.addons.chat;

import java.util.Map;

final class ChatPlayerNameMatcher {
    private ChatPlayerNameMatcher() {}

    static <T> T find(String message, Map<String, T> names) {
        if (message == null || message.isEmpty() || names.isEmpty()) return null;
        int bestIndex = Integer.MAX_VALUE;
        int bestLength = -1;
        T best = null;
        for (Map.Entry<String, T> entry : names.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.isEmpty()) continue;
            int from = 0;
            while (from <= message.length() - name.length()) {
                int index = indexOfIgnoreCase(message, name, from);
                if (index < 0) break;
                int end = index + name.length();
                if ((index == 0 || !isWord(message.charAt(index - 1)))
                        && (end == message.length() || !isWord(message.charAt(end)))) {
                    if (index < bestIndex || index == bestIndex && name.length() > bestLength) {
                        bestIndex = index;
                        bestLength = name.length();
                        best = entry.getValue();
                    }
                    break;
                }
                from = index + 1;
            }
        }
        return best;
    }

    private static int indexOfIgnoreCase(String value, String needle, int from) {
        int limit = value.length() - needle.length();
        for (int i = from; i <= limit; i++) {
            if (value.regionMatches(true, i, needle, 0, needle.length())) return i;
        }
        return -1;
    }

    private static boolean isWord(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }
}
