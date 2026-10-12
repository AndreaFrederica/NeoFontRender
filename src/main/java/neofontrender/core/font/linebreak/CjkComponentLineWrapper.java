package neofontrender.core.font.linebreak;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.core.font.support.ScopedFontRenderBypass;

import java.util.ArrayList;
import java.util.List;

/**
 * Component-preserving counterpart of FontRenderer's formatted-string wrapping.
 */
public final class CjkComponentLineWrapper {
    private CjkComponentLineWrapper() {
    }

    @FunctionalInterface
    public interface WidthMeasurer {
        int sizeToWidth(FontRenderer font, String text, int width);
    }

    public static List<IChatComponent> wrap(IChatComponent text, int maxWidth,
                                            FontRenderer font, boolean removeLeadingSpace,
                                            boolean forceTextColor) {
        return wrap(text, maxWidth, font, removeLeadingSpace, forceTextColor,
                (renderer, source, width) -> renderer.sizeStringToWidth(source, width));
    }

    public static List<IChatComponent> wrap(IChatComponent text, int maxWidth,
                                            FontRenderer font, boolean removeLeadingSpace,
                                            boolean forceTextColor, WidthMeasurer measurer) {
        int widthLimit = Math.max(1, maxWidth);
        List<IChatComponent> pending = new ArrayList<>();
        for (Object raw : text) {
            IChatComponent component = (IChatComponent) raw;
            String formatted = removeTextColorsIfConfigured(
                    component.getChatStyle().getFormattingCode() + component.getUnformattedTextForChat(),
                    forceTextColor);
            pending.add(copyWithText(component, FormattedColorWrapping.prepare(formatted)));
        }

        List<IChatComponent> lines = new ArrayList<>();
        IChatComponent line = new ChatComponentText("");
        int lineWidth = 0;

        for (int index = 0; index < pending.size(); index++) {
            IChatComponent source = pending.get(index);
            String formatted = removeTextColorsIfConfigured(
                    source.getChatStyle().getFormattingCode() + source.getUnformattedTextForChat(),
                    forceTextColor);
            boolean endLine = false;
            String newlineRemainder = null;
            String wrappedRemainder = null;

            int newline = formatted.indexOf('\n');
            if (newline >= 0) {
                newlineRemainder = FormattedColorWrapping.continuation(
                        formatted.substring(0, newline), formatted.substring(newline + 1));
                formatted = formatted.substring(0, newline);
                endLine = true;
            }

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

                int cut = measurer.sizeToWidth(font, formatted, Math.max(1, available));
                // A formatting-only prefix is not progress, especially after carrying it forward.
                cut = Math.max(cut, FormattedColorWrapping.firstSafeBoundary(formatted));
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
                    wrappedRemainder = FormattedColorWrapping.continuation(before, after);
                }
                formatted = before;
                formattedWidth = font.getStringWidth(formatted);
                endLine = true;
            }

            // Commit remainders only after deciding not to retry this component on a fresh line.
            if (wrappedRemainder != null && newlineRemainder != null) {
                // The soft-wrapped tail must still end at the original mandatory break.
                wrappedRemainder += "\n" + newlineRemainder;
                newlineRemainder = null;
            }
            if (newlineRemainder != null) {
                pending.add(index + 1, copyWithText(source, newlineRemainder));
            }
            if (wrappedRemainder != null) {
                pending.add(index + 1, copyWithText(source, wrappedRemainder));
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

    /**
     * 1.7.10 has no {@code GuiUtilRenderComponents}; this mirrors its colour-stripping rule using
     * the vanilla chat colour setting instead.
     */
    private static String removeTextColorsIfConfigured(String text, boolean forceTextColor) {
        return forceTextColor || Minecraft.getMinecraft().gameSettings.chatColours
                ? text : EnumChatFormatting.getTextWithoutFormattingCodes(text);
    }

    private static IChatComponent copyWithText(IChatComponent source, String text) {
        return new ChatComponentText(text).setChatStyle(source.getChatStyle().createDeepCopy());
    }

    /**
     * Measures through the selected text route when the replacement font is active.
     * The vanilla call is retained only for an explicitly unhandled route; otherwise
     * calling FontRenderer.sizeStringToWidth here re-enters our own Mixin and causes
     * the wrapping/measurement recursion seen during startup.
     */
    public static int routeSizeToWidth(FontRenderer font, String text, int width) {
        TextRenderRouteLayout layout = TextRenderRouteApi.layout(font, text, 0xFFFFFFFF, false);
        if (layout.handled()) {
            return layout.sizeToWidth(Math.max(1, width), true);
        }
        return ScopedFontRenderBypass.call(
                () -> font.sizeStringToWidth(text, Math.max(1, width)));
    }
}
