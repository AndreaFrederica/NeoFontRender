package neofontrender.core.font.cosmic;

import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.ShadowRenderSpec;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CosmicRasterPreparationTest {
    private static byte[] raster(int flags) {
        return ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0x434F534D).putInt(1).putInt(1).putInt(-2).putInt(4)
                .putFloat(6.5F).putFloat(7).putFloat(2).putInt(flags).putInt(0xFFFFFFFF).array();
    }

    private static CosmicRasterPreparation.Options options(boolean sdf, boolean shadow) {
        return new CosmicRasterPreparation.Options(1, 7, sdf, 4, shadow, 0xFF000000,
                ShadowRenderSpec.of(1, 1, 0, 0xFF000000, "fixed", 0, "linear", null, 1));
    }

    @Test
    void preservesLogicalAdvanceAndBearingAcrossShadowAndSdfPreparation() {
        var plain = CosmicRasterPreparation.prepare(raster(CosmicNative.RASTER_MODEL_MASK), options(false, false));
        var shadow = CosmicRasterPreparation.prepare(raster(CosmicNative.RASTER_MODEL_MASK), options(false, true));
        var sdf = CosmicRasterPreparation.prepare(raster(CosmicNative.RASTER_MODEL_MASK), options(true, true));
        assertEquals(6.5F, plain.advance());
        assertEquals(plain.advance(), shadow.advance());
        assertEquals(plain.advance(), sdf.advance());
        assertEquals(-1, plain.foreground().x());
        assertEquals(2, plain.foreground().y());
        assertEquals(3, shadow.foreground().width());
        assertEquals(3, shadow.foreground().height());
        assertEquals(plain.foreground().x(), shadow.foreground().x());
        assertEquals(plain.foreground().y(), sdf.foreground().y());
        assertNull(shadow.shadow(), "RGBA shadow and foreground share one texture");
        assertNotNull(sdf.shadow());
        assertNotNull(sdf.foreground().sdf());
        assertNull(sdf.foreground().rgba());
        assertEquals(37, sdf.bytes());
    }

    @Test
    void coloredGradientsKeepRgbaAndMalformedPayloadsFailBeforeAllocation() {
        var gradient = CosmicRasterPreparation.prepare(raster(CosmicNative.RASTER_MODEL_GRADIENT_COLOR), options(true, false));
        assertNull(gradient.foreground().sdf());
        assertNotNull(gradient.foreground().rgba());
        byte[] corrupt = raster(1);
        ByteBuffer.wrap(corrupt).order(ByteOrder.LITTLE_ENDIAN).putInt(4, Integer.MAX_VALUE);
        assertThrows(IllegalStateException.class, () -> CosmicRasterPreparation.prepare(corrupt, options(false, false)));
        assertThrows(IllegalStateException.class, () -> CosmicRasterPreparation.prepare(new byte[0], options(false, false)));
    }

    @Test
    void retainedPendingLayoutResolvesNewResultsOnDrawAndKeepsItsMeasuredAdvance() {
        AtomicInteger draws = new AtomicInteger();
        AtomicReference<TextRenderResult> resolved = new AtomicReference<>(new TextRenderResult() {
            public float advance() { return 6.5F; }
            public void draw(float x, float y, float alpha) {
                assertEquals(10, x); assertEquals(20, y); assertEquals(0.5F, alpha);
                draws.incrementAndGet();
            }
        });
        var pending = new CosmicTextRenderer.PendingTextResult("123", 0xFFFFFFFF,
                false, false, false, false, 9, 6.5F, false, options(false, false), resolved::get);
        pending.draw(10, 20, 0.5F);
        assertEquals(1, draws.get());
        assertEquals(6.5F, pending.advance());
        resolved.set(TextRenderResult.EMPTY); // renderer reload/close invalidates the old generation
        pending.draw(10, 20, 0.5F);
        assertEquals(1, draws.get());
    }
}
