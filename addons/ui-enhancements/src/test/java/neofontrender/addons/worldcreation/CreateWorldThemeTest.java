package neofontrender.addons.worldcreation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CreateWorldThemeTest {
    @Test
    void parsesThemesAndFallsBackToTabbed() {
        assertEquals(CreateWorldTheme.TABBED, CreateWorldTheme.parse(null));
        assertEquals(CreateWorldTheme.TABBED, CreateWorldTheme.parse("unknown"));
        assertEquals(CreateWorldTheme.TABBED, CreateWorldTheme.parse(" TABBED "));
        assertEquals(CreateWorldTheme.MODERN_UI, CreateWorldTheme.parse("ModernUI"));
    }

    @Test
    void exposesThemeCapabilities() {
        assertFalse(CreateWorldTheme.VANILLA.usesTabbedLayout());
        assertFalse(CreateWorldTheme.VANILLA.usesModernStyle());
        assertTrue(CreateWorldTheme.TABBED.usesTabbedLayout());
        assertFalse(CreateWorldTheme.TABBED.usesModernStyle());
        assertTrue(CreateWorldTheme.MODERN_UI.usesTabbedLayout());
        assertTrue(CreateWorldTheme.MODERN_UI.usesModernStyle());
    }
}
