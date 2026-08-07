package neofontrender.addons.api.inline;

import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Entry point for measuring and drawing text through all registered inline glyph providers. */
public final class InlineTextEngine {
    private InlineTextEngine() {}

    public static InlineTextLayout layout(FontRenderer font, String source) {
        String text = source == null ? "" : source;
        List<InlineTextLayout.Run> runs = new ArrayList<>();
        FormattingState formatting = new FormattingState();
        int runStart = 0;
        String runPrefix = "";
        int x = 0;
        int height = Math.max(1, font.FONT_HEIGHT);
        int index = 0;
        while (index < text.length()) {
            if (text.charAt(index) == '\u00a7' && index + 1 < text.length()) {
                formatting.accept(text.charAt(index + 1));
                index += 2;
                continue;
            }
            InlineGlyphMatch match = InlineGlyphRegistry.match(text, index);
            if (match == null) {
                index++;
                continue;
            }
            if (runStart < index) {
                String raw = text.substring(runStart, index);
                int width = font.getStringWidth(raw);
                runs.add(InlineTextLayout.Run.text(runStart, index, x, width,
                        runPrefix + raw));
                x += width;
            }
            int width = Math.max(0, match.glyph().advance(font));
            height = Math.max(height, Math.max(1, match.glyph().height(font)));
            runs.add(InlineTextLayout.Run.glyph(match, x, width));
            x += width;
            for (int i = index; i + 1 < match.end(); i++) {
                if (text.charAt(i) == '\u00a7') formatting.accept(text.charAt(++i));
            }
            index = match.end();
            runStart = index;
            runPrefix = formatting.prefix();
        }
        if (runStart < text.length()) {
            String raw = text.substring(runStart);
            int width = font.getStringWidth(raw);
            runs.add(InlineTextLayout.Run.text(runStart, text.length(), x, width,
                    runPrefix + raw));
            x += width;
        }
        return new InlineTextLayout(text, x, height, runs);
    }

    public static int width(FontRenderer font, String source) {
        return layout(font, source).width();
    }

    private static final class FormattingState {
        private char color;
        private boolean obfuscated;
        private boolean bold;
        private boolean strike;
        private boolean underline;
        private boolean italic;

        private void accept(char rawCode) {
            char code = Character.toLowerCase(rawCode);
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = code;
                clearStyles();
                return;
            }
            switch (code) {
                case 'k': obfuscated = true; break;
                case 'l': bold = true; break;
                case 'm': strike = true; break;
                case 'n': underline = true; break;
                case 'o': italic = true; break;
                case 'r': color = 0; clearStyles(); break;
                default: break;
            }
        }

        private void clearStyles() {
            obfuscated = bold = strike = underline = italic = false;
        }

        private String prefix() {
            StringBuilder result = new StringBuilder(12);
            if (color != 0) result.append('\u00a7').append(color);
            if (obfuscated) result.append("\u00a7k");
            if (bold) result.append("\u00a7l");
            if (strike) result.append("\u00a7m");
            if (underline) result.append("\u00a7n");
            if (italic) result.append("\u00a7o");
            return result.toString().toLowerCase(Locale.ROOT);
        }
    }
}
