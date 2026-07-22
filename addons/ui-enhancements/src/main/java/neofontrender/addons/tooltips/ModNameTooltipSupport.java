package neofontrender.addons.tooltips;

import java.util.List;
import java.util.Locale;

/** Pure formatting and duplicate-detection helpers kept independent of Forge event loading. */
final class ModNameTooltipSupport {
    private ModNameTooltipSupport() {}

    static boolean containsModName(List<String> tooltip, String modName) {
        if (tooltip == null || modName == null) return false;
        for (String line : tooltip) {
            String plain = stripFormatting(line);
            if (plain != null && modName.equals(plain.trim())) return true;
        }
        return false;
    }

    static String format(String friendlyFormat) {
        if (friendlyFormat == null || friendlyFormat.trim().isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (String token : friendlyFormat.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            char code = formattingCode(token);
            if (code != 0) result.append('\u00a7').append(code);
        }
        return result.toString();
    }

    private static String stripFormatting(String value) {
        if (value == null || value.indexOf('\u00a7') < 0) return value;
        StringBuilder plain = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\u00a7' && i + 1 < value.length() && isFormattingCode(value.charAt(i + 1))) {
                i++;
            } else {
                plain.append(current);
            }
        }
        return plain.toString();
    }

    private static boolean isFormattingCode(char value) {
        char code = Character.toLowerCase(value);
        return code >= '0' && code <= '9' || code >= 'a' && code <= 'f'
                || code >= 'k' && code <= 'o' || code == 'r';
    }

    private static char formattingCode(String name) {
        switch (name) {
            case "black": return '0';
            case "dark_blue": return '1';
            case "dark_green": return '2';
            case "dark_aqua": return '3';
            case "dark_red": return '4';
            case "dark_purple": return '5';
            case "gold": return '6';
            case "gray": return '7';
            case "dark_gray": return '8';
            case "blue": return '9';
            case "green": return 'a';
            case "aqua": return 'b';
            case "red": return 'c';
            case "light_purple": return 'd';
            case "yellow": return 'e';
            case "white": return 'f';
            case "obfuscated": return 'k';
            case "bold": return 'l';
            case "strikethrough": return 'm';
            case "underline": return 'n';
            case "italic": return 'o';
            default: return 0;
        }
    }
}
