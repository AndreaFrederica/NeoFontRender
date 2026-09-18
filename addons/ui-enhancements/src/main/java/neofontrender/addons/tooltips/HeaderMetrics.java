package neofontrender.addons.tooltips;

import net.minecraft.item.ItemStack;

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
        int textOffset = Math.max(0, (height - textHeight) / 2);
        int iconY = y;
        if (TooltipHeaderLayout.hasIcon(stack)) {
            if ("title".equals(TooltipConfig.headerIconAlignment)) {
                // "title" means the visible title text block, including its optional
                // rarity row. This is the useful two-line anchor for left-aligned titles.
                iconY += textOffset + Math.max(0, (textHeight - iconHeight) / 2);
            } else if ("first_line".equals(TooltipConfig.headerIconAlignment)) {
                int firstLineHeight = rawAdvances != null && !rawAdvances.isEmpty()
                        ? Math.max(1, rawAdvances.get(0))
                        : (advances == null || advances.isEmpty()
                        ? TooltipConfig.lineHeight : Math.max(1, advances.get(0)));
                iconY += textOffset + Math.max(0, (firstLineHeight - iconHeight) / 2);
            } else {
                // The default header anchor includes the rarity row when it is enabled.
                iconY += Math.max(0, (height - iconHeight) / 2);
            }
        }
        return new HeaderMetrics(height, textOffset, iconY - y, textHeight);
    }
}
