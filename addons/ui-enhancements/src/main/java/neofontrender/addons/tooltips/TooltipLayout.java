package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.core.font.support.TooltipBoundsCompat;
import neofontrender.core.font.FontManager;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.api.text.pipeline.TextPipelineApi;
import neofontrender.api.text.pipeline.TextPipelineEngine;
import neofontrender.api.text.pipeline.TextPipelineWrapping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

final class TooltipLayout {
    final List<String> lines;
    final List<Boolean> compactLines;
    final int titleLines;
    final int x;
    final int y;
    final int width;
    final int height;
    final int visualTop;
    final int visualBottom;
    /** Final per-line widths; TC6 title/divider drawing must consume these exact values. */
    final List<Integer> lineWidths;
    final List<Integer> lineAdvances;
    final TooltipVisualPlan visualPlan;
    private final TooltipConfig.Profile profile;

    private TooltipLayout(List<String> lines, List<Boolean> compactLines, int titleLines,
                          int x, int y, int width, int height, List<Integer> lineWidths,
                          List<Integer> lineAdvances, int visualTop, int visualBottom,
                          TooltipConfig.Profile profile, TooltipVisualPlan visualPlan) {
        this.lines = lines;
        this.compactLines = compactLines;
        this.titleLines = titleLines;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visualTop = visualTop;
        this.visualBottom = visualBottom;
        this.lineWidths = lineWidths;
        this.lineAdvances = lineAdvances;
        this.profile = profile;
        this.visualPlan = visualPlan;
    }

    TooltipConfig.Profile profile() { return profile; }

    static TooltipLayout calculate(RenderTooltipEvent.Pre event, boolean[] compactSource,
                                   TooltipConfig.Profile profile) {
        return calculate(event, compactSource, profile, null);
    }

