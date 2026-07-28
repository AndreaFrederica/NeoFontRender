package neofontrender.api.text;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FontRenderSpecTest {
    @Test
    void normalizesScopedSelection() {
        FontRenderSpec spec = FontRenderSpec.builder()
                .backend(FontRenderBackend.AWT)
                .fonts(Arrays.asList(" first.ttf ", "", "first.ttf", "second.ttf"))
                .size(Float.NaN)
                .build();

        assertEquals(FontRenderBackend.AWT, spec.backend());
        assertEquals(Arrays.asList("first.ttf", "second.ttf"), spec.fonts());
        assertEquals(8.0F, spec.size());
    }
}
