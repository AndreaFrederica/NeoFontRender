package neofontrender.addons.chat;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.addons.api.inline.InlineTextEngine;

import java.util.List;

/** Pixel-height layout helpers for vanilla chat rows containing inline images. */
public final class ChatInlineLayout {
    private ChatInlineLayout() {}

    public static int lineHeight(ChatLine line, FontRenderer font) {
        if (!EnhancedChatFeatures.inlineGlyphs() || line == null) return font.FONT_HEIGHT;
        return Math.max(font.FONT_HEIGHT, InlineTextEngine.layout(font,
                line.func_151461_a().getFormattedText()).height());
    }

    public static int heightBefore(List<ChatLine> lines, int scrollPos, int row, FontRenderer font) {
        int total = 0;
        int end = Math.min(lines.size(), Math.max(0, scrollPos) + Math.max(0, row));
        for (int index = Math.max(0, scrollPos); index < end; index++) {
            total += lineHeight(lines.get(index), font);
        }
        return total;
    }

    public static float contentY(List<ChatLine> lines, int scrollPos, int row, FontRenderer font) {
        int index = scrollPos + row;
        if (index < 0 || index >= lines.size()) return -row * font.FONT_HEIGHT - font.FONT_HEIGHT + 1;
        return -heightBefore(lines, scrollPos, row, font) - lineHeight(lines.get(index), font) + 1;
    }

    public static int bottomAlignedY(List<ChatLine> lines, int scrollPos, int row,
                                     int objectHeight, FontRenderer font) {
        return -heightBefore(lines, scrollPos, row, font) - objectHeight;
    }

    public static int visibleLineCount(List<ChatLine> lines, int scrollPos, int pixelHeight,
                                       FontRenderer font) {
        int used = 0;
        int count = 0;
        for (int index = Math.max(0, scrollPos); index < lines.size(); index++) {
            int height = lineHeight(lines.get(index), font);
            if (count > 0 && used + height > pixelHeight) break;
            used += height;
            count++;
            if (used >= pixelHeight) break;
        }
        return count;
    }
}
