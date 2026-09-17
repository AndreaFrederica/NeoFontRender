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
        int count = Math.max(0, Math.min(titleLines, advances == null ? 0 : advances.size()));
        int reserved = 0;
        for (int i = 0; i < count; i++) reserved += Math.max(0, advances.get(i));

        // A line advance can include reserved icon/rarity space. Measure the text block
        // separately so a one-line title is centered beside a 16px icon instead of pinned
        // to the top of the expanded row.
        int textHeight = 0;
        for (int i = 0; i < count; i++) {
            textHeight += Math.min(Math.max(1, advances.get(i)), TooltipConfig.lineHeight);
        }
        if (TooltipHeaderLayout.hasRarity(stack)) {
            textHeight += TooltipHeaderLayout.RARITY_HEIGHT
                    + TooltipHeaderLayout.RARITY_BOTTOM_GAP;
        }
        textHeight = Math.max(1, textHeight);

        int iconHeight = TooltipHeaderLayout.hasIcon(stack)
                ? TooltipHeaderLayout.ICON_SIZE : 0;
        int height = Math.max(1, Math.max(reserved, Math.max(textHeight, iconHeight)));
        int textOffset = Math.max(0, (height - textHeight) / 2);
        int iconY = TooltipHeaderLayout.hasIcon(stack)
                ? y + Math.max(0, (height - iconHeight) / 2) : y;
        return new HeaderMetrics(height, textOffset, iconY - y, textHeight);
    }
}
