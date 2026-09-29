package neofontrender.addons.tooltips;

import neofontrender.api.text.TextVisualBounds;

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
    final TooltipTextLine rarity;
    final float rarityX, rarityY;
    final List<Row> rows;
    final List<BlockPlacement> blocks;

    private TooltipContentLayout(int sideWidth, int sideHeight, HeaderMetrics header,
                                 List<Row> rows, List<BlockPlacement> blocks,
                                 TooltipTextLine rarity, float rarityX, float rarityY) {
        this.sideWidth = sideWidth;
        this.sideHeight = sideHeight;
        this.header = header;
        this.rarity = rarity; this.rarityX = rarityX; this.rarityY = rarityY;
        this.rows = Collections.unmodifiableList(rows);
        this.blocks = Collections.unmodifiableList(blocks);
    }

    static TooltipContentLayout build(TooltipLayout layout, FontRenderer font, ItemStack stack) {
        TooltipVisualPlan plan = layout.visualPlan;
        int sideWidth = plan == null ? 0 : plan.sideWidth();
        int sideHeight = plan == null ? 0 : plan.sideHeight();
        int titleCount = Math.max(0, Math.min(layout.titleLines, layout.lines.size()));
        HeaderMetrics header = HeaderMetrics.measure(stack, titleCount, layout.lineAdvances,
                layout.rawLineAdvances, layout.y, font, layout.lines,
                layout.profile().textScale, layout.compactLines);
        int contentWidth = Math.max(1, layout.width - sideWidth);
        int rowY = layout.y;
        int textY = layout.y + header.textOffset;
        List<Row> rows = new ArrayList<>(layout.lines.size());
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int i = 0; i < layout.lines.size(); i++) {
            String line = layout.lines.get(i);
            int advance = i < layout.lineAdvances.size() ? layout.lineAdvances.get(i) : 0;
            boolean title = i < titleCount;
            TooltipTextLine measured = layout.textLines.get(i);
            float scale = layout.profile().textScale * (layout.compactLines.get(i) ? 0.5F : 1);
            TextVisualBounds bounds = measured.bounds.scale(scale);
            int renderedWidth = (int) Math.ceil(bounds.width());
            int inset = title ? TooltipHeaderLayout.titleInset(stack) : 0;
            float textX = layout.x + sideWidth + inset + TooltipTextLine.alignedOrigin(
                    Math.max(1, contentWidth - inset), bounds,
                    title ? TooltipConfig.titleAlignment : TooltipConfig.bodyAlignment);
            boolean anchor = NfrTooltipAnchor.isAnchorLine(line);
            rows.add(new Row(i, layout.x + sideWidth, rowY, contentWidth, Math.max(0, advance),
                    textX, textY, renderedWidth, title, measured));
            if (!anchor && i + 1 == titleCount && TooltipConfig.titleBreak
                    && hasContentAfterTitle(layout.lines, titleCount, plan)) {
                int dividerY = Math.round(textY + advance - header.textOffset
                        + TooltipConfig.dividerTopMargin);
                rows.get(rows.size() - 1).dividerY = dividerY;
            }

            rowY += advance;
            textY += advance;
            if (i + 1 == titleCount) {
                if (TooltipConfig.titleBreak && hasContentAfterTitle(layout.lines, titleCount, plan)) {
                    rowY += TooltipLayout.dividerSpacing();
                    textY += TooltipLayout.dividerSpacing();
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
        TooltipTextLine rarity = null;
        float rarityX = 0, rarityY = 0;
        if (titleCount > 0 && TooltipHeaderLayout.hasRarity(stack)) {
            rarity = TooltipHeaderLayout.measureRarity(stack, font);
            int inset = TooltipHeaderLayout.titleInset(stack);
            rarityX = layout.x + sideWidth + inset + TooltipTextLine.alignedOrigin(
                    Math.max(1, contentWidth - inset), rarity.bounds, TooltipConfig.rarityAlignment);
            Row lastTitle = rows.get(titleCount - 1);
            rarityY = lastTitle.textY + Math.max(0, lastTitle.height
                    - TooltipHeaderLayout.RARITY_HEIGHT - TooltipHeaderLayout.RARITY_BOTTOM_GAP);
        }
        return new TooltipContentLayout(sideWidth, sideHeight, header, rows, blocks, rarity, rarityX, rarityY);
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
        final int index, x, y, width, height, textY, textWidth;
        final float textX;
        final TooltipTextLine text;
        final boolean title;
        int dividerY = Integer.MIN_VALUE;

        Row(int index, int x, int y, int width, int height, float textX, int textY,
            int textWidth, boolean title, TooltipTextLine text) {
            this.index = index; this.x = x; this.y = y; this.width = width; this.height = height;
            this.textX = textX; this.textY = textY; this.textWidth = textWidth; this.title = title;
            this.text = text;
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
