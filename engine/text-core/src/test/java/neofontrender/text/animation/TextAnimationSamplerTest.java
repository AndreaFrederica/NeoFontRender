package neofontrender.text.animation;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.StandardSyntaxEngines;
import neofontrender.text.syntax.TextAnimatorCompatibilityProvider;
import neofontrender.text.syntax.TextSyntaxEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextAnimationSamplerTest {
    @Test
    void nestedEffectsApplyInSourceModsReverseOrder() {
        TextAnimationEngine.GlyphAnimation glyph = glyph("<wave><rainb>X</rainb></wave>");
        assertEquals("textanimator:rainb", glyph.effects().get(0).effectId());
        assertEquals("textanimator:wave", glyph.effects().get(1).effectId());
    }

    @Test
    void wordTypewriterRevealsOneLineBreakUnitAtOriginalRate() {
        TextSyntaxEngine engine = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true, "all", 5,
                        "by_word"))
                .build();
        StructuredText text = engine.parse("<typewriter>unique word test</typewriter>");
        TextAnimationEngine.GlyphAnimationFrame frame = frame(text);
        TextAnimationSampler sampler = new TextAnimationSampler();
        long now = 8_000L;
        assertFalse(sampler.sample(frame.glyphs().get(0), now).visible);
        assertTrue(sampler.sample(frame.glyphs().get(0), now + 21L).visible);
        assertFalse(sampler.sample(frame.glyphs().get(7), now + 21L).visible);
        sampler.sample(frame.glyphs().get(0), now + 42L);
        sampler.sample(frame.glyphs().get(0), now + 63L);
        sampler.sample(frame.glyphs().get(0), now + 84L);
        assertFalse(sampler.sample(frame.glyphs().get(7), now + 105L).visible);
        assertTrue(sampler.sample(frame.glyphs().get(7), now + 126L).visible);
    }

    @Test
    void outerTypewriterGatesAnInnerAnimationSpan() {
        StructuredText text = parse("<typewriter><wave>nested-typewriter-regression</wave></typewriter>");
        assertEquals(2, text.effects().size());
        assertEquals("textanimator:wave", text.effects().get(0).effectId());
        assertEquals("textanimator:typewriter", text.effects().get(1).effectId());

        TextAnimationEngine.GlyphAnimationFrame frame = frame(text);
        TextAnimationSampler sampler = new TextAnimationSampler();
        long now = 90_000L;
        assertFalse(sampler.sample(frame.glyphs().get(0), now).visible);
        assertTrue(sampler.sample(frame.glyphs().get(0), now + 21L).visible);
    }

    @Test
    void identicalTypewriterTextUsesIndependentInstanceTimelines() {
        StructuredText text = parse("<typewriter>duplicate-instance-regression</typewriter>");
        long firstId = TextAnimationFrame.allocateInstanceId();
        long secondId = TextAnimationFrame.allocateInstanceId();
        TextAnimationEngine engine = new TextAnimationEngine();
        TextAnimationEngine.GlyphAnimation first = engine.frame(text, 0L, firstId).glyphs().get(0);
        TextAnimationEngine.GlyphAnimation second = engine.frame(text, 0L, secondId).glyphs().get(0);
        TextAnimationSampler sampler = new TextAnimationSampler();

        long now = 140_000L;
        assertFalse(sampler.sample(first, now).visible);
        assertTrue(sampler.sample(first, now + 21L).visible);
        assertFalse(sampler.sample(second, now + 21L).visible);
    }

    @Test
    void colorfulEffectsLeaveShadowPassUntinted() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationEngine.GlyphAnimation rainbow = glyph("<rainb>X</rainb>");
        assertTrue(sampler.sample(rainbow, 1234L, false).rgb >= 0);
        assertEquals(-1, sampler.sample(rainbow, 1234L, true).rgb);

        TextAnimationEngine.GlyphAnimation pulse = glyph("<pulse>X</pulse>");
        assertNotEquals(1.0F, sampler.sample(pulse, 1234L, false).colorMultiplier);
        assertEquals(1.0F, sampler.sample(pulse, 1234L, true).colorMultiplier);
    }

    @Test
    void pulseChangesForegroundBrightnessOverTimeButNeverShadow() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationEngine.GlyphAnimation pulse = glyph("<pulse base=0.6 a=0.4 f=1>X</pulse>");
        TextAnimationSampler.Sample low = sampler.sample(pulse, 0L, false, 0.0F, 0.0F,
                0xFFFFFFFF);
        TextAnimationSampler.Sample high = sampler.sample(pulse, 785L, false, 0.0F, 0.0F,
                0xFFFFFFFF);
        assertTrue((low.argb & 0xFFFFFF) < (high.argb & 0xFFFFFF),
                "pulse must update the foreground color as time advances");
        assertEquals(0xFFFFFFFF, sampler.sample(pulse, 785L, true, 0.0F, 0.0F,
                0xFFFFFFFF).argb);
    }

    @Test
    void shakeProducesSubpixelDisplacementThatSurvivesSampling() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationEngine.GlyphAnimation shake = glyph("<shake>X</shake>");
        TextAnimationSampler.Sample first = sampler.sample(shake, 0L);
        TextAnimationSampler.Sample second = sampler.sample(shake, 200L);
        assertTrue(Math.hypot(first.x, first.y) > 0.0,
                "shake must displace each glyph");
        assertTrue(Math.hypot(second.x - first.x, second.y - first.y) > 0.01,
                "shake direction must change over time");
    }

    @Test
    void shakeKeepsMovingAtEpochTimeAndBeyondTheIntSeedBoundary() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationEngine.GlyphAnimationFrame frame = frame(parse("<shake>WARNING!汉字</shake>"));
        for (long start : new long[] {1_788_900_000_000L, (Integer.MAX_VALUE + 1L) * 100L}) {
            java.util.Set<String> characterPositions = new java.util.HashSet<>();
            for (TextAnimationEngine.GlyphAnimation glyph : frame.glyphs()) {
                java.util.Set<String> positions = new java.util.HashSet<>();
                for (long time = start; time < start + 2_000L; time += 100L) {
                    TextAnimationSampler.Sample sample = sampler.sample(glyph, time);
                    positions.add(sample.x + "," + sample.y);
                    assertEquals(0.6, Math.hypot(sample.x, sample.y), 0.000001);
                    TextAnimationSampler.Sample shadow = sampler.sample(glyph, time, true);
                    assertEquals(sample.x, shadow.x);
                    assertEquals(sample.y, shadow.y);
                }
                assertTrue(positions.size() > 10, "each character must keep shaking at real timestamps");
                TextAnimationSampler.Sample first = sampler.sample(glyph, start);
                characterPositions.add(first.x + "," + first.y);
            }
            assertTrue(characterPositions.size() > 1, "characters must not share a saturated seed");
        }
    }

    @Test
    void shakeZeroSpeedAndAmplitudeKeepTheirOriginalMeaning() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationEngine.GlyphAnimation still = glyph("<shake f=0 a=2>X</shake>");
        long now = 1_788_900_000_000L;
        TextAnimationSampler.Sample first = sampler.sample(still, now);
        TextAnimationSampler.Sample later = sampler.sample(still, now + 1_000L);
        assertEquals(first.x, later.x);
        assertEquals(first.y, later.y);
        assertEquals(1.2, Math.hypot(first.x, first.y), 0.000001);
        TextAnimationSampler.Sample zero = sampler.sample(glyph("<shake a=0>X</shake>"), now);
        assertEquals(0.0, Math.hypot(zero.x, zero.y));
    }

    @Test
    void pulseRetainsOriginalAmplitudeAndOffersExplicitBrightnessEndpoints() {
        assertPulseRange("<pulse base=0.6 a=0.4 f=1.5>Power Rising</pulse>", 0.6F, 0.7F);
        assertPulseRange("<pulse min=0.6 max=1 f=1.5>Power Rising</pulse>", 0.6F, 1.0F);
        assertPulseRange("<pulse min=0.2 max=1 base=0 a=0 f=1.5>X</pulse>", 0.2F, 1.0F);
        assertPulseRange("<pulse base=0.6 a=1.6 f=1.5>Power Rising</pulse>", 0.6F, 1.0F);
    }

    private static void assertPulseRange(String source, float minimum, float maximum) {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationEngine.GlyphAnimation glyph = glyph(source);
        float low = Float.POSITIVE_INFINITY, high = Float.NEGATIVE_INFINITY;
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        long start = 1_788_900_000_000L;
        for (long time = start; time < start + 2_200L; time += 16L) {
            TextAnimationSampler.Sample sample = sampler.sample(glyph, time, false,
                    0.0F, 0.0F, 0x80336699);
            low = Math.min(low, sample.colorMultiplier);
            high = Math.max(high, sample.colorMultiplier);
            colors.add(sample.argb);
            assertEquals(0x80, sample.argb >>> 24, "pulse must preserve caller opacity");
            assertEquals(0x80336699, sampler.sample(glyph, time, true,
                    0.0F, 0.0F, 0x80336699).argb, "pulse must leave the shadow unchanged");
        }
        assertEquals(minimum, low, 0.001F);
        assertEquals(maximum, high, 0.001F);
        assertTrue(colors.size() > 5, "foreground colors must vary throughout the cycle");
    }

    @Test
    void shadowEffectOnlyChangesShadowPass() {
        TextAnimationEngine.GlyphAnimation glyph = glyph(
                "<shadow x=2 y=-1 c=FF0000 a=0.5>X</shadow>");
        TextAnimationSampler sampler = new TextAnimationSampler();
        assertFalse(sampler.sample(glyph, 0L, false).shadow);
        TextAnimationSampler.Sample shadow = sampler.sample(glyph, 0L, true);
        assertTrue(shadow.shadow);
        assertEquals(2.0, shadow.shadowX);
        assertEquals(-1.0, shadow.shadowY);
        assertEquals(2.0, shadow.x);
        assertEquals(-1.0, shadow.y);
        assertEquals(0xFF0000, shadow.rgb);
        assertEquals(0.5F, shadow.alpha);

        TextAnimationSampler.Sample components = sampler.sample(
                glyph("<shadow r=1 g=0.5 b=0>X</shadow>"), 0L, true);
        assertEquals(0xFF8000, components.rgb);
    }

    @Test
    void neonAndGlitchExposeTheirExtraDrawLayers() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationSampler.Sample neon = sampler.sample(
                glyph("<neon p=8 r=3 a=0.2>X</neon>"), 0L);
        assertTrue(neon.glow);
        assertEquals(8, neon.glowPasses);
        assertEquals(3.0F, neon.glowRadius);
        assertEquals(0.2F, neon.glowStrength);

        TextAnimationSampler.Sample glitch = sampler.sample(
                glyph("<glitch s=1 j=0 b=0>X</glitch>"), 250L);
        assertTrue(glitch.sibling);
        assertTrue(Math.abs(glitch.siblingX) >= 1.5);
        assertTrue(glitch.maskTop > 0.0F);
        assertTrue(glitch.siblingMaskBottom > 0.0F);
    }

    @Test
    void nestedGlitchAndNeonPreserveEveryOriginalDrawLayer() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationSampler.Sample sample = sampler.sample(glyph(
                "<glitch s=1 j=0 b=0><glitch s=1 j=0 b=0>"
                        + "<neon p=4><neon p=7>X</neon></neon>"
                        + "</glitch></glitch>"), 250L, false,
                0.0F, 0.0F, 0xFF336699);

        assertEquals(4, sample.layers().size());
        assertEquals(2, sample.glows().size());
        assertEquals(7, sample.glows().get(0).passes);
        assertEquals(4, sample.glows().get(1).passes);
    }

    @Test
    void glitchShadowKeepsBaseShadowAndColorsBothSlices() {
        TextAnimationSampler.Sample sample = new TextAnimationSampler().sample(
                glyph("<glitch s=1 j=0 b=0>X</glitch>"), 250L, true,
                2.0F, 3.0F, 0xFF808080);

        assertEquals(3, sample.layers().size());
        TextAnimationSampler.Sample preserved = sample.layers().get(1);
        TextAnimationSampler.Sample shifted = sample.layers().get(2);
        assertEquals(0.0, preserved.x);
        assertEquals(0.0, preserved.y);
        assertNotEquals(0x808080, sample.argb & 0xFFFFFF);
        assertNotEquals(0x808080, shifted.argb & 0xFFFFFF);
    }

    @Test
    void nestedColorEffectsApplyInOriginalOrderInsteadOfBeingFlattened() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationSampler.Sample rainbowLast = sampler.sample(
                glyph("<rainb><pulse base=0 a=1>X</pulse></rainb>"), 1234L,
                false, 0.0F, 0.0F, 0xFFFFFFFF);
        TextAnimationSampler.Sample pulseLast = sampler.sample(
                glyph("<pulse base=0 a=1><rainb>X</rainb></pulse>"), 1234L,
                false, 0.0F, 0.0F, 0xFFFFFFFF);

        assertNotEquals(rainbowLast.argb, pulseLast.argb);
        assertEquals(rainbowLast.rgb, rainbowLast.argb & 0xFFFFFF);
    }

    @Test
    void swingAndPendulumKeepIndependentRadianRotations() {
        TextAnimationSampler sampler = new TextAnimationSampler();
        TextAnimationSampler.Sample swing = sampler.sample(glyph("<swing>X</swing>"), 500L);
        assertNotEquals(0.0, swing.rotation);
        assertEquals(0.0, swing.pendulumRotation);
        TextAnimationSampler.Sample pend = sampler.sample(glyph("<pend>X</pend>"), 500L);
        assertEquals(0.0, pend.rotation);
        assertNotEquals(0.0, pend.pendulumRotation);
    }

    private static StructuredText parse(String source) {
        return StandardSyntaxEngines.minecraftWithAnimationCompatibility().parse(source);
    }

    private static TextAnimationEngine.GlyphAnimation glyph(String source) {
        return frame(parse(source)).glyphs().get(0);
    }

    private static TextAnimationEngine.GlyphAnimationFrame frame(StructuredText text) {
        return new TextAnimationEngine().frame(text, 0L);
    }
}
