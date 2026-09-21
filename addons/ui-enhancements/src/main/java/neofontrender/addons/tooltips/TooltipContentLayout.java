package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The retained placement result consumed by both painting and the F3 inspector.  Text and
 * visual nodes are measured once by {@link TooltipLayout}; this class only assigns their final
 * relative rectangles, so diagnostics cannot drift away from the pixels on screen.
 */
final class TooltipContentLayout {
    final int sideWidth;
    final int sideHeight;
    final HeaderMetrics header;
    final List<Row> rows;
    final List<BlockPlacement> blocks;

    private TooltipContentLayout(int sideWidth, int sideHeight, HeaderMetrics header,
                                 List<Row> rows, List<BlockPlacement> blocks) {
        this.sideWidth = sideWidth;
        this.sideHeight = sideHeight;
        this.header = header;
        this.rows = Collections.unmodifiableList(rows);
        this.blocks = Collections.unmodifiableList(blocks);
    }

    static TooltipContentLayout build(TooltipLayout layout, FontRenderer font, ItemStack stack,
                                      boolean lineBreaksAlreadyApplied) {
        TooltipVisualPlan plan = layout.visualPlan;
        int sideWidth = plan == null ? 0 : plan.sideWidth();
        int sideHeight = plan == null ? 0 : plan.sideHeight();
        int titleCount = Math.max(0, Math.min(layout.titleLines, layout.lines.size()));
        HeaderMetrics header = HeaderMetrics.measure(stack, titleCount, layout.lineAdvances,
                layout.rawLineAdvances, layout.y);
        int contentWidth = Math.max(1, layout.width - sideWidth);
            int rowY = layout.y;
        int textY = layout.y + header.textOffset;
        List<Row> rows = new ArrayList<>(layout.lines.size());
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int i = 0; i < layout.lines.size(); i++) {
            String line = layout.lines.get(i);
            int advance = i < layout.lineAdvances.size() ? layout.lineAdvances.get(i) : 0;
            boolean title = i < titleCount;
            int renderedWidth = i < layout.lineWidths.size() ? layout.lineWidths.get(i)
                    : TooltipLayout.measuredLineWidth(font, line, false, layout.profile().textScale);
            int textX = layout.x + sideWidth
                    + (title ? TooltipHeaderLayout.titleTextOffset(stack, contentWidth, font, line,
                    i < layout.compactLines.size() && layout.compactLines.get(i),
                    layout.profile().textScale) : 0);
            boolean anchor = NfrTooltipAnchor.isAnchorLine(line);
            rows.add(new Row(i, layout.x + sideWidth, rowY, contentWidth, Math.max(0, advance),
                    anchor ? textX : textX, textY, renderedWidth, title));
            if (!anchor && i + 1 == titleCount && TooltipConfig.titleBreak
                    && hasContentAfterTitle(layout.lines, titleCount, plan)) {
                int dividerY = Math.round(textY + advance - header.textOffset - 1.5F);
                rows.get(rows.size() - 1).dividerY = dividerY;
            }

            rowY += advance;
            textY += advance;
            if (i + 1 == titleCount) {
                if (TooltipConfig.titleBreak && hasContentAfterTitle(layout.lines, titleCount, plan)) {
                    rowY += TooltipConfig.titleGap;
                    textY += TooltipConfig.titleGap;
                }
                textY -= header.textOffset;
            }
            if (plan != null) {
                for (TooltipVisualBlock block : plan.after(i)) {
                    blocks.add(new BlockPlacement(i, layout.x + sideWidth, textY, block));
                    rowY += block.height();
                    textY += block.height();
                }
            }
        }
        return new TooltipContentLayout(sideWidth, sideHeight, header, rows, blocks);
    }

    private static boolean hasContentAfterTitle(List<String> lines, int titleLines,
                                                TooltipVisualPlan plan) {
        int count = Math.max(0, Math.min(titleLines, lines.size()));
        if (lines.size() > count) return true;
        if (plan == null) return false;
        for (int i = Math.max(0, count - 1); i < lines.size(); i++) {
            if (plan.hasAfter(i)) return true;
        }
        return false;
    }

    static final class Row {
        final int index, x, y, width, height, textX, textY, textWidth;
        final boolean title;
        int dividerY = Integer.MIN_VALUE;

        Row(int index, int x, int y, int width, int height, int textX, int textY,
            int textWidth, boolean title) {
            this.index = index; this.x = x; this.y = y; this.width = width; this.height = height;
            this.textX = textX; this.textY = textY; this.textWidth = textWidth; this.title = title;
        }
    }

    static final class BlockPlacement {
        final int line, x, y;
        final TooltipVisualBlock block;
        BlockPlacement(int line, int x, int y, TooltipVisualBlock block) {
            this.line = line; this.x = x; this.y = y; this.block = block;
        }
    }
}
