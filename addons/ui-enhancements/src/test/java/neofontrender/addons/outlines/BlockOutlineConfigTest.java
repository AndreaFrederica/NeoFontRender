package neofontrender.addons.outlines;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void rainbowCyclePhaseWrapsWithoutAFrameDiscontinuity() {
        assertEquals(0.0F, BlockOutlineRenderer.rainbowCyclePhase(0L, 3000.0F), 0.0001F);
        assertEquals(0.5F, BlockOutlineRenderer.rainbowCyclePhase(1500L, 3000.0F), 0.0001F);
        assertEquals(0.0F, BlockOutlineRenderer.rainbowCyclePhase(3000L, 3000.0F), 0.0001F);
    }

    @Test
    void rainbowCornerPhasesUseStableSelectionBoxLocalCoordinates() {
        assertEquals(0.0F, BlockOutlineRenderer.cornerHuePhase(0, 1.0F), 0.0001F);
        assertEquals(0.31F, BlockOutlineRenderer.cornerHuePhase(1, 1.0F), 0.0001F);
        assertEquals(0.43F, BlockOutlineRenderer.cornerHuePhase(2, 1.0F), 0.0001F);
        assertEquals(1.0F, BlockOutlineRenderer.cornerHuePhase(7, 1.0F), 0.0001F);
        assertEquals(2.0F, BlockOutlineRenderer.cornerHuePhase(7, 2.0F), 0.0001F);
    }

    @Test
    void outlineShaderPackagesRainbowAndGlowUniforms() throws Exception {
        String path = "/assets/neofontrender_ui_enhancements/shaders/block_outline.fsh";
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String uniform : Arrays.asList("uRainbowPhase", "uHueStart", "uHueDelta",
                    "uGlowPass", "uGlowRadius", "uGlowIntensity", "uGlowFalloff")) {
                assertTrue(source.contains("uniform float " + uniform + ";"), uniform);
            }
        }
    }
}
