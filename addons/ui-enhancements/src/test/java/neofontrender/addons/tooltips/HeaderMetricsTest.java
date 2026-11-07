package neofontrender.addons.tooltips;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderMetricsTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void iconOnlyHeaderCentersSingleLineText() {
        boolean oldRarity = TooltipConfig.rarityEnabled;
        TooltipConfig.rarityEnabled = false;
        try {
        ItemStack stack = new ItemStack(new Item().setRegistryName(
                new ResourceLocation("test", "header")));
        HeaderMetrics metrics = HeaderMetrics.measure(stack, 1,
                Collections.singletonList(TooltipHeaderLayout.MIN_HEIGHT), 0);

        assertEquals(TooltipHeaderLayout.MIN_HEIGHT, metrics.headerHeight);
        assertEquals(6, metrics.textOffset);
        assertEquals(3, metrics.iconY);
        } finally {
            TooltipConfig.rarityEnabled = oldRarity;
        }
    }

    @Test
    void headerWithoutIconUsesTextHeight() {
        boolean old = TooltipConfig.headerIconEnabled;
        try {
            TooltipConfig.headerIconEnabled = false;
            HeaderMetrics metrics = HeaderMetrics.measure(ItemStack.EMPTY, 1,
                    Collections.singletonList(TooltipConfig.lineHeight), 10);
            assertEquals(TooltipConfig.lineHeight, metrics.headerHeight);
            assertEquals(0, metrics.textOffset);
            assertEquals(0, metrics.iconY);
        } finally {
            TooltipConfig.headerIconEnabled = old;
        }
    }

    @Test
    void iconAnchorCanCenterTheTitleWithoutTheRarityRow() {
        boolean oldRarity = TooltipConfig.rarityEnabled;
        String oldAlignment = TooltipConfig.headerIconAlignment;
        try {
            ItemStack stack = new ItemStack(new Item().setRegistryName(
                    new ResourceLocation("test", "header_anchor")));
            TooltipConfig.rarityEnabled = true;
            TooltipConfig.headerIconAlignment = "header";
            HeaderMetrics header = HeaderMetrics.measure(stack, 1,
                    Collections.singletonList(22), Collections.singletonList(10), 0);
            TooltipConfig.headerIconAlignment = "title";
            HeaderMetrics title = HeaderMetrics.measure(stack, 1,
                    Collections.singletonList(22), Collections.singletonList(10), 0);

            assertEquals(3, header.iconY);
            assertEquals(3, title.iconY);
            assertEquals(header.headerHeight, title.headerHeight);
        } finally {
            TooltipConfig.rarityEnabled = oldRarity;
            TooltipConfig.headerIconAlignment = oldAlignment;
        }
    }
}
