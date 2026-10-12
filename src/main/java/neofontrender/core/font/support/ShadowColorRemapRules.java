package neofontrender.core.font.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Immutable user rules that remap colored-shadow RGB values without changing foreground text. */
public final class ShadowColorRemapRules {
    private static final ShadowColorRemapRules EMPTY = new ShadowColorRemapRules(Collections.emptyList());

    private final List<Rule> rules;
    private final String config;

    private ShadowColorRemapRules(List<Rule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        StringBuilder value = new StringBuilder();
        for (Rule rule : rules) {
            if (value.length() > 0) value.append(';');
            value.append(rule.slot >= 0 ? "slot:" + Integer.toHexString(rule.slot) :
                    String.format(Locale.ROOT, "rgb:%06X", rule.sourceRgb));
            value.append('=').append(String.format(Locale.ROOT, "%06X", rule.targetRgb));
        }
        this.config = value.toString();
    }

    /**
     * Parses semicolon, comma, or whitespace separated rules. Sources are {@code rgb:RRGGBB},
     * {@code #RRGGBB}, or {@code slot:0} through {@code slot:f}; targets are RGB colors.
     */
    public static ShadowColorRemapRules parse(String value) {
        if (value == null || value.trim().isEmpty()) return EMPTY;
        List<Rule> parsed = new ArrayList<>();
        String compact = value.trim().replaceAll("\\s*([=>])\\s*", "$1");
        for (String token : compact.split("[,;\\s]+")) {
            int separator = token.indexOf('=');
            if (separator < 0) separator = token.indexOf('>');
            if (separator <= 0 || separator >= token.length() - 1) continue;
            String source = token.substring(0, separator).trim();
            String target = token.substring(separator + 1).trim();
            int targetRgb = parseRgb(target);
            if (targetRgb < 0) continue;
            int slot = parseSlot(source);
            if (slot >= 0) {
                parsed.add(new Rule(slot, -1, targetRgb));
                continue;
            }
            int sourceRgb = parseRgb(stripRgbPrefix(source));
            if (sourceRgb >= 0) parsed.add(new Rule(-1, sourceRgb, targetRgb));
        }
        return parsed.isEmpty() ? EMPTY : new ShadowColorRemapRules(parsed);
    }

    /** Applies the first matching rule, preserving the candidate shadow alpha channel. */
    public int remap(int foregroundArgb, int[] palette) {
        return remap(foregroundArgb, foregroundArgb, palette);
    }

    /** Applies the first matching rule to a candidate shadow while matching the foreground RGB. */
    public int remap(int foregroundArgb, int shadowArgb, int[] palette) {
        int sourceRgb = foregroundArgb & 0xFFFFFF;
        for (Rule rule : rules) {
            if (rule.matches(sourceRgb, palette)) {
                return shadowArgb & 0xFF000000 | rule.targetRgb;
            }
        }
        return shadowArgb;
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /** Canonical, user-editable representation suitable for the TOML string value. */
    public String toConfigString() {
        return config;
    }

    /** Stable cache discriminator for rendered modern-shadow textures. */
    public int profileHash() {
        return config.hashCode();
    }

    private static String stripRgbPrefix(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.regionMatches(true, 0, "rgb:", 0, 4)) return normalized.substring(4);
        return normalized;
    }

    private static int parseRgb(String value) {
        String normalized = stripRgbPrefix(value);
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) normalized = normalized.substring(2);
        if (!normalized.matches("(?i)[0-9a-f]{6}")) return -1;
        return Integer.parseInt(normalized, 16);
    }

    private static int parseSlot(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("slot:")) normalized = normalized.substring(5);
        else if (normalized.startsWith("§")) normalized = normalized.substring(1);
        else return -1;
        if (normalized.length() != 1) return -1;
        return Character.digit(normalized.charAt(0), 16);
    }

    private static final class Rule {
        private final int slot;
        private final int sourceRgb;
        private final int targetRgb;

        private Rule(int slot, int sourceRgb, int targetRgb) {
            this.slot = slot;
            this.sourceRgb = sourceRgb;
            this.targetRgb = targetRgb;
        }

        private boolean matches(int rgb, int[] palette) {
            if (slot < 0) return rgb == sourceRgb;
            return palette != null && slot < palette.length && rgb == (palette[slot] & 0xFFFFFF);
        }
    }
}
