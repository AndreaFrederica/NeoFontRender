package neofontrender.addons.api.inline;

import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Plain-string wrapper that keeps image tokens atomic and preserves raw cursor indices. */
public final class InlineTextWrapping {
    private InlineTextWrapping() {}

    public static List<String> wrap(FontRenderer font, String source, int maximumWidth) {
        String text = source == null ? "" : source;
        if (text.isEmpty()) return Collections.singletonList("");
        int limit = Math.max(1, maximumWidth);
        List<String> lines = new ArrayList<>();
        int lineStart = 0;
        int index = 0;
        int width = 0;
        int lastSpace = -1;
        while (index < text.length()) {
            int end;
            int advance;
            if (text.charAt(index) == '\u00a7' && index + 1 < text.length()) {
                end = index + 2;
                advance = 0;
            } else {
                InlineGlyphMatch glyph = InlineGlyphRegistry.match(text, index);
                if (glyph != null) {
                    end = glyph.end();
                    advance = glyph.glyph().advance(font);
                } else {
                    int codePoint = Character.codePointAt(text, index);
                    end = index + Character.charCount(codePoint);
                    advance = font.getStringWidth(text.substring(index, end));
                }
            }
            if (width + advance > limit && index > lineStart) {
                int lineEnd = lastSpace >= lineStart ? lastSpace : index;
                if (lineEnd <= lineStart) lineEnd = index;
                lines.add(text.substring(lineStart, lineEnd));
                lineStart = lineEnd;
                if (lineStart < text.length() && text.charAt(lineStart) == ' ') lineStart++;
                index = lineStart;
                width = 0;
                lastSpace = -1;
                continue;
            }
            if (text.charAt(index) == ' ') lastSpace = index;
            width += advance;
            index = end;
        }
        lines.add(text.substring(lineStart));
        return lines;
    }
}
