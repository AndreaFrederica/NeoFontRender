package neofontrender.core.config;

import neofontrender.api.color.TextColorPaletteRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NeofontrenderConfigTest {
    @Test
    void paletteProviderFallsBackToAutoBeforeConfigIsLoaded() {
        assertFalse(NeofontrenderConfig.isLoaded());
        assertEquals(TextColorPaletteRegistry.AUTO,
                NeofontrenderConfig.textColorPaletteProvider());
    }
}
