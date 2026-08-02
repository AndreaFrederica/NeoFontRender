package neofontrender.addons.chat;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class ChatRuleMatcher {
    private ChatRuleMatcher() {}

    static boolean matches(String expression, String text) {
        if (expression == null || expression.trim().isEmpty() || text == null) return false;
        try {
            return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(text).find();
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    static boolean containsName(String commaSeparatedNames, String playerName) {
        if (commaSeparatedNames == null || playerName == null || playerName.isEmpty()) return false;
        String expected = playerName.trim().toLowerCase(Locale.ROOT);
        for (String value : commaSeparatedNames.split(",")) {
            if (value.trim().toLowerCase(Locale.ROOT).equals(expected)) return true;
        }
        return false;
    }

    static String addName(String commaSeparatedNames, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()
                || containsName(commaSeparatedNames, playerName)) return commaSeparatedNames == null ? "" : commaSeparatedNames;
        String current = commaSeparatedNames == null ? "" : commaSeparatedNames.trim();
        return current.isEmpty() ? playerName.trim() : current + ", " + playerName.trim();
    }

    static boolean mentioned(String text, String localName) {
        if (text == null || localName == null || localName.isEmpty()) return false;
        return matches("(?<![\\p{L}\\p{N}_])@" + Pattern.quote(localName)
                + "(?![\\p{L}\\p{N}_])", text);
    }
}
