package neofontrender.addons.cjk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.CjkParagraphLayoutProvider;
import neofontrender.api.text.CjkParagraphLayoutRegistry;
import neofontrender.core.config.NeofontrenderConfig;

import net.minecraft.util.IChatComponent;
import java.util.List;

/** UIE rendering helper for surfaces that own both paragraph width and drawing. */
public final class CjkTypographyRenderer {
    private static final int UNBOUNDED_GUI_WIDTH = 1_000_000;

    private CjkTypographyRenderer() {}

    public static CjkParagraphLayoutProvider.Layout layout(
            FontRenderer font, String text, int width, int lineHeight) {
        if (!NeofontrenderConfig.fixCjkLineBreak() || text == null) return null;
        return CjkParagraphLayoutRegistry.layout(new CjkParagraphLayoutProvider.Request(
                text, width, lineHeight, languageCode(), font::getStringWidth));
    }

    public static List<IChatComponent> splitComponents(
            FontRenderer font, IChatComponent component, int width,
            boolean removeLeadingSpace, boolean forceTextColor,
            CjkParagraphLayoutProvider.ComponentRequest.Surface surface) {
        if (!NeofontrenderConfig.fixCjkLineBreak() || component == null) return null;
        return CjkParagraphLayoutRegistry.splitComponents(
                new CjkParagraphLayoutProvider.ComponentRequest(
                        component, width, font.FONT_HEIGHT, languageCode(),
                        removeLeadingSpace, forceTextColor, font::getStringWidth, surface));
    }

    private static String languageCode() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft == null || minecraft.getLanguageManager() == null
                || minecraft.getLanguageManager().getCurrentLanguage() == null ? ""
                : minecraft.getLanguageManager().getCurrentLanguage().getLanguageCode();
    }

    public static int measuredWidth(FontRenderer font, String text) {
        CjkParagraphLayoutProvider.Layout layout = layout(
                font, text, UNBOUNDED_GUI_WIDTH, font.FONT_HEIGHT);
        return layout == null ? font.getStringWidth(text) : measuredWidth(font, layout);
    }

    public static int measuredWidth(FontRenderer font, CjkParagraphLayoutProvider.Layout layout) {
        float right = 0;
        for (CjkParagraphLayoutProvider.Line line : layout.lines()) {
            for (CjkParagraphLayoutProvider.Run run : line.runs()) {
                right = Math.max(right, run.xOffset() + font.getStringWidth(run.formattedText()));
            }
        }
        return (int) Math.ceil(right);
    }

    public static boolean draw(FontRenderer font, CjkParagraphLayoutProvider.Layout layout,
                               float x, float y, int color, boolean shadow) {
        return draw(font, layout, x, y, color, shadow, Integer.MAX_VALUE);
    }

    public static boolean draw(FontRenderer font, CjkParagraphLayoutProvider.Layout layout,
                               float x, float y, int color, boolean shadow, int maxLines) {
        if (layout == null) return false;
        int count = Math.min(Math.max(0, maxLines), layout.lines().size());
        for (int lineIndex = 0; lineIndex < count; lineIndex++) {
            CjkParagraphLayoutProvider.Line line = layout.lines().get(lineIndex);
            for (CjkParagraphLayoutProvider.Run run : line.runs()) {
                float drawX = x + run.xOffset();
                float drawY = y + line.yOffset();
                if (shadow) {
                    font.drawStringWithShadow(run.formattedText(),
                            Math.round(drawX), Math.round(drawY), color);
                } else {
                    font.drawString(run.formattedText(),
                            Math.round(drawX), Math.round(drawY), color, false);
                }
            }
        }
        return true;
    }
}
