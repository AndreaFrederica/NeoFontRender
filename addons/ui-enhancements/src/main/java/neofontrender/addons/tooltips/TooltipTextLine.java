package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.api.text.TextVisualBounds;
import neofontrender.api.text.paragraph.TextParagraphProvider;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;

import java.util.ArrayList;
import java.util.List;

/** The exact positioned runs used for both measurement and drawing of one finalized row. */
final class TooltipTextLine {
    private final List<Run> runs;
    final TextVisualBounds bounds;
    final float advance;

    private TooltipTextLine(List<Run> runs, TextVisualBounds bounds, float advance) {
        this.runs = runs;
        this.bounds = bounds;
        this.advance = advance;
    }

    static TooltipTextLine measure(FontRenderer font, String text, int color, boolean shadow) {
        List<Run> runs = new ArrayList<>();
        if (NfrTooltipAnchor.isAnchorLine(text)) {
            return new TooltipTextLine(runs, TextVisualBounds.EMPTY, 0);
        }
        // Line wrapping has already been decided. Never wrap again in the painting stage.
        TextRenderRouteLayout whole = TextRenderRouteApi.layout(font, text, color, shadow);
        TextParagraphProvider.Layout paragraph = whole.hasInlineContent() ? null
                : CjkTypographyRenderer.layout(font, text, 1_000_000, TooltipConfig.lineHeight);
        if (paragraph == null) {
            runs.add(new Run(whole, 0, 0));
        } else {
            for (TextParagraphProvider.Line line : paragraph.lines()) {
                for (TextParagraphProvider.Run run : line.runs()) {
                    runs.add(new Run(TextRenderRouteApi.layout(font, run.formattedText(), color, shadow),
                            run.xOffset(), line.yOffset()));
                }
            }
        }
        TextVisualBounds bounds = TextVisualBounds.EMPTY;
        float advance = 0;
        for (Run run : runs) {
            bounds = bounds.union(run.layout.visualBounds().translate(run.x, run.y));
            advance = Math.max(advance, run.x + run.layout.advance());
        }
        return new TooltipTextLine(runs, bounds, advance);
    }

    void draw(FontRenderer font, float x, float y, int color, boolean shadow) {
        for (Run run : runs) {
            // Keep FontRenderer's alpha, palette and scale-tuning hooks. It dispatches to
            // the same route; only paragraph placement is retained here.
            font.drawString(run.layout.source(), x + run.x, y + run.y, color, shadow);
        }
    }

    static float alignedOrigin(float regionWidth, TextVisualBounds bounds, String alignment) {
        float spare = Math.max(0, regionWidth - bounds.width());
        return ("right".equals(alignment) ? spare : "center".equals(alignment) ? spare * 0.5F : 0)
                - bounds.left;
    }

    private static final class Run {
        final TextRenderRouteLayout layout;
        final float x, y;
        Run(TextRenderRouteLayout layout, float x, float y) {
            this.layout = layout; this.x = x; this.y = y;
        }
    }
}
