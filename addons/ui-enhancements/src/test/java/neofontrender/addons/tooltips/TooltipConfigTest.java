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
    void previewPanelAndAlignmentParticipateInSettingsRollback() {
        assertEquals("top", TooltipConfig.normalizeSideAlignment(null));
        assertEquals("top", TooltipConfig.normalizeSideAlignment("unknown"));
        assertEquals("center", TooltipConfig.normalizeSideAlignment(" CENTER "));
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.sideAlignment = "center";
            TooltipConfig.previewPanelFrameEnabled = true;
            TooltipConfig.previewPanelFrameRarityColor = false;
            TooltipConfig.previewPanelFrameColor = 0x80443322;
            TooltipConfig.previewPanelBackgroundEnabled = true;
            TooltipConfig.previewPanelBackgroundColor = 0x20112233;
            TooltipConfig.previewPanelRounded = false;
            TooltipConfig.previewPanelCornerRadius = 8;
            TooltipConfig.Snapshot saved = TooltipConfig.snapshot();
            original.restore();
            saved.restore();
            assertEquals("center", TooltipConfig.sideAlignment);
            assertTrue(TooltipConfig.previewPanelFrameEnabled);
            assertFalse(TooltipConfig.previewPanelFrameRarityColor);
            assertEquals(0x80443322, TooltipConfig.previewPanelFrameColor);
            assertTrue(TooltipConfig.previewPanelBackgroundEnabled);
            assertEquals(0x20112233, TooltipConfig.previewPanelBackgroundColor);
            assertFalse(TooltipConfig.previewPanelRounded);
            assertEquals(8, TooltipConfig.previewPanelCornerRadius);
            assertEquals(0x80443322, TooltipHeaderLayout.frameColor(ItemStack.EMPTY,
                    TooltipConfig.previewPanelFrameColor, false));
            assertEquals(0x80AAB4C4, TooltipHeaderLayout.frameColor(ItemStack.EMPTY,
                    TooltipConfig.previewPanelFrameColor, true));
        } finally {
            original.restore();
        }
    }

    @Test
    void measurementAndSwitchAnimationOptionsParticipateInSettingsRollback() {
        assertEquals("continue", TooltipConfig.normalizeZoomAnimationSwitch(null));
        assertEquals("continue", TooltipConfig.normalizeZoomAnimationSwitch("unknown"));
        assertEquals("restart", TooltipConfig.normalizeZoomAnimationSwitch(" RESTART "));
        assertEquals("instant", TooltipConfig.normalizeZoomAnimationSwitch("INSTANT"));
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.previewMeasureBounds = true;
            TooltipConfig.zoomOverlayMeasureBounds = false;
            TooltipConfig.zoomOverlayAnimationSwitch = "instant";
            TooltipConfig.Snapshot saved = TooltipConfig.snapshot();
            TooltipConfig.previewMeasureBounds = false;
            TooltipConfig.zoomOverlayMeasureBounds = true;
            TooltipConfig.zoomOverlayAnimationSwitch = "restart";
            saved.restore();
            assertTrue(TooltipConfig.previewMeasureBounds);
            assertFalse(TooltipConfig.zoomOverlayMeasureBounds);
            assertEquals("instant", TooltipConfig.zoomOverlayAnimationSwitch);
        } finally {
            original.restore();
        }
    }

    @Test
    void zoomOverlapOrderDefaultsSafelyAndParticipatesInSettingsRollback() {
        assertEquals("below_tooltip", TooltipConfig.normalizeZoomOverlayLayer(null));
        assertEquals("below_tooltip", TooltipConfig.normalizeZoomOverlayLayer("unknown"));
        assertEquals("above_tooltip", TooltipConfig.normalizeZoomOverlayLayer(" ABOVE_TOOLTIP "));
        assertEquals("below_tooltip", TooltipConfig.normalizeZoomOverlayLayer(" BELOW_TOOLTIP "));
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.zoomOverlayLayer = "above_tooltip";
            TooltipConfig.Snapshot saved = TooltipConfig.snapshot();
            TooltipConfig.zoomOverlayLayer = "below_tooltip";
            saved.restore();
            assertEquals("above_tooltip", TooltipConfig.zoomOverlayLayer);
        } finally {
            original.restore();
        }
    }

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
    void snapshotRestoresAdvancedAndPreviewSelectorOptions() {
        boolean oldAdvanced = TooltipConfig.advancedEnabled;
        boolean oldCtrl = TooltipConfig.advancedRequireCtrl;
        boolean oldUnlocalized = TooltipConfig.advancedUnlocalizedName;
        boolean oldMeta = TooltipConfig.advancedMeta;
        boolean oldNbtShift = TooltipConfig.advancedNbtRequireShift;
        int oldLimit = TooltipConfig.advancedNbtCharacterLimit;
        String oldOwnership = TooltipConfig.legendaryOwnership;
        List<String> oldWhitelist = TooltipConfig.previewWhitelist;
        List<String> oldBlacklist = TooltipConfig.previewBlacklist;
        try {
            TooltipConfig.advancedEnabled = true;
            TooltipConfig.advancedRequireCtrl = true;
            TooltipConfig.advancedUnlocalizedName = true;
            TooltipConfig.advancedMeta = true;
            TooltipConfig.advancedNbtRequireShift = true;
            TooltipConfig.advancedNbtCharacterLimit = 120;
            TooltipConfig.legendaryOwnership = "auto";
            TooltipConfig.previewWhitelist = Arrays.asList("minecraft:diamond");
            TooltipConfig.previewBlacklist = Arrays.asList("minecraft:dirt");
            TooltipConfig.Snapshot snapshot = TooltipConfig.snapshot();

            TooltipConfig.advancedEnabled = false;
            TooltipConfig.advancedRequireCtrl = false;
            TooltipConfig.advancedUnlocalizedName = false;
            TooltipConfig.advancedMeta = false;
            TooltipConfig.advancedNbtRequireShift = false;
            TooltipConfig.advancedNbtCharacterLimit = 1;
            TooltipConfig.legendaryOwnership = "uie";
            TooltipConfig.previewWhitelist = Arrays.asList("test:other");
            TooltipConfig.previewBlacklist = Arrays.asList("test:block");

            snapshot.restore();

            assertTrue(TooltipConfig.advancedEnabled);
            assertTrue(TooltipConfig.advancedRequireCtrl);
            assertTrue(TooltipConfig.advancedUnlocalizedName);
            assertTrue(TooltipConfig.advancedMeta);
            assertTrue(TooltipConfig.advancedNbtRequireShift);
            assertEquals(120, TooltipConfig.advancedNbtCharacterLimit);
            assertEquals("auto", TooltipConfig.legendaryOwnership);
            assertEquals(Arrays.asList("minecraft:diamond"), TooltipConfig.previewWhitelist);
            assertEquals(Arrays.asList("minecraft:dirt"), TooltipConfig.previewBlacklist);
        } finally {
            TooltipConfig.advancedEnabled = oldAdvanced;
            TooltipConfig.advancedRequireCtrl = oldCtrl;
            TooltipConfig.advancedUnlocalizedName = oldUnlocalized;
            TooltipConfig.advancedMeta = oldMeta;
            TooltipConfig.advancedNbtRequireShift = oldNbtShift;
            TooltipConfig.advancedNbtCharacterLimit = oldLimit;
            TooltipConfig.legendaryOwnership = oldOwnership;
            TooltipConfig.previewWhitelist = oldWhitelist;
            TooltipConfig.previewBlacklist = oldBlacklist;
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
        try {
            ItemStack stack = new ItemStack(new Item().setRegistryName(
                    new ResourceLocation("test", "alignment")));
            TooltipConfig.titleAlignment = "left";
            int left = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);
            TooltipConfig.titleAlignment = "right";
            int right = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);
            TooltipConfig.titleAlignment = "center";
            int center = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);

            assertEquals(20, left);
            assertEquals(80, right);
            assertEquals(50, center);

            // Rows with different widths must share the same center/right edge rather than
            // being forced to the same left edge.
            int narrowCenter = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);
            int wideCenter = TooltipHeaderLayout.titleTextOffset(stack, 100, 40);
            assertEquals(60, narrowCenter + 10);
            assertEquals(60, wideCenter + 20);

            int narrowRight;
            int wideRight;
            TooltipConfig.titleAlignment = "right";
            narrowRight = TooltipHeaderLayout.titleTextOffset(stack, 100, 20);
            wideRight = TooltipHeaderLayout.titleTextOffset(stack, 100, 40);
            assertEquals(100, narrowRight + 20);
            assertEquals(100, wideRight + 40);
        } finally {
            TooltipConfig.titleAlignment = oldAlignment;
        }
    }

    @Test
    void bodyAlignmentUsesMeasuredWidthForTheFinalTextOrigin() {
        String oldAlignment = TooltipConfig.bodyAlignment;
        try {
            TooltipConfig.bodyAlignment = "left";
            assertEquals(0, TooltipHeaderLayout.bodyTextOffset(100, 20));
            TooltipConfig.bodyAlignment = "center";
            assertEquals(40, TooltipHeaderLayout.bodyTextOffset(100, 20));
            TooltipConfig.bodyAlignment = "right";
            assertEquals(80, TooltipHeaderLayout.bodyTextOffset(100, 20));
        } finally {
            TooltipConfig.bodyAlignment = oldAlignment;
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
