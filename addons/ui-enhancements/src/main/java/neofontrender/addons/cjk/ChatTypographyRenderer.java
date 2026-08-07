package neofontrender.addons.cjk;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IChatComponent;
import neofontrender.api.text.CjkParagraphLayoutProvider;

/** Draws and queries the exact Tiqian geometry carried by a chat component line. */
public final class ChatTypographyRenderer {
    private ChatTypographyRenderer() {}

    public static boolean isPositioned(IChatComponent component) {
        return component instanceof PositionedTextLine;
    }

    public static int draw(FontRenderer font, IChatComponent component, float x, float y,
                           int color, boolean shadow) {
        if (!(component instanceof PositionedTextLine)) {
            String text = component == null ? "" : component.getFormattedText();
            return shadow ? font.drawStringWithShadow(text, Math.round(x), Math.round(y), color)
                    : font.drawString(text, Math.round(x), Math.round(y), color, false);
        }
        PositionedTextLine line = (PositionedTextLine) component;
        for (CjkParagraphLayoutProvider.Run run : line.nfrUi$runs()) {
            float drawX = x + run.xOffset();
            if (shadow) {
                font.drawStringWithShadow(run.formattedText(), Math.round(drawX), Math.round(y), color);
            } else {
                font.drawString(run.formattedText(), Math.round(drawX), Math.round(y), color, false);
            }
        }
        return (int) Math.ceil(x + line.nfrUi$width());
    }

    public static int width(FontRenderer font, IChatComponent component) {
        return component instanceof PositionedTextLine
                ? (int) Math.ceil(((PositionedTextLine) component).nfrUi$width())
                : font.getStringWidth(component == null ? "" : component.getFormattedText());
    }

    public static IChatComponent componentAt(IChatComponent component, float x) {
        return component instanceof PositionedTextLine
                ? ((PositionedTextLine) component).nfrUi$componentAt(x) : null;
    }

    public static int formattedIndexAt(IChatComponent component, float x) {
        if (!(component instanceof PositionedTextLine)) return 0;
        int visible = ((PositionedTextLine) component).nfrUi$visibleOffsetAt(x);
        return formattedIndexForVisible(component.getFormattedText(), visible);
    }

    public static float xAtFormattedIndex(IChatComponent component, int formattedIndex) {
        if (!(component instanceof PositionedTextLine)) return 0.0F;
        int visible = visibleOffsetForFormattedIndex(component.getFormattedText(), formattedIndex);
        return ((PositionedTextLine) component).nfrUi$xAtVisibleOffset(visible);
    }

    private static int formattedIndexForVisible(String text, int visibleTarget) {
        int visible = 0;
        for (int raw = 0; raw < text.length();) {
            if (text.charAt(raw) == '\u00a7' && raw + 1 < text.length()) {
                raw += 2;
                continue;
            }
            if (visible >= visibleTarget) return raw;
            int count = Character.charCount(text.codePointAt(raw));
            raw += count;
            visible += count;
        }
        return text.length();
    }

    private static int visibleOffsetForFormattedIndex(String text, int rawTarget) {
        int visible = 0;
        int limit = Math.max(0, Math.min(text.length(), rawTarget));
        for (int raw = 0; raw < limit;) {
            if (text.charAt(raw) == '\u00a7' && raw + 1 < text.length()) {
                raw = Math.min(limit, raw + 2);
                continue;
            }
            int count = Character.charCount(text.codePointAt(raw));
            raw += count;
            visible += count;
        }
        return visible;
    }
}
