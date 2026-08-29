package neofontrender.addons.outlines;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOutlineConfigTest {
    @Test
    void editorRulesAcceptCommaAndNewlineSeparatedEntries() {
        assertEquals(Arrays.asList(
                        "minecraft:stone=2;#66000000",
                        "minecraft:glass:0=3;#66FFFFFF"),
                BlockOutlineConfig.parseEditorRules(
                        "minecraft:stone=2;#66000000,\nminecraft:glass:0=3;#66FFFFFF"));
    }

    @Test
    void emptyEditorRulesBecomeAnEmptyList() {
        assertEquals(Collections.emptyList(), BlockOutlineConfig.parseEditorRules("  ,\n  "));
        assertEquals("minecraft:stone=2;#66000000, minecraft:glass=1;#66FFFFFF",
                BlockOutlineConfig.editorRules(Arrays.asList(
                        "minecraft:stone=2;#66000000", "minecraft:glass=1;#66FFFFFF")));
    }

    @Test
    void optionValuesAreNormalizedToSafeDefaults() {
        assertEquals(BlockOutlineConfig.MODE_NATIVE, BlockOutlineConfig.normalizeRenderMode("Native"));
        assertEquals(BlockOutlineConfig.MODE_GEOMETRY, BlockOutlineConfig.normalizeRenderMode("unknown"));
        assertEquals(BlockOutlineConfig.PATTERN_DASHED, BlockOutlineConfig.normalizePattern("DASHED"));
        assertEquals(BlockOutlineConfig.PATTERN_SOLID, BlockOutlineConfig.normalizePattern(null));
        assertEquals(BlockOutlineConfig.CAP_SQUARE, BlockOutlineConfig.normalizeCap("square"));
        assertEquals(BlockOutlineConfig.CAP_ROUND, BlockOutlineConfig.normalizeCap("invalid"));
        assertEquals(BlockOutlineConfig.DEPTH_XRAY, BlockOutlineConfig.normalizeDepthMode("xray"));
        assertEquals(BlockOutlineConfig.DEPTH_VISIBLE, BlockOutlineConfig.normalizeDepthMode("invalid"));
        assertEquals(BlockOutlineConfig.BLEND_ADDITIVE, BlockOutlineConfig.normalizeBlendMode("additive"));
        assertEquals(BlockOutlineConfig.BLEND_ALPHA, BlockOutlineConfig.normalizeBlendMode("invalid"));
    }
}
