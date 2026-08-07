package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Component-preserving counterpart of FontRenderer's formatted-string wrapping.
 */
public final class CjkComponentLineWrapper {
    private CjkComponentLineWrapper() {
    }

    public static List<IChatComponent> wrap(IChatComponent text, int maxWidth,
                                            FontRenderer font, boolean removeLeadingSpace,
                                            boolean forceTextColor) {
        int widthLimit = Math.max(1, maxWidth);
        List<IChatComponent> pending = new ArrayList<>();
        for (Object raw : text) {
            pending.add((IChatComponent) raw);
        }

        List<IChatComponent> lines = new ArrayList<>();
        IChatComponent line = new ChatComponentText("");
        int lineWidth = 0;

        for (int index = 0; index < pending.size(); index++) {
            IChatComponent source = pending.get(index);
            String sourceText = source.getUnformattedText();
            boolean endLine = false;

            int newline = sourceText.indexOf('\n');
            if (newline >= 0) {
                IChatComponent remainder = copyWithText(
                        source, sourceText.substring(newline + 1));
                pending.add(index + 1, remainder);
                sourceText = sourceText.substring(0, newline);
                endLine = true;
            }

            String formatted = source.getChatStyle().getFormattingCode() + sourceText;
            int available = widthLimit - lineWidth;
            int formattedWidth = font.getStringWidth(formatted);

            if (formattedWidth > available) {
                if (lineWidth > 0 && available <= 0) {
                    lines.add(line);
                    line = new ChatComponentText("");
                    lineWidth = 0;
                    index--;
                    continue;
                }

                int cut = font.sizeStringToWidth(formatted, Math.max(1, available));
                if (cut <= 0) {
                    cut = firstSafeBoundary(formatted);
                }
                cut = Math.min(cut, formatted.length());
                if (lineWidth > 0
                        && font.getStringWidth(formatted.substring(0, cut)) > available) {
                    lines.add(line);
                    line = new ChatComponentText("");
                    lineWidth = 0;
                    index--;
                    continue;
                }

                String before = formatted.substring(0, cut);
                String after = formatted.substring(cut);
                if (removeLeadingSpace && !after.isEmpty() && after.charAt(0) == ' ') {
                    after = after.substring(1);
                }
                if (!after.isEmpty()) {
                    pending.add(index + 1, copyWithText(source, after));
                }
                formatted = before;
                formattedWidth = font.getStringWidth(formatted);
                endLine = true;
            }

            if (!formatted.isEmpty()) {
                line.appendSibling(copyWithText(source, formatted));
                lineWidth += formattedWidth;
            }

            if (endLine) {
                lines.add(line);
                line = new ChatComponentText("");
                lineWidth = 0;
            }
        }

        lines.add(line);
        return lines;
    }

    private static IChatComponent copyWithText(IChatComponent source, String text) {
        return new ChatComponentText(text).setChatStyle(
                source.getChatStyle().createDeepCopy());
    }

    private static int firstSafeBoundary(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        if (text.charAt(0) == '\u00A7' && text.length() > 1) {
            return Math.min(text.length(), 2 + firstCodePointLength(text, 2));
        }
        return firstCodePointLength(text, 0);
    }

    private static int firstCodePointLength(String text, int index) {
        return index >= text.length() ? 0 : Character.charCount(text.codePointAt(index));
    }
}