    static TooltipLayout calculate(RenderTooltipEvent.Pre event, boolean[] compactSource,
                                   TooltipConfig.Profile profile,
                                   ThaumcraftTooltipCompat.Context thaumcraftContext) {
        if (thaumcraftContext != null) {
            return calculateThaumcraft(event, compactSource, profile, thaumcraftContext);
        }
        FontRenderer font = event.getFontRenderer();
        List<String> source = normalizedSource(event.getLines(), event.getStack().isEmpty());
        TooltipConfig.Profile activeProfile = profile == null ? TooltipConfig.profile("vanilla") : profile;
        int horizontalPadding = TooltipConfig.horizontalPadding;
        int verticalPadding = TooltipConfig.verticalPadding;
        int cursorOffset = TooltipConfig.cursorOffset;
        TooltipVisualExtents extents = TooltipVisualExtents.current();
        List<Integer> lineWidths = measureLineWidths(font, source,
                flagsFor(source.size(), compactSource), activeProfile.textScale);
        int width = maxWidth(lineWidths);
        int x = event.getX() + cursorOffset;
        boolean wrap = false;

        int screenWidthLimit = screenWidthLimit(
                event.getScreenWidth(), horizontalPadding, extents);
        int contentLimit = screenWidthLimit;
        if (event.getMaxWidth() > 0) contentLimit = Math.min(contentLimit, event.getMaxWidth());
        if (TooltipConfig.maxWidth > 0) contentLimit = Math.min(contentLimit, TooltipConfig.maxWidth);
        TooltipVisualPlan visualPlan = TooltipVisualPlan.collect(
                event.getStack(), source, font, contentLimit);
        width = Math.max(width, visualPlan.maxWidth());
        if (width > screenWidthLimit) {
            width = screenWidthLimit;
            wrap = true;
        } else if (x + width + horizontalPadding > event.getScreenWidth()) {
            x = event.getX() - cursorOffset - horizontalPadding - width;
        }
        if (event.getMaxWidth() > 0 && width > event.getMaxWidth()) {
            width = event.getMaxWidth();
            wrap = true;
        }
        if (TooltipConfig.maxWidth > 0 && width > TooltipConfig.maxWidth) {
            width = TooltipConfig.maxWidth;
            wrap = true;
        }

        List<String> lines = source;
        List<Boolean> compactLines = flagsFor(source.size(), compactSource);
        int titleLines = source.isEmpty() ? 0 : 1;
        if (wrap) {
            List<String> wrapped = new ArrayList<>();
            List<Boolean> wrappedCompact = new ArrayList<>();
            List<Integer> lastWrappedLineBySource = new ArrayList<>(source.size());
            for (int i = 0; i < source.size(); i++) {
                int sourceWidth = compactLines.get(i) ? Math.max(1, Math.round(width * 2.0F / activeProfile.textScale))
                        : Math.max(1, Math.round(width / activeProfile.textScale));
                List<String> part = wrapLine(font, source.get(i), sourceWidth);
                if (i == 0) titleLines = part.size();
                wrapped.addAll(part);
                lastWrappedLineBySource.add(wrapped.size() - 1);
                for (int j = 0; j < part.size(); j++) {
                    wrappedCompact.add(compactLines.get(i));
                }
            }
            lines = wrapped;
            compactLines = wrappedCompact;
            visualPlan = visualPlan.remap(lastWrappedLineBySource, lines.size());
            lineWidths = measureLineWidths(font, lines, compactLines,
                    activeProfile.textScale);
            width = maxWidth(lineWidths);
            width = Math.max(width, visualPlan.maxWidth());
            x = event.getX() > event.getScreenWidth() / 2
                    ? event.getX() - cursorOffset - horizontalPadding - width
                    : event.getX() + cursorOffset;
        }

        List<Integer> lineAdvances = lineAdvances(font, lines, compactLines,
                activeProfile.textScale);
        int height = lineAdvances.stream().mapToInt(Integer::intValue).sum()
                + visualPlan.totalHeight();
        if (TooltipConfig.titleBreak && hasContentAfterTitle(lines, titleLines, visualPlan)) {
            height += TooltipConfig.titleGap;
        }
        int[] visualBounds = visualBounds(font, lines, compactLines, lineAdvances,
                titleLines, activeProfile.textScale, visualPlan);
        int y = event.getY() - cursorOffset;
        y = Math.max(verticalPadding + extents.top - visualBounds[0], Math.min(y,
                event.getScreenHeight() - visualBounds[1] - verticalPadding - extents.bottom));
        x = Math.max(horizontalPadding + extents.left, Math.min(x,
                event.getScreenWidth() - width - horizontalPadding - extents.right));
        return new TooltipLayout(lines, compactLines, titleLines, x, y, width, height,
                lineWidths, lineAdvances, visualBounds[0], visualBounds[1], activeProfile,
                visualPlan);
    }

