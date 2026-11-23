package neofontrender.core.font.cosmic;

import neofontrender.text.StructuredText;
import neofontrender.text.animation.TextAnimationEngine;
import neofontrender.text.animation.TextAnimationSampler;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmicPulseRasterTest {
    @Test
    void pulseBrightnessReachesNativePixelsWithoutChangingCoverageOrAdvance() throws Exception {
        CosmicRuntimeSupport.Compatibility compatibility = CosmicRuntimeSupport.ensureLoaded();
        assertTrue(compatibility.isSupported(), compatibility.getMessage());
        byte[] font;
        try (InputStream stream = getClass().getResourceAsStream(
                "/assets/neofontrender/fonts/noto_sans_sc-regular.otf")) {
            assertNotNull(stream);
            font = stream.readAllBytes();
        }
        long engine = CosmicNative.createEngine(new byte[][] {font}, new String[] {"pulse-test"},
                "", new String[0], "", "", "", "", false, 0, 9.0F, "en-US");
        try {
            assertPulsePixels(engine, "<pulse base=0.6 a=0.4 f=1.5>Power Rising</pulse>", 153, 179);
            assertPulsePixels(engine, "<pulse min=0.6 max=1 f=1.5>Power Rising</pulse>", 153, 255);
        } finally {
            CosmicNative.destroyEngine(engine);
        }
    }

    private static void assertPulsePixels(long engine, String source, int expectedMin, int expectedMax) {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility().parse(source);
        long start = 1_788_900_000_000L;
        TextAnimationEngine.GlyphAnimation glyph = new TextAnimationEngine().frame(text, start)
                .glyphs().get(0);
        TextAnimationSampler sampler = new TextAnimationSampler();
        Set<Integer> brightnesses = new HashSet<>();
        int[] coverage = null;
        float advance = -1.0F;
        for (long now = start; now < start + 2_200L; now += 100L) {
            TextAnimationSampler.Sample sample = sampler.sample(glyph, now);
            byte[] raster = CosmicNative.renderSized(engine, "P", sample.argb, 0, 9.0F, 2.0F);
            ByteBuffer data = ByteBuffer.wrap(raster).order(ByteOrder.LITTLE_ENDIAN);
            int width = data.getInt(4), height = data.getInt(8);
            assertTrue(width > 0 && height > 0);
            assertEquals(CosmicNative.RASTER_MODEL_MASK, data.getInt(32));
            boolean first = coverage == null;
            if (first) {
                coverage = new int[width * height];
                advance = data.getFloat(20);
            }
            assertEquals(coverage.length, width * height);
            assertEquals(advance, data.getFloat(20));
            data.position(36);
            int visible = 0;
            for (int index = 0; index < coverage.length; index++) {
                int pixel = data.getInt();
                int alpha = pixel >>> 24;
                if (first) coverage[index] = alpha;
                else assertEquals(coverage[index], alpha, "pulse must not fade glyph coverage");
                if (alpha == 0) continue;
                visible++;
                assertEquals(sample.argb & 0xFFFFFF, pixel & 0xFFFFFF,
                        "native glyph cache must not retain a previous frame's brightness");
                brightnesses.add(pixel & 255);
            }
            assertTrue(visible > 0);
        }
        assertTrue(brightnesses.size() > 5);
        assertEquals(expectedMin, brightnesses.stream().mapToInt(Integer::intValue).min().getAsInt(), 1);
        assertEquals(expectedMax, brightnesses.stream().mapToInt(Integer::intValue).max().getAsInt(), 1);
    }
}
