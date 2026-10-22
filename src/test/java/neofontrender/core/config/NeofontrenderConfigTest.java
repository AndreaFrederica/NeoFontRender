package neofontrender.core.config;

import neofontrender.api.color.TextColorPaletteRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeofontrenderConfigTest {
    @Test
    void menuBrandingOverridesRetainNovaByDefault() {
        assertTrue(NeofontrenderConfig.compatCustomMainMenu());
        assertTrue(NeofontrenderConfig.compatFancyMenu());
    }

    @Test
    void paletteProviderFallsBackToAutoBeforeConfigIsLoaded() {
        assertFalse(NeofontrenderConfig.isLoaded());
        assertEquals(TextColorPaletteRegistry.AUTO,
                NeofontrenderConfig.textColorPaletteProvider());
    }
}