    /**
     * Keeps the coordinates used by UtilsFX stable while still reusing NFR's panel and text
     * renderer. TC6 passes a side flag instead of letting Forge choose a side from the measured
     * width; recomputing that choice in the generic layout is what made small windows oscillate.
     */
    private static TooltipLayout calculateThaumcraft(RenderTooltipEvent.Pre event,
                                                     boolean[] compactSource,
                                                     TooltipConfig.Profile profile,
                                                     ThaumcraftTooltipCompat.Context context) {
        FontRenderer font = event.getFontRenderer();
        TooltipConfig.Profile activeProfile = profile == null ? TooltipConfig.profile("thaumcraft") : profile;
        int horizontalPadding = TooltipConfig.horizontalPadding;
        int verticalPadding = TooltipConfig.verticalPadding;
        TooltipVisualExtents extents = TooltipVisualExtents.current();
        List<String> source = event.getLines();
        List<Boolean> sourceCompact = flagsFor(source.size(), compactSource);
        List<Integer> lineWidths = measureLineWidths(font, source, sourceCompact,
                activeProfile.textScale);
        int width = maxWidth(lineWidths);
        int maxWidth = event.getMaxWidth() > 0 ? event.getMaxWidth() : 240;
        if (TooltipConfig.maxWidth > 0) maxWidth = Math.min(maxWidth, TooltipConfig.maxWidth);
        maxWidth = Math.min(maxWidth, screenWidthLimit(
                event.getScreenWidth(), horizontalPadding, extents));
        TooltipVisualPlan visualPlan = TooltipVisualPlan.collect(
                event.getStack(), source, font, maxWidth);
        width = Math.max(width, visualPlan.maxWidth());

        boolean placeLeft = context.right;
        if (!placeLeft && context.cursorX + width + 24 > event.getScreenWidth()) {
            placeLeft = true;
        }
        int available = placeLeft ? context.cursorX - 24 - 8
                : event.getScreenWidth() - context.cursorX - 24;
        if (available > 0 && width > available) maxWidth = Math.min(maxWidth, available);
        visualPlan = visualPlan.constrain(maxWidth);
        width = Math.max(maxWidth(lineWidths), visualPlan.maxWidth());
        boolean wrap = width > maxWidth;

        List<String> lines = source;
        List<Boolean> compactLines = sourceCompact;
        int titleLines = source.isEmpty() ? 0 : 1;
        if (wrap) {
            List<String> wrapped = new ArrayList<>();
            List<Boolean> wrappedCompact = new ArrayList<>();
            List<Integer> lastWrappedLineBySource = new ArrayList<>(source.size());
            for (int i = 0; i < source.size(); i++) {
                int sourceWidth = sourceCompact.get(i)
                        ? Math.max(1, Math.round(maxWidth * 2.0F / activeProfile.textScale))
                        : Math.max(1, Math.round(maxWidth / activeProfile.textScale));
                List<String> part = wrapLine(font, source.get(i), sourceWidth);
                if (i == 0) titleLines = part.size();
                wrapped.addAll(part);
                lastWrappedLineBySource.add(wrapped.size() - 1);
                for (int j = 0; j < part.size(); j++) wrappedCompact.add(sourceCompact.get(i));
            }
            lines = wrapped;
            compactLines = wrappedCompact;
            visualPlan = visualPlan.remap(lastWrappedLineBySource, lines.size());
            lineWidths = measureLineWidths(font, lines, compactLines, activeProfile.textScale);
            width = maxWidth(lineWidths);
            width = Math.max(width, visualPlan.maxWidth());
        }

        int x = placeLeft ? context.cursorX - width - 24 : context.cursorX + 12;
        List<Integer> lineAdvances = lineAdvances(font, lines, compactLines,
                activeProfile.textScale);
        int height = lineAdvances.stream().mapToInt(Integer::intValue).sum()
                + visualPlan.totalHeight();
        if (TooltipConfig.titleBreak && hasContentAfterTitle(lines, titleLines, visualPlan)) {
            height += TooltipConfig.titleGap;
        }
        int[] visualBounds = visualBounds(font, lines, compactLines, lineAdvances,
                titleLines, activeProfile.textScale, visualPlan);
        int y = context.cursorY - 12;
        y = Math.max(verticalPadding + extents.top - visualBounds[0], Math.min(y,
                event.getScreenHeight() - visualBounds[1] - verticalPadding - extents.bottom));
        x = Math.max(horizontalPadding + extents.left, Math.min(x,
                event.getScreenWidth() - width - horizontalPadding - extents.right));
        return new TooltipLayout(lines, compactLines, titleLines, x, y, width, height,
                lineWidths, lineAdvances, visualBounds[0], visualBounds[1], activeProfile,
                visualPlan);
    }

    private static int[] visualBounds(FontRenderer font, List<String> lines,
                                      List<Boolean> compactLines, List<Integer> lineAdvances,
                                      int titleLines, float profileScale,
                                      TooltipVisualPlan visualPlan) {
        float top = Float.POSITIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        float baseline = 0.0F;
        int titleCount = Math.max(0, Math.min(titleLines, lines.size()));
        for (int i = 0; i < lines.size(); i++) {
            boolean compact = compactLines.get(i);
            float scale = profileScale * (compact ? 0.5F : 1.0F);
            String line = lines.get(i);
            float lineTop = 0.0F;
            float lineBottom;
            if (line == null || line.trim().isEmpty()) {
                lineBottom = lineAdvances.get(i);
            } else if (hasInlineContent(font, line)) {
                lineBottom = Math.max(lineAdvances.get(i),
                        TextPipelineEngine.height(font, line) * scale);
            } else {
                TooltipBoundsCompat.VerticalBounds measured =
                        TooltipBoundsCompat.measuredVerticalBounds(
                                font, line, TooltipConfig.textShadow);
                lineTop = measured.top * scale;
                lineBottom = measured.bottom * scale;
            }
            top = Math.min(top, baseline + lineTop);
            bottom = Math.max(bottom, baseline + lineBottom);
            baseline += lineAdvances.get(i);
            if (i + 1 == titleCount && TooltipConfig.titleBreak
                    && hasContentAfterTitle(lines, titleCount, visualPlan)) {
                baseline += TooltipConfig.titleGap;
            }
            for (TooltipVisualBlock block : visualPlan.after(i)) {
                top = Math.min(top, baseline);
                baseline += block.height();
                bottom = Math.max(bottom, baseline);
            }
        }
        if (!Float.isFinite(top) || !Float.isFinite(bottom) || bottom <= top) {
            return new int[]{0, Math.max(1, Math.round(baseline))};
        }
        return new int[]{(int) Math.floor(top), (int) Math.ceil(bottom)};
    }

