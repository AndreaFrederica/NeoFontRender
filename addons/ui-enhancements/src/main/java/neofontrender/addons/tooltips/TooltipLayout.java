package neofontrender.addons.tooltips;

import neofontrender.api.text.TextVisualBounds;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.core.font.FontManager;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.api.client.tooltip.NfrTooltipApi;

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
    final List<TooltipTextLine> textLines;
    /** Baseline advances before header icon/rarity reservation is added. */
    final List<Integer> rawLineAdvances;
    final List<Integer> lineAdvances;
    final TooltipVisualPlan visualPlan;
    private final TooltipConfig.Profile profile;

    private TooltipLayout(List<String> lines, List<Boolean> compactLines, int titleLines,
                          int x, int y, int width, int height, List<Integer> lineWidths,
                          List<Integer> rawLineAdvances, List<Integer> lineAdvances,
                          int visualTop, int visualBottom,
                          TooltipConfig.Profile profile, TooltipVisualPlan visualPlan, FontRenderer font) {
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
        this.rawLineAdvances = rawLineAdvances;
        this.lineAdvances = lineAdvances;
        this.profile = profile;
        this.visualPlan = visualPlan;
        this.textLines = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            textLines.add(TooltipTextLine.measure(font, lines.get(i),
                    i < titleLines ? TooltipConfig.titleColor : TooltipConfig.textColor,
                    TooltipConfig.textShadow));
        }
    }

    TooltipConfig.Profile profile() { return profile; }

    TooltipPanelBounds panelBounds() {
        return TooltipPanelBounds.of(x, y, width, visualTop, visualBottom);
    }

    /** Builds the same retained composition for the settings screen without a Forge event. */
    static TooltipLayout preview(FontRenderer font, ItemStack stack, List<String> source,
                                 List<Boolean> compactSource, TooltipConfig.Profile profile,
                                 int maxWidth) {
        List<String> safeSource = source == null ? java.util.Collections.<String>emptyList() : source;
        TooltipConfig.Profile active = profile == null ? TooltipConfig.profile("vanilla") : profile;
        List<Boolean> compact = flagsFor(safeSource.size(), compactSource == null ? null
                : toFlags(compactSource));
        List<Integer> widths = measureLineWidths(font, safeSource, compact, active.textScale);
        TooltipVisualPlan visualPlan;
        List<NfrTooltipApi.VisualNode> previous = TooltipVisualPlan.EXTERNAL.get();
        try {
            java.util.Optional<NfrTooltipApi.TooltipDocument> document =
                    BuiltinPreviewProvider.INSTANCE.create(stack, safeSource);
            if (document.isPresent()) TooltipVisualPlan.EXTERNAL.set(document.get().nodes);
            else TooltipVisualPlan.EXTERNAL.remove();
            visualPlan = TooltipVisualPlan.collect(stack, safeSource, font, Math.max(1, maxWidth));
        } finally {
            if (previous == null) TooltipVisualPlan.EXTERNAL.remove();
            else TooltipVisualPlan.EXTERNAL.set(previous);
        }
        visualPlan = visualPlan.constrain(Math.max(1, maxWidth));
        int limit = Math.max(1, maxWidth);
        int width = visualPlanContentWidth(widths, visualPlan, stack, safeSource.isEmpty() ? 0 : 1, font);
        List<String> finalLines = safeSource;
        List<Boolean> finalCompact = compact;
        int titleLines = safeSource.isEmpty() ? 0 : 1;
        if (width > limit && !safeSource.isEmpty()) {
            List<String> wrapped = new ArrayList<>();
            List<Boolean> wrappedCompact = new ArrayList<>();
            List<Integer> last = new ArrayList<>();
            for (int i = 0; i < safeSource.size(); i++) {
                int available = Math.max(1, limit - visualPlan.sideWidth()
                        - (i == 0 ? TooltipHeaderLayout.titleInset(stack) : 0));
                int sourceWidth = finalCompact.get(i)
                        ? Math.max(1, Math.round(available * 2.0F / active.textScale))
                        : Math.max(1, Math.round(available / active.textScale));
                List<String> parts = wrapLine(font, safeSource.get(i), sourceWidth);
                if (i == 0) titleLines = parts.size();
                wrapped.addAll(parts);
                for (int p = 0; p < parts.size(); p++) wrappedCompact.add(finalCompact.get(i));
                last.add(wrapped.size() - 1);
            }
            finalLines = wrapped;
            finalCompact = wrappedCompact;
            visualPlan = visualPlan.remap(last, finalLines.size());
            widths = measureLineWidths(font, finalLines, finalCompact, active.textScale);
            width = Math.min(limit, visualPlanContentWidth(widths, visualPlan, stack, titleLines, font));
        }
        List<Integer> raw = lineAdvances(font, finalLines, finalCompact, active.textScale);
        List<Integer> expanded = new ArrayList<>(raw);
        addHeaderAdvance(expanded, stack, titleLines);
        int height = contentFlowHeight(expanded, titleLines, finalLines, visualPlan);
        int[] bounds = visualBounds(font, finalLines, finalCompact, expanded, titleLines,
                active.textScale, active.offsetY, visualPlan, stack, raw);
        return new TooltipLayout(finalLines, finalCompact, titleLines, 0, 0, width,
                Math.max(1, height), widths, raw, expanded, bounds[0], bounds[1], active, visualPlan, font);
    }

    private static boolean[] toFlags(List<Boolean> values) {
        boolean[] result = new boolean[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = Boolean.TRUE.equals(values.get(i));
        return result;
    }

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
        int leftPadding = TooltipConfig.leftPadding;
        int rightPadding = TooltipConfig.rightPadding;
        int cursorOffset = TooltipConfig.cursorOffset;
        TooltipVisualExtents extents = TooltipVisualExtents.current();
        List<Integer> lineWidths = measureLineWidths(font, source,
                flagsFor(source.size(), compactSource), activeProfile.textScale);
        int width = visualPlanContentWidth(lineWidths, null, event.getStack(), 1, font);
        int x = event.getX() + cursorOffset;
        boolean wrap = false;

        int screenWidthLimit = screenWidthLimit(
                event.getScreenWidth(), leftPadding, rightPadding, extents);
        int contentLimit = screenWidthLimit;
        if (event.getMaxWidth() > 0) contentLimit = Math.min(contentLimit, event.getMaxWidth());
        if (TooltipConfig.maxWidth > 0) contentLimit = Math.min(contentLimit, TooltipConfig.maxWidth);
        TooltipVisualPlan visualPlan = TooltipVisualPlan.collect(
                event.getStack(), source, font, contentLimit);
        visualPlan = visualPlan.constrain(contentLimit);
        width = visualPlan.contentWidth(width);
        if (width > screenWidthLimit) {
            width = screenWidthLimit;
            wrap = true;
        } else if (x + width + rightPadding > event.getScreenWidth()) {
            x = event.getX() - cursorOffset - rightPadding - width;
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
                int textWidth = Math.max(1, width - visualPlan.sideWidth()
                        - (i == 0 ? TooltipHeaderLayout.titleInset(event.getStack()) : 0));
                int sourceWidth = compactLines.get(i) ? Math.max(1, Math.round(textWidth * 2.0F / activeProfile.textScale))
                        : Math.max(1, Math.round(textWidth / activeProfile.textScale));
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
            width = visualPlanContentWidth(lineWidths, visualPlan, event.getStack(), titleLines, font);
            x = event.getX() > event.getScreenWidth() / 2
                    ? event.getX() - cursorOffset - rightPadding - width
                    : event.getX() + cursorOffset;
        }

        List<Integer> rawLineAdvances = lineAdvances(font, lines, compactLines,
                activeProfile.textScale);
        List<Integer> lineAdvances = new ArrayList<>(rawLineAdvances);
        addHeaderAdvance(lineAdvances, event.getStack(), titleLines);
        int height = contentFlowHeight(lineAdvances, titleLines, lines, visualPlan);
        int[] visualBounds = visualBounds(font, lines, compactLines, lineAdvances,
                titleLines, activeProfile.textScale, activeProfile.offsetY, visualPlan, event.getStack(), rawLineAdvances);
        int y = event.getY() - cursorOffset;
        y = Math.max(TooltipConfig.topPadding + extents.top - visualBounds[0], Math.min(y,
                event.getScreenHeight() - visualBounds[1] - TooltipConfig.bottomPadding - extents.bottom));
        x = Math.max(leftPadding + extents.left, Math.min(x,
                event.getScreenWidth() - width - rightPadding - extents.right));
        return new TooltipLayout(lines, compactLines, titleLines, x, y, width, height,
                lineWidths, rawLineAdvances, lineAdvances, visualBounds[0], visualBounds[1], activeProfile,
                visualPlan, font);
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
        int leftPadding = TooltipConfig.leftPadding;
        int rightPadding = TooltipConfig.rightPadding;
        TooltipVisualExtents extents = TooltipVisualExtents.current();
        List<String> source = event.getLines();
        List<Boolean> sourceCompact = flagsFor(source.size(), compactSource);
        List<Integer> lineWidths = measureLineWidths(font, source, sourceCompact,
                activeProfile.textScale);
        int width = visualPlanContentWidth(lineWidths, null, event.getStack(), 1, font);
        int maxWidth = event.getMaxWidth() > 0 ? event.getMaxWidth() : 240;
        if (TooltipConfig.maxWidth > 0) maxWidth = Math.min(maxWidth, TooltipConfig.maxWidth);
        maxWidth = Math.min(maxWidth, screenWidthLimit(
                event.getScreenWidth(), leftPadding, rightPadding, extents));
        TooltipVisualPlan visualPlan = TooltipVisualPlan.collect(
                event.getStack(), source, font, maxWidth);
        width = visualPlanContentWidth(lineWidths, visualPlan, event.getStack(), 1, font);

        boolean placeLeft = context.right;
        if (!placeLeft && context.cursorX + width + 24 > event.getScreenWidth()) {
            placeLeft = true;
        }
        int available = placeLeft ? context.cursorX - 24 - 8
                : event.getScreenWidth() - context.cursorX - 24;
        if (available > 0 && width > available) maxWidth = Math.min(maxWidth, available);
        visualPlan = visualPlan.constrain(maxWidth);
        width = visualPlan.contentWidth(maxWidth(lineWidths));
        boolean wrap = width > maxWidth;

        List<String> lines = source;
        List<Boolean> compactLines = sourceCompact;
        int titleLines = source.isEmpty() ? 0 : 1;
        if (wrap) {
            List<String> wrapped = new ArrayList<>();
            List<Boolean> wrappedCompact = new ArrayList<>();
            List<Integer> lastWrappedLineBySource = new ArrayList<>(source.size());
            for (int i = 0; i < source.size(); i++) {
                int textWidth = Math.max(1, maxWidth - visualPlan.sideWidth()
                        - (i == 0 ? TooltipHeaderLayout.titleInset(event.getStack()) : 0));
                int sourceWidth = sourceCompact.get(i)
                        ? Math.max(1, Math.round(textWidth * 2.0F / activeProfile.textScale))
                        : Math.max(1, Math.round(textWidth / activeProfile.textScale));
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
            width = visualPlanContentWidth(lineWidths, visualPlan, event.getStack(), titleLines, font);
        }

        int x = placeLeft ? context.cursorX - width - 24 : context.cursorX + 12;
        List<Integer> rawLineAdvances = lineAdvances(font, lines, compactLines,
                activeProfile.textScale);
        List<Integer> lineAdvances = new ArrayList<>(rawLineAdvances);
        addHeaderAdvance(lineAdvances, event.getStack(), titleLines);
        int height = contentFlowHeight(lineAdvances, titleLines, lines, visualPlan);
        int[] visualBounds = visualBounds(font, lines, compactLines, lineAdvances,
                titleLines, activeProfile.textScale, activeProfile.offsetY, visualPlan, event.getStack(), rawLineAdvances);
        int y = context.cursorY - 12;
        y = Math.max(TooltipConfig.topPadding + extents.top - visualBounds[0], Math.min(y,
                event.getScreenHeight() - visualBounds[1] - TooltipConfig.bottomPadding - extents.bottom));
        x = Math.max(leftPadding + extents.left, Math.min(x,
                event.getScreenWidth() - width - rightPadding - extents.right));
        return new TooltipLayout(lines, compactLines, titleLines, x, y, width, height,
                lineWidths, rawLineAdvances, lineAdvances, visualBounds[0], visualBounds[1], activeProfile,
                visualPlan, font);
    }

    private static int[] visualBounds(FontRenderer font, List<String> lines,
                                      List<Boolean> compactLines, List<Integer> lineAdvances,
                                      int titleLines, float profileScale, float offsetY,
                                      TooltipVisualPlan visualPlan, ItemStack stack, List<Integer> rawAdvances) {
        float top = Float.POSITIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        float baseline = 0.0F;
        int titleCount = Math.max(0, Math.min(titleLines, lines.size()));
        HeaderMetrics header = HeaderMetrics.measure(stack, titleCount, lineAdvances,
                rawAdvances, 0, font, lines, profileScale, compactLines);
        for (int i = 0; i < lines.size(); i++) {
            boolean compact = compactLines.get(i);
            float scale = profileScale * (compact ? 0.5F : 1.0F);
            String line = lines.get(i);
            float lineTop = 0.0F;
            float lineBottom;
            if (line == null || line.trim().isEmpty()
                    || NfrTooltipAnchor.isAnchorLine(line)) {
                lineBottom = lineAdvances.get(i);
            } else {
                TextVisualBounds measured = TooltipTextLine.measure(font, line,
                        i < titleCount ? TooltipConfig.titleColor : TooltipConfig.textColor,
                        TooltipConfig.textShadow).bounds;
                lineTop = measured.top * scale;
                lineBottom = measured.bottom * scale;
            }
            // Use the same final title origin as TooltipContentLayout, including optical
            // centering. Measuring at the unshifted baseline makes single-line text sit low.
            float textOrigin = baseline + (i < titleCount ? header.textOffset : 0) + offsetY;
            top = Math.min(top, textOrigin + lineTop);
            bottom = Math.max(bottom, textOrigin + lineBottom);
            baseline += lineAdvances.get(i);
            if (i + 1 == titleCount && TooltipHeaderLayout.hasRarity(stack)) {
                float rarityY = baseline + header.textOffset
                        - TooltipHeaderLayout.RARITY_HEIGHT - TooltipHeaderLayout.RARITY_BOTTOM_GAP;
                TextVisualBounds rarityBounds = TooltipHeaderLayout.measureRarity(stack, font).bounds;
                top = Math.min(top, rarityY + rarityBounds.top);
                bottom = Math.max(bottom, rarityY + rarityBounds.bottom);
            }
            if (i + 1 == titleCount && TooltipConfig.titleBreak
                    && hasContentAfterTitle(lines, titleCount, visualPlan)) {
                float dividerY = baseline + TooltipConfig.dividerTopMargin;
                top = Math.min(top, dividerY);
                bottom = Math.max(bottom, dividerY + 1);
                baseline += dividerSpacing();
            }
            for (TooltipVisualBlock block : visualPlan.after(i)) {
                top = Math.min(top, baseline);
                baseline += block.height();
                bottom = Math.max(bottom, baseline);
            }
        }
        if (TooltipHeaderLayout.hasIcon(stack)) {
            top = Math.min(top, header.iconY - TooltipHeaderLayout.iconDecorationInset());
            bottom = Math.max(bottom, header.iconY + TooltipHeaderLayout.ICON_SIZE
                    + TooltipHeaderLayout.iconDecorationInset());
        }
        if (visualPlan != null && visualPlan.sideHeight() > 0) {
            top = Math.min(top, 0.0F);
            bottom = Math.max(bottom, visualPlan.sideHeight());
        }
        if (!Float.isFinite(top) || !Float.isFinite(bottom) || bottom <= top) {
            return roundedVisualBounds(0, Math.max(1, baseline));
        }
        return roundedVisualBounds(top, bottom);
    }

    static int[] roundedVisualBounds(float top, float bottom) {
        return new int[]{(int) Math.floor(top), (int) Math.ceil(bottom)};
    }

    static int dividerSpacing() {
        return TooltipConfig.titleBreak
                ? Math.max(0, TooltipConfig.dividerTopMargin)
                + 1 + Math.max(0, TooltipConfig.dividerBottomMargin) : 0;
    }

    private static int contentFlowHeight(List<Integer> lineAdvances, int titleLines,
                                         List<String> lines, TooltipVisualPlan visualPlan) {
        int textHeight = lineAdvances == null ? 0
                : lineAdvances.stream().mapToInt(Integer::intValue).sum();
        int visualHeight = visualPlan == null ? 0 : visualPlan.totalHeight();
        int planHeight = visualPlan == null ? 0 : visualPlan.contentHeight();
        if (hasContentAfterTitle(lines, titleLines, visualPlan)) textHeight += dividerSpacing();
        int height = Math.max(textHeight + visualHeight, planHeight);
        return Math.max(1, height);
    }

    private static int visualPlanContentWidth(List<Integer> lineWidths,
                                              TooltipVisualPlan visualPlan,
                                              ItemStack stack, int titleLines,
                                              FontRenderer font) {
        int width = maxWidth(lineWidths);
        if (lineWidths != null && !lineWidths.isEmpty()) {
            int titleWidth = 1;
            for (int index = 0; index < Math.min(titleLines, lineWidths.size()); index++) {
                Integer lineWidth = lineWidths.get(index);
                titleWidth = Math.max(titleWidth, lineWidth == null ? 1 : lineWidth);
            }
            width = Math.max(width,
                    TooltipHeaderLayout.requiredContentWidth(titleWidth, stack, font));
        }
        return visualPlan == null ? width : visualPlan.contentWidth(width);
    }

    private static void addHeaderAdvance(List<Integer> advances,
                                         ItemStack stack, int titleLines) {
        if (advances == null || advances.isEmpty()) return;
        int titleCount = Math.max(1, Math.min(titleLines, advances.size()));
        int titleHeight = 0;
        for (int index = 0; index < titleCount; index++) titleHeight += advances.get(index);
        int extra = TooltipHeaderLayout.extraHeight(stack, titleHeight);
        int last = titleCount - 1;
        advances.set(last, advances.get(last) + extra);
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
            if (NfrTooltipAnchor.isAnchorLine(lines.get(i))) {
                // Marker lines carry position only; anchored content advances on its own.
                result.add(0);
                continue;
            }
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
        try {
            TextRenderRouteLayout measured = TextRenderRouteApi.layout(font, line);
            return measured.hasInlineContent() ? Math.round(measured.height()) : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    /**
     * Keep inline middleware tokens atomic while deciding tooltip line breaks. Calling the
     * vanilla wrapper here would split a long SVG/LaTeX token before the text pipeline sees it.
     */
    static List<String> wrapLine(FontRenderer font, String line, int width) {
        if (NfrTooltipAnchor.isAnchorLine(line)) return java.util.Collections.singletonList(line);
        if (!hasInlineContent(font, line)) {
            List<String> tiqian = CjkTypographyRenderer.wrap(
                    font, line, width, TooltipConfig.lineHeight);
            if (tiqian != null) return tiqian;
        }
        return hasInlineContent(font, line)
                ? TextRenderRouteApi.wrap(font, line, width)
                : font.listFormattedStringToWidth(line, width);
    }

    static int screenWidthLimit(int screenWidth, int leftPadding, int rightPadding,
                                TooltipVisualExtents extents) {
        int left = extents == null ? 0 : extents.left;
        int right = extents == null ? 0 : extents.right;
        return Math.max(1, screenWidth - Math.max(0, leftPadding) - Math.max(0, rightPadding) - left - right);
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
        if (NfrTooltipAnchor.isAnchorLine(line)) return 0;
        return Math.max(1, (int) Math.ceil(measuredLineBounds(font, line, compact, textScale).width()));
    }

    static TextVisualBounds measuredLineBounds(
            FontRenderer font, String line, boolean compact, float textScale) {
        return TooltipTextLine.measure(font, line, 0xFFFFFFFF, TooltipConfig.textShadow)
                .bounds.scale(textScale * (compact ? 0.5F : 1.0F));
    }

    static float measuredLineVisualLeft(FontRenderer font, String line, boolean compact, float textScale) {
        return measuredLineBounds(font, line, compact, textScale).left;
    }

    private static boolean hasInlineContent(FontRenderer font, String line) {
        try {
            return TextRenderRouteApi.layout(font, line).hasInlineContent();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

}
