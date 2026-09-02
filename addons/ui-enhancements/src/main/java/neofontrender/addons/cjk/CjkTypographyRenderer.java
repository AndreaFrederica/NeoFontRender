package neofontrender.addons.cjk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.pipeline.ParagraphLayoutMiddleware;
import neofontrender.api.text.pipeline.TextPipelineApi;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.TooltipBoundsCompat;

import net.minecraft.util.text.ITextComponent;
import java.util.ArrayList;
import java.util.List;

/** UIE rendering helper for surfaces that own both paragraph width and drawing. */
public final class CjkTypographyRenderer {
    private static final int UNBOUNDED_GUI_WIDTH = 1_000_000;

    private CjkTypographyRenderer() {}

    public static ParagraphLayoutMiddleware.Layout layout(
            FontRenderer font, String text, int width, int lineHeight) {
        if (!NeofontrenderConfig.fixCjkLineBreak() || text == null) return null;
        return TextPipelineApi.layoutParagraph(new ParagraphLayoutMiddleware.Request(
                text, width, lineHeight, languageCode(), font::getStringWidth));
    }

    public static List<ITextComponent> splitComponents(
            FontRenderer font, ITextComponent component, int width,
            boolean removeLeadingSpace, boolean forceTextColor,
            ParagraphLayoutMiddleware.ComponentRequest.Surface surface) {
        if (!NeofontrenderConfig.fixCjkLineBreak() || component == null) return null;
        return TextPipelineApi.splitComponents(
                new ParagraphLayoutMiddleware.ComponentRequest(
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
        ParagraphLayoutMiddleware.Layout layout = layout(
                font, text, UNBOUNDED_GUI_WIDTH, font.FONT_HEIGHT);
        return layout == null ? font.getStringWidth(text) : measuredWidth(font, layout);
    }

    public static int measuredWidth(FontRenderer font, ParagraphLayoutMiddleware.Layout layout) {
        float right = 0;
        for (ParagraphLayoutMiddleware.Line line : layout.lines()) {
            for (ParagraphLayoutMiddleware.Run run : line.runs()) {
                right = Math.max(right, run.xOffset() + font.getStringWidth(run.formattedText()));
            }
        }
        return (int) Math.ceil(right);
    }

    /** Measures the same positioned Tiqian runs that {@link #draw} will render. */
    public static int measuredVisualWidth(FontRenderer font, String text) {
        ParagraphLayoutMiddleware.Layout layout = layout(
                font, text, UNBOUNDED_GUI_WIDTH, font.FONT_HEIGHT);
        if (layout == null) return -1;
        float right = 0.0F;
        for (ParagraphLayoutMiddleware.Line line : layout.lines()) {
            for (ParagraphLayoutMiddleware.Run run : line.runs()) {
                right = Math.max(right, run.xOffset()
                        + TooltipBoundsCompat.measuredWidth(font, run.formattedText()));
            }
        }
        return Math.max(1, (int) Math.ceil(right));
    }

    /** Returns Tiqian's formatted line runs, or {@code null} when Tiqian did not handle the text. */
    public static List<String> wrap(FontRenderer font, String text, int width, int lineHeight) {
        ParagraphLayoutMiddleware.Layout layout = layout(font, text, width, lineHeight);
        if (layout == null) return null;
        List<String> lines = new ArrayList<>(Math.max(1, layout.lines().size()));
        for (ParagraphLayoutMiddleware.Line line : layout.lines()) {
            StringBuilder formatted = new StringBuilder();
            for (ParagraphLayoutMiddleware.Run run : line.runs()) {
                formatted.append(run.formattedText());
            }
            lines.add(formatted.toString());
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    public static boolean draw(FontRenderer font, ParagraphLayoutMiddleware.Layout layout,
                               float x, float y, int color, boolean shadow) {
        return draw(font, layout, x, y, color, shadow, Integer.MAX_VALUE);
    }

    public static boolean draw(FontRenderer font, ParagraphLayoutMiddleware.Layout layout,
                               float x, float y, int color, boolean shadow, int maxLines) {
        if (layout == null) return false;
        int count = Math.min(Math.max(0, maxLines), layout.lines().size());
        for (int lineIndex = 0; lineIndex < count; lineIndex++) {
            ParagraphLayoutMiddleware.Line line = layout.lines().get(lineIndex);
            for (ParagraphLayoutMiddleware.Run run : line.runs()) {
                float drawX = x + run.xOffset();
                float drawY = y + line.yOffset();
                if (shadow) font.drawStringWithShadow(run.formattedText(), drawX, drawY, color);
                else font.drawString(run.formattedText(), drawX, drawY, color, false);
            }
        }
        return true;
    }
}
