package neofontrender.api.client.font;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuiltinFontRegistryTest {
    @Test
    void dependentModsCanRegisterNamespacedSelectableFonts() {
        BuiltinFontRegistry.register("test_mod:formula_font", "Formula Font",
                "test_mod:fonts/formula_font.otf", false);

        BuiltinFontRegistry.Entry entry = BuiltinFontRegistry.entries().stream()
                .filter(font -> font.id().equals("test_mod:formula_font"))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("test_mod", entry.namespace());
        assertEquals("Formula Font", entry.familyName());
        assertEquals("test_mod:fonts/formula_font.otf", entry.location());
        assertFalse(entry.defaultFallback());
    }

    @Test
    void rejectsUnnamespacedCatalogIds() {
        assertThrows(IllegalArgumentException.class, () -> BuiltinFontRegistry.register(
                "formula_font", "Formula Font", "test_mod:fonts/formula_font.otf", false));
    }
}
