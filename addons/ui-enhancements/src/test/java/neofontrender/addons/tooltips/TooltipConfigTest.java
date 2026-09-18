package neofontrender.addons.tooltips;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TooltipConfigTest {
    @Test
    void normalizesKnownAndUnknownStyles() {
        assertEquals("modernui", TooltipConfig.normalizeStyle("modernui"));
        assertEquals("mica", TooltipConfig.normalizeStyle("MICA"));
        assertEquals("legacy", TooltipConfig.normalizeStyle("LEGACY"));
        assertEquals("modernui", TooltipConfig.normalizeStyle("unknown"));
        assertEquals("modernui", TooltipConfig.normalizeStyle(null));
        assertEquals("gradient", TooltipConfig.normalizeBorderShading(null));
        assertEquals("horizontal", TooltipConfig.normalizeBorderShading("HORIZONTAL"));
        assertEquals("spectrum", TooltipConfig.normalizeBorderShading("spectrum"));
        assertEquals("left", TooltipConfig.normalizeTitleAlignment("LEFT"));
        assertEquals("right", TooltipConfig.normalizeTitleAlignment("right"));
        assertEquals("center", TooltipConfig.normalizeTitleAlignment("unknown"));
        assertEquals("title", TooltipConfig.normalizeHeaderIconAlignment("TITLE"));
        assertEquals("first_line", TooltipConfig.normalizeHeaderIconAlignment("first_line"));
        assertEquals("header", TooltipConfig.normalizeHeaderIconAlignment("unknown"));
    }

    @Test
    void parsesArgbAndRgbColors() {
        assertEquals(0x7F123456, TooltipConfig.parseColor("#7F123456", 0));
        assertEquals(0xFF123456, TooltipConfig.parseColor("123456", 0));
        assertEquals(0xCAFEBABE, TooltipConfig.parseColor("bad color", 0xCAFEBABE));
    }

    @Test
    void snapshotRestoresMicaOptions() {
        boolean original = TooltipConfig.lowBrightnessMicaEnhancement;
        boolean originalSampleUi = TooltipConfig.micaSampleUi;
        try {
            TooltipConfig.lowBrightnessMicaEnhancement = true;
            TooltipConfig.micaSampleUi = true;
            TooltipConfig.Snapshot snapshot = TooltipConfig.snapshot();
            TooltipConfig.lowBrightnessMicaEnhancement = false;
            TooltipConfig.micaSampleUi = false;

            snapshot.restore();

            assertTrue(TooltipConfig.lowBrightnessMicaEnhancement);
            assertTrue(TooltipConfig.micaSampleUi);
        } finally {
            TooltipConfig.lowBrightnessMicaEnhancement = original;
            TooltipConfig.micaSampleUi = originalSampleUi;
        }
    }

    @Test
    void snapshotRestoresArmorStandBasePlateOption() {
        boolean original = TooltipConfig.armorStandBasePlate;
        try {
            TooltipConfig.armorStandBasePlate = true;
            TooltipConfig.Snapshot snapshot = TooltipConfig.snapshot();
            TooltipConfig.armorStandBasePlate = false;

            snapshot.restore();

            assertTrue(TooltipConfig.armorStandBasePlate);
        } finally {
            TooltipConfig.armorStandBasePlate = original;
        }
    }

    @Test
    void mapsEveryVanillaRarityToAnAddonTranslation() {
        assertEquals("neofontrender_ui_enhancements.tooltip.rarity.common",
                TooltipHeaderLayout.rarityTranslationKey(EnumRarity.COMMON));
        assertEquals("neofontrender_ui_enhancements.tooltip.rarity.uncommon",
                TooltipHeaderLayout.rarityTranslationKey(EnumRarity.UNCOMMON));
        assertEquals("neofontrender_ui_enhancements.tooltip.rarity.rare",
                TooltipHeaderLayout.rarityTranslationKey(EnumRarity.RARE));
        assertEquals("neofontrender_ui_enhancements.tooltip.rarity.epic",
                TooltipHeaderLayout.rarityTranslationKey(EnumRarity.EPIC));
    }

    @Test
    void titleAlignmentUsesOneSharedTextOriginForTitleAndRarity() {
        String oldAlignment = TooltipConfig.titleAlignment;
        boolean oldCenter = TooltipConfig.centerTitle;
        try {
            ItemStack stack = new ItemStack(new Item().setRegistryName(
                    new ResourceLocation("test", "alignment")));
            TooltipConfig.centerTitle = false;
            TooltipConfig.titleAlignment = "left";
            int left = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);
            TooltipConfig.titleAlignment = "right";
            int right = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);
            TooltipConfig.titleAlignment = "center";
            TooltipConfig.centerTitle = true;
            int center = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);

            assertEquals(20, left);
            assertEquals(80, right);
            assertEquals(50, center);
        } finally {
            TooltipConfig.titleAlignment = oldAlignment;
            TooltipConfig.centerTitle = oldCenter;
        }
    }

    @Test
    void formatsAndDeduplicatesModNames() {
        assertEquals("\u00a79\u00a7o", ModNameTooltipSupport.format("blue italic"));
        assertEquals("", ModNameTooltipSupport.format("unknown reset"));
        assertTrue(ModNameTooltipSupport.containsModName(
                Arrays.asList("Item", "\u00a79\u00a7oMinecraft"), "Minecraft"));
        assertTrue(ModNameTooltipSupport.containsModName(
                Arrays.asList("Item", "  \u00a79\u00a7oMinecraft  "), "Minecraft"));
        assertFalse(ModNameTooltipSupport.containsModName(
                Arrays.asList("Item", "minecraft:stone"), "Minecraft"));
    }

    @Test
    void movesModNameAfterLinesAddedLater() {
        List<String> lines = new ArrayList<>(Arrays.asList(
                "Iron Ingot", "\u00a79\u00a7oMinecraft", "1"));

        assertTrue(ModNameTooltipSupport.moveModNameToEnd(lines, "Minecraft"));
        assertEquals(Arrays.asList("Iron Ingot", "1", "\u00a79\u00a7oMinecraft"), lines);
        assertFalse(ModNameTooltipSupport.moveModNameToEnd(lines, "Minecraft"));
    }
}