    static List<Integer> lineAdvances(FontRenderer font, List<String> lines,
                                               List<Boolean> compactLines, float profileScale) {
        return lineAdvances(lines, compactLines, profileScale,
                line -> inlineContentHeight(font, line));
    }

    static List<Integer> lineAdvances(List<String> lines, List<Boolean> compactLines,
                                      float profileScale,
                                      ToIntFunction<String> inlineContentHeight) {
        List<Integer> result = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            boolean compact = compactLines.get(i);
            float visualScale = profileScale * (compact ? 0.5F : 1.0F);
            // Use the configured baseline contract for every normal row. Title metrics affect
            // visual bounds, but must not shorten the layout advance of a single-line tooltip.
            int base = compact ? ThaumcraftTooltipCompat.COMPACT_LINE_HEIGHT
                    : TooltipConfig.lineHeight;
            int advance = Math.max(1, Math.round(base * profileScale));
            int rawContentHeight = inlineContentHeight == null
                    ? 0 : inlineContentHeight.applyAsInt(lines.get(i));
            if (rawContentHeight > 0) {
                int contentHeight = Math.max(1, Math.round(
                        rawContentHeight * visualScale));
                // Minecraft's tooltip renderer has no paragraph layout. Reserve whole
                // baseline slots for tall inline content so later lines never overlap it.
                int rowHeight = Math.max(1, Math.round(base * profileScale));
                int rows = (contentHeight + rowHeight - 1) / rowHeight;
                advance = Math.max(advance, rows * rowHeight);
            }
            result.add(advance);
        }
        return result;
    }

    /** Returns zero for ordinary text, even when inline middleware is globally registered. */
    private static int inlineContentHeight(FontRenderer font, String line) {
        if (!TextPipelineApi.hasInlineContentMiddleware()) return 0;
        try {
            neofontrender.api.text.pipeline.TextPipelineLayout measured =
                    TextPipelineEngine.layout(font, line);
            return measured.hasInlineContent() ? measured.height() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    /**
     * Keep inline middleware tokens atomic while deciding tooltip line breaks. Calling the
     * vanilla wrapper here would split a long SVG/LaTeX token before the text pipeline sees it.
     */
    private static List<String> wrapLine(FontRenderer font, String line, int width) {
        if (!hasInlineContent(font, line)) {
            List<String> tiqian = CjkTypographyRenderer.wrap(
                    font, line, width, TooltipConfig.lineHeight);
            if (tiqian != null) return tiqian;
        }
        return TextPipelineApi.hasInlineContentMiddleware()
                ? TextPipelineWrapping.wrap(font, line, width)
                : font.listFormattedStringToWidth(line, width);
    }

    static int screenWidthLimit(int screenWidth, int horizontalPadding,
                                TooltipVisualExtents extents) {
        int padding = Math.max(0, horizontalPadding);
        int left = extents == null ? 0 : extents.left;
        int right = extents == null ? 0 : extents.right;
        return Math.max(1, screenWidth - padding * 2 - left - right);
    }

    private static List<Boolean> flagsFor(int size, boolean[] compactSource) {
        List<Boolean> flags = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            flags.add(compactSource != null && i < compactSource.length && compactSource[i]);
        }
        return flags;
    }

    private static boolean hasContentAfterTitle(List<String> lines, int titleLines,
                                                TooltipVisualPlan visualPlan) {
        int titleCount = Math.max(0, Math.min(titleLines, lines.size()));
        if (lines.size() > titleCount) return true;
        if (visualPlan == null) return false;
        for (int i = Math.max(0, titleCount - 1); i < lines.size(); i++) {
            if (visualPlan.hasAfter(i)) return true;
        }
        return false;
    }

    /** GUI-label tooltips have no item PostText content, so trailing empty placeholders are inert. */
    static List<String> normalizedSource(List<String> source, boolean emptyStack) {
        if (!emptyStack || source == null || source.size() <= 1) return source;
        int size = source.size();
        while (size > 1) {
            String line = source.get(size - 1);
            if (line != null && !line.trim().isEmpty()) break;
            size--;
        }
        return size == source.size() ? source : new ArrayList<>(source.subList(0, size));
    }

    private static int measure(FontRenderer font, List<String> lines, boolean[] compactSource,
                               float textScale) {
        int width = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineWidth = pipelineWidth(font, line);
            if (lineWidth < 0) lineWidth = renderedWidth(font, line);
            if (compactSource != null && i < compactSource.length && compactSource[i]) {
                lineWidth = (lineWidth + 1) / 2;
            }
            width = Math.max(width, Math.max(1, Math.round(lineWidth * textScale)));
        }
        return width;
    }

    private static int measure(FontRenderer font, List<String> lines, List<Boolean> compactLines,
                               float textScale) {
        boolean[] compact = new boolean[compactLines.size()];
        for (int i = 0; i < compact.length; i++) compact[i] = compactLines.get(i);
        return measure(font, lines, compact, textScale);
    }

    private static List<Integer> measureLineWidths(FontRenderer font, List<String> lines,
                                                    List<Boolean> compactLines, float textScale) {
        List<Integer> widths = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            widths.add(measuredLineWidth(font, lines.get(i), compactLines.get(i), textScale));
        }
        return widths;
    }

    private static int maxWidth(List<Integer> widths) {
        int width = 0;
        for (Integer value : widths) width = Math.max(width, value == null ? 1 : value);
        return Math.max(1, width);
    }

    /** Width shared by TC6 layout, title centering, and the divider's horizontal span. */
    static int measuredLineWidth(FontRenderer font, String line, boolean compact, float textScale) {
        int pipelineWidth = pipelineWidth(font, line);
        if (pipelineWidth >= 0) {
            if (compact) pipelineWidth = (pipelineWidth + 1) / 2;
            return Math.max(1, Math.round(pipelineWidth * textScale));
        }
        int lineWidth = renderedWidth(font, line);
        if (compact) lineWidth = (lineWidth + 1) / 2;
        return Math.max(1, Math.round(lineWidth * textScale));
    }

    /** Match the width used by the active renderer, including shaped visual overhang. */
    private static int renderedWidth(FontRenderer font, String line) {
        int tiqianWidth = CjkTypographyRenderer.measuredVisualWidth(font, line);
        if (tiqianWidth >= 0) return tiqianWidth;
        if (FontManager.INSTANCE.isTextBackendActive() || FontManager.INSTANCE.isSfrActive()) {
            return TooltipBoundsCompat.measuredWidth(font, line);
        }
        return CjkTypographyRenderer.measuredWidth(font, line);
    }

    private static boolean hasInlineContent(FontRenderer font, String line) {
        if (!TextPipelineApi.hasInlineContentMiddleware()) return false;
        try {
            return TextPipelineEngine.layout(font, line).hasInlineContent();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Returns logical inline width, or -1 when the line is ordinary text. */
    private static int pipelineWidth(FontRenderer font, String line) {
        if (!TextPipelineApi.hasInlineContentMiddleware()) return -1;
        try {
            neofontrender.api.text.pipeline.TextPipelineLayout measured =
                    TextPipelineEngine.layout(font, line);
            return measured.hasInlineContent() ? measured.width() : -1;
        } catch (RuntimeException ignored) {
            // Layout measurement must never make a vanilla tooltip fail closed.
            return -1;
        }
    }
}
