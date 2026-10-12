package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosmicPremultipliedOpacityTest {
    @Test
    void clampsTheCommonRgbAndAlphaMultiplier() {
        assertEquals(0.25F, CosmicTextRenderer.premultipliedOpacity(0.25F));
        assertEquals(0.0F, CosmicTextRenderer.premultipliedOpacity(-1.0F));
        assertEquals(1.0F, CosmicTextRenderer.premultipliedOpacity(2.0F));
        assertEquals(0.0F, CosmicTextRenderer.premultipliedOpacity(Float.NaN));
    }
}
