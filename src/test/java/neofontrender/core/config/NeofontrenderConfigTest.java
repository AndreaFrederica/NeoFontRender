package neofontrender.core.config;

import neofontrender.api.color.TextColorPaletteRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeofontrenderConfigTest {
    @Test
    void defaultsAndAutoPaletteSelectionAreAvailableBeforeConfigIsLoaded() {
        assertFalse(NeofontrenderConfig.isLoaded());
        assertEquals(TextColorPaletteRegistry.AUTO, TextColorPaletteRegistry.normalizeSelection(""));
        assertEquals("cosmic", NeofontrenderConfig.renderingEngine());
        assertTrue(NeofontrenderConfig.enabled());
    }
}
