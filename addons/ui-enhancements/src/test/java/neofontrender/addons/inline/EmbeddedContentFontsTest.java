package neofontrender.addons.inline;

import neofontrender.api.client.font.BuiltinFontRegistry;
import neofontrender.client.gui.font.FontEntry;
import neofontrender.client.gui.util.FontCatalog;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedContentFontsTest {
    @Test
    void firaMathUsesTheSharedNamespacedBuiltinFontRegistry() throws Exception {
        EmbeddedContentFonts.register();
        BuiltinFontRegistry.Entry entry = BuiltinFontRegistry.entries().stream()
                .filter(font -> font.id().equals(EmbeddedContentFonts.FIRA_MATH_ID))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals("neofontrender_ui_enhancements", entry.namespace());
        assertEquals("Fira Math", entry.familyName());
        assertFalse(entry.defaultFallback());
        String classpath = "assets/" + entry.location().replace(':', '/');
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(classpath)) {
            assertNotNull(input);
            assertEquals("Fira Math", java.awt.Font.createFont(
                    java.awt.Font.TRUETYPE_FONT, input).getFamily(java.util.Locale.ROOT));
        }
    }

    @Test
    void firaMathIsExposedByTheNfrFontSelectorWithItsResourceLocation() {
        EmbeddedContentFonts.register();

        FontEntry font = FontCatalog.builtinFonts().stream()
                .filter(entry -> EmbeddedContentFonts.FIRA_MATH_LOCATION.equals(entry.path))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals(EmbeddedContentFonts.FIRA_MATH_FAMILY, font.familyName);
        assertEquals(EmbeddedContentFonts.FIRA_MATH_LOCATION, font.path);
        assertTrue(font.displayName.contains(EmbeddedContentFonts.FIRA_MATH_ID));
    }
}
