package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FramebufferAlphaBlendTest {
    @Test
    void transparentTexturePaddingPreservesDestinationAlpha() {
        assertEquals(0.75F, FramebufferAlphaBlend.composite(0.0F, 0.75F));
    }

    @Test
    void glyphCoverageCompositesOverDestinationAlpha() {
        assertEquals(0.625F, FramebufferAlphaBlend.composite(0.5F, 0.25F));
        assertEquals(1.0F, FramebufferAlphaBlend.composite(1.0F, 0.25F));
    }
}
