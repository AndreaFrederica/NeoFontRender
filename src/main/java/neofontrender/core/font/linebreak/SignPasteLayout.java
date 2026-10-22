package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Distributes pasted text across the four sign lines with the protocol-aware wrapper.
 *
 * <p>The previous implementation appended one code point at a time and measured the growing
 * string. A color token's width is not stable under that operation ({@code #FF634} is six
 * visible characters, {@code #FF6347} is a zero-width marker), so the break decision was
 * made on an interpretation that no longer held once the token completed; the paste could
 * therefore cut a token in half and leave a line that renders literal markup. Building the
 * candidate line first and wrapping it once removes that class of failure.</p>
 */
public final class SignPasteLayout {
    /** Vanilla sign line budget, matching {@code GuiEditSign.keyTyped}. */
    public static final int LINE_WIDTH = 90;
    public static final int LINE_COUNT = 4;

    private SignPasteLayout() {
    }

    public static List<String> layout(FontRenderer font, String currentLine, String clipboard,
                                      int maxWidth, int maxLines) {
        List<String> result = new ArrayList<>();
        if (maxLines <= 0) return result;
        String base = currentLine == null ? "" : currentLine;
        String normalized = clipboard == null ? ""
                : clipboard.replace("\r\n", "\n").replace('\r', '\n');
        String[] segments = stripSectionSigns(normalized).split("\n", -1);
        if (!ProtocolTextWrapper.enabled(ProtocolTextWrapper.Scope.SIGN)) {
            // Scope disabled: write each pasted line as-is, without automatic wrapping.
            for (int segment = 0; segment < segments.length && result.size() < maxLines; segment++) {
                result.add(segment == 0 ? base + segments[0] : segments[segment]);
            }
            if (result.isEmpty()) result.add(base);
            return result;
        }
        for (int segment = 0; segment < segments.length && result.size() < maxLines; segment++) {
            String candidate = (segment == 0 ? base : "") + segments[segment];
            if (candidate.isEmpty()) {
                result.add("");
                continue;
            }
            for (String wrapped : ProtocolTextWrapper.wrap(font, candidate, maxWidth, true)) {
                if (result.size() >= maxLines) break;
                result.add(wrapped);
            }
        }
        if (result.isEmpty()) result.add(base);
        return result;
    }

    /** Signs cannot enter section signs through the vanilla editor; keep pasted text consistent. */
    private static String stripSectionSigns(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\u00a7') {
                if (index + 1 < value.length()) index++;
                continue;
            }
            result.append(character);
        }
        return result.toString();
    }
}
