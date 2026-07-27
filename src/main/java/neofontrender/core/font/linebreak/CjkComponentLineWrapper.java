package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

/**
 * Component-preserving counterpart of FontRenderer's formatted-string wrapping.
 */
public final class CjkComponentLineWrapper {
    private CjkComponentLineWrapper() {
    }

    public static List<ITextComponent> wrap(ITextComponent text, int maxWidth,
                                            FontRenderer font, boolean removeLeadingSpace,
                                            boolean forceTextColor) {
        int widthLimit = Math.max(1, maxWidth);
        List<ITextComponent> pending = new ArrayList<>();
        for (ITextComponent component : text) {
            pending.add(component);
        }

        List<ITextComponent> lines = new ArrayList<>();
        ITextComponent line = new TextComponentString("");
        int lineWidth = 0;

        for (int index = 0; index < pending.size(); index++) {
            ITextComponent source = pending.get(index);
            String sourceText = source.getUnformattedComponentText();
            boolean endLine = false;

            int newline = sourceText.indexOf('\n');
            if (newline >= 0) {
                ITextComponent remainder = copyWithText(
                        source, sourceText.substring(newline + 1));
                pending.add(index + 1, remainder);
                sourceText = sourceText.substring(0, newline);
                endLine = true;
            }

            String formatted = GuiUtilRenderComponents.removeTextColorsIfConfigured(
                    source.getStyle().getFormattingCode() + sourceText, forceTextColor);
            int available = widthLimit - lineWidth;
            int formattedWidth = font.getStringWidth(formatted);

            if (formattedWidth > available) {
                if (lineWidth > 0 && available <= 0) {
                    lines.add(line);
                    line = new TextComponentString("");
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
                    line = new TextComponentString("");
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
                line = new TextComponentString("");
                lineWidth = 0;
            }
        }

        lines.add(line);
        return lines;
    }

    private static ITextComponent copyWithText(ITextComponent source, String text) {
        return new TextComponentString(text).setStyle(source.getStyle().createDeepCopy());
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
