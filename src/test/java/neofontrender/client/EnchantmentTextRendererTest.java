package neofontrender.client;

import neofontrender.api.text.FontRenderBackend;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentTextRendererTest {
    @Test
    void mapsConfiguredBackendsWithoutBroadeningVanillaFallback() {
        assertEquals(FontRenderBackend.AWT, EnchantmentTextRenderer.backend("awt"));
        assertEquals(FontRenderBackend.COSMIC, EnchantmentTextRenderer.backend("cosmic"));
        assertEquals(FontRenderBackend.AUTO, EnchantmentTextRenderer.backend("auto"));
        assertEquals(FontRenderBackend.VANILLA, EnchantmentTextRenderer.backend("vanilla"));
        assertEquals(FontRenderBackend.VANILLA, EnchantmentTextRenderer.backend("unknown"));
    }
}
