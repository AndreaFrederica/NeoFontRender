package neofontrender.core.font.pipeline.builtin;

import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.StructuredTextRewriter;

import java.util.ArrayList;
import java.util.List;

/** Converts the laboratory hex and gradient syntax into structured color spans. */
public final class HexChatStructuredMiddleware implements StructuredTextMiddleware {
    public static final HexChatStructuredMiddleware INSTANCE =
            new HexChatStructuredMiddleware();
    public static final String ID = "neofontrender:hex_chat";

    private HexChatStructuredMiddleware() {}

    @Override public String id() { return ID; }
    @Override public int priority() { return 90; }
    @Override public boolean isEnabled() { return NeofontrenderConfig.laboratoryHexChat(); }

    @Override
    public StructuredText process(StructuredText input) {
        return process(input, NeofontrenderConfig.laboratoryHexChatResetStyles());
    }

    static StructuredText process(StructuredText input, boolean resetStyles) {
        String plain = input.plainText();
        List<StructuredTextRewriter.Replacement> replacements = new ArrayList<>();
        for (int marker = 0; marker < plain.length();) {
            int markerLength = markerLength(plain, marker);
            if (markerLength == 0) {
                marker += Character.charCount(plain.codePointAt(marker));
                continue;
            }
            int[] colors = markerColors(plain, marker);
            int contentStart = marker + markerLength;
            int contentEnd = contentStart;
            while (contentEnd < plain.length()) {
                if (markerLength(plain, contentEnd) > 0) break;
                if (hasColorOrResetBefore(input, contentEnd)) break;
                contentEnd += Character.charCount(plain.codePointAt(contentEnd));
            }
            int characterCount = plain.codePointCount(contentStart, contentEnd);
            replacements.add(StructuredTextRewriter.text(marker, contentStart, "",
                    style -> style));
            if (characterCount > 0) {
                int cursor = contentStart;
                for (int character = 0; cursor < contentEnd; character++) {
                    int next = cursor + Character.charCount(plain.codePointAt(cursor));
                    int rgb = interpolate(colors, character, characterCount);
                    TextStyle style = resetStyles
                            ? localStyle(input, contentStart, cursor).withColorOverride(rgb)
                            : input.styleAt(cursor).withColorOverride(rgb);
                    String emitted = plain.substring(cursor, next);
                    replacements.add(StructuredTextRewriter.text(cursor, next, emitted,
                            ignored -> style));
                    cursor = next;
                }
            }
            marker = contentEnd;
        }
        return StructuredTextRewriter.rewrite(input, replacements, ID);
    }

    static int markerLength(String text, int index) {
        if (text == null || index < 0 || index >= text.length() || text.charAt(index) != '#'
                || index + 6 >= text.length()) return 0;
        int cursor = index + 1;
        while (cursor + 5 < text.length()) {
            if (!isHex6(text, cursor)) return cursor == index + 1 ? 0 : cursor - index;
            cursor += 6;
            if (cursor >= text.length() || text.charAt(cursor) != '-'
                    || !isHex6(text, cursor + 1)) break;
            cursor++;
        }
        return cursor == index + 1 ? 0 : cursor - index;
    }

    static int[] markerColors(String text, int index) {
        int length = markerLength(text, index);
        if (length == 0) return new int[0];
        String[] parts = text.substring(index + 1, index + length).split("-");
        int[] colors = new int[parts.length];
        for (int i = 0; i < parts.length; i++) colors[i] = Integer.parseInt(parts[i], 16);
        return colors;
    }

    static int interpolate(int[] colors, int index, int totalLength) {
        if (colors.length == 0) return 0xFFFFFF;
        if (colors.length == 1 || totalLength <= 1) return colors[0];
        float position = (colors.length - 1) * Math.max(0, Math.min(totalLength - 1, index))
                / (float) (totalLength - 1);
        int before = Math.min(colors.length - 1, (int) position);
        int after = Math.min(colors.length - 1, before + 1);
        float fraction = position - before;
        return mix(colors[before] >> 16 & 255, colors[after] >> 16 & 255, fraction) << 16
                | mix(colors[before] >> 8 & 255, colors[after] >> 8 & 255, fraction) << 8
                | mix(colors[before] & 255, colors[after] & 255, fraction);
    }

    private static int mix(int before, int after, float fraction) {
        return Math.round(before * (1.0F - fraction) + after * fraction);
    }

    private static boolean isHex6(String text, int index) {
        if (index < 0 || index + 6 > text.length()) return false;
        for (int digit = index; digit < index + 6; digit++) {
            if (Character.digit(text.charAt(digit), 16) < 0) return false;
        }
        return true;
    }

    private static boolean hasColorOrResetBefore(StructuredText text, int plainBoundary) {
        int start = text.sourceMap().sourceStart(plainBoundary);
        int end = text.sourceMap().sourceEnd(plainBoundary);
        String source = text.sourceText();
        for (int index = start; index + 1 < end; index++) {
            if (source.charAt(index) != '\u00A7') continue;
            char code = Character.toLowerCase(source.charAt(index + 1));
            if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f' || code == 'r') {
                return true;
            }
        }
        return false;
    }

    private static TextStyle localStyle(StructuredText text, int start, int boundary) {
        TextStyle result = TextStyle.DEFAULT;
        int rawStart = text.sourceMap().sourceEnd(start);
        int rawEnd = text.sourceMap().sourceEnd(boundary);
        String source = text.sourceText();
        for (int index = rawStart; index + 1 < rawEnd; index++) {
            if (source.charAt(index) != '\u00A7') continue;
            char code = Character.toLowerCase(source.charAt(++index));
            if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f' || code == 'r') {
                result = TextStyle.DEFAULT;
            } else if (code == 'k') result = result.withObfuscated(true);
            else if (code == 'l') result = result.withBold(true);
            else if (code == 'm') result = result.withStrikethrough(true);
            else if (code == 'n') result = result.withUnderline(true);
            else if (code == 'o') result = result.withItalic(true);
        }
        return result;
    }
}
