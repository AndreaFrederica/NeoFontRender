package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import neofontrender.core.font.support.TooltipBoundsCompat;

import java.util.List;

/** Immutable geometry shared by header measurement, painting and diagnostics. */
final class HeaderMetrics {
    final int headerHeight;
    final int textOffset;
    final int iconY;
    final int titleTextHeight;

    private HeaderMetrics(int headerHeight, int textOffset, int iconY, int titleTextHeight) {
        this.headerHeight = headerHeight;
        this.textOffset = textOffset;
        this.iconY = iconY;
        this.titleTextHeight = titleTextHeight;
    }

    static HeaderMetrics measure(ItemStack stack, int titleLines, List<Integer> advances,
                                 int y) {
        java.util.ArrayList<Integer> legacyRaw = new java.util.ArrayList<>();
        if (advances != null) {
            for (Integer value : advances) {
                legacyRaw.add(Math.min(Math.max(1, value == null ? 1 : value), TooltipConfig.lineHeight));
            }
        }
        return measure(stack, titleLines, advances, legacyRaw, y);
    }

    /**
     * Measures the header from the raw text rows. The expanded list reserves icon/rarity
     * space for the outer flow and must not be used as a proxy for glyph height.
     */
    static HeaderMetrics measure(ItemStack stack, int titleLines, List<Integer> advances,
                                 List<Integer> rawAdvances, int y) {
        return measure(stack, titleLines, advances, rawAdvances, y, null, null, 1.0F, null);
    }

    /**
     * Measures the header using the visible font bounds when the retained layout has them.
     * Logical line advances still determine flow, while visual bounds determine optical
     * centering of the icon and title block.
     */
    static HeaderMetrics measure(ItemStack stack, int titleLines, List<Integer> advances,
                                 List<Integer> rawAdvances, int y, FontRenderer font,
                                 List<String> lines, float profileScale,
                                 List<Boolean> compactLines) {
        int count = Math.max(0, Math.min(titleLines, advances == null ? 0 : advances.size()));
        int reserved = 0;
        for (int i = 0; i < count; i++) reserved += Math.max(0, advances.get(i));

        // A line advance can include reserved icon/rarity space. Measure the text block
        // separately so a one-line title is centered beside a 16px icon instead of pinned
        // to the top of the expanded row.
        int titleOnlyHeight = 0;
        for (int i = 0; i < count; i++) {
            int raw = rawAdvances != null && i < rawAdvances.size()
                    ? rawAdvances.get(i) : advances.get(i);
            titleOnlyHeight += Math.max(1, raw);
        }
        titleOnlyHeight = Math.max(1, titleOnlyHeight);
        int textHeight = titleOnlyHeight;
        if (TooltipHeaderLayout.hasRarity(stack)) {
            textHeight += TooltipHeaderLayout.RARITY_HEIGHT
                    + TooltipHeaderLayout.RARITY_BOTTOM_GAP;
        }
        textHeight = Math.max(1, textHeight);

        int iconHeight = TooltipHeaderLayout.hasIcon(stack)
                ? TooltipHeaderLayout.ICON_SIZE : 0;
        int height = Math.max(1, Math.max(reserved, Math.max(textHeight, iconHeight)));
        float visualTop = 0.0F;
        float visualBottom = textHeight;
        float firstVisualTop = 0.0F;
        float firstVisualBottom = titleOnlyHeight;
        boolean hasVisualBounds = font != null && lines != null && !lines.isEmpty() && count > 0;
        if (hasVisualBounds) {
            visualTop = Float.POSITIVE_INFINITY;
            visualBottom = Float.NEGATIVE_INFINITY;
            firstVisualTop = Float.POSITIVE_INFINITY;
            firstVisualBottom = Float.NEGATIVE_INFINITY;
            float cursor = 0.0F;
            for (int i = 0; i < count; i++) {
                String line = i < lines.size() ? lines.get(i) : "";
                boolean compact = compactLines != null && i < compactLines.size()
                        && Boolean.TRUE.equals(compactLines.get(i));
                float scale = profileScale * (compact ? 0.5F : 1.0F);
                TooltipBoundsCompat.VerticalBounds bounds =
                        TooltipBoundsCompat.measuredVerticalBounds(font, line, TooltipConfig.textShadow);
                float top = cursor + bounds.top * scale;
                float bottom = cursor + bounds.bottom * scale;
                visualTop = Math.min(visualTop, top);
                visualBottom = Math.max(visualBottom, bottom);
                if (i == 0) {
                    firstVisualTop = top;
                    firstVisualBottom = bottom;
                }
                cursor += rawAdvances != null && i < rawAdvances.size()
                        ? Math.max(1, rawAdvances.get(i)) : Math.max(1, advances.get(i));
            }
            if (TooltipHeaderLayout.hasRarity(stack)) {
                int rarityY = 0;
                for (int i = 0; i < count; i++) {
                    rarityY += Math.max(1, advances.get(i));
                }
                rarityY -= TooltipHeaderLayout.RARITY_HEIGHT + TooltipHeaderLayout.RARITY_BOTTOM_GAP;
                TooltipBoundsCompat.VerticalBounds rarityBounds =
                        TooltipBoundsCompat.measuredVerticalBounds(font,
                                TooltipHeaderLayout.rarityLabel(stack), TooltipConfig.textShadow);
                visualTop = Math.min(visualTop, rarityY + rarityBounds.top);
                visualBottom = Math.max(visualBottom, rarityY + rarityBounds.bottom);
            }
            if (!Float.isFinite(visualTop) || !Float.isFinite(visualBottom)
                    || visualBottom <= visualTop) {
                hasVisualBounds = false;
            }
        }

        int textOffset = hasVisualBounds
                ? Math.max(0, Math.round((height - (visualBottom - visualTop)) * 0.5F - visualTop))
                : Math.max(0, (height - textHeight) / 2);
        int iconY = y;
        if (TooltipHeaderLayout.hasIcon(stack)) {
            if ("title".equals(TooltipConfig.headerIconAlignment)) {
                // "title" means the visible title text block, including its optional
                // rarity row. This is the useful two-line anchor for left-aligned titles.
                if (hasVisualBounds) {
                    iconY += textOffset + Math.round((visualTop + visualBottom - iconHeight) * 0.5F);
                } else {
                    iconY += textOffset + Math.max(0, (textHeight - iconHeight) / 2);
                }
            } else if ("first_line".equals(TooltipConfig.headerIconAlignment)) {
                int firstLineHeight = rawAdvances != null && !rawAdvances.isEmpty()
                        ? Math.max(1, rawAdvances.get(0))
                        : (advances == null || advances.isEmpty()
                        ? TooltipConfig.lineHeight : Math.max(1, advances.get(0)));
                if (hasVisualBounds) {
                    iconY += textOffset + Math.round(
                            (firstVisualTop + firstVisualBottom - iconHeight) * 0.5F);
                } else {
                    iconY += textOffset + Math.max(0, (firstLineHeight - iconHeight) / 2);
                }
            } else {
                // The default header anchor includes the rarity row when it is enabled.
                iconY += Math.max(0, (height - iconHeight) / 2);
            }
        }
        return new HeaderMetrics(height, textOffset, iconY - y, textHeight);
    }
}
