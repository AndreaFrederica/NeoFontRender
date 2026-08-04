package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosmicTextRendererFontFamilyTest {
    @Test
    void registersEveryFaceOfALocalFallbackFamily() {
        LinkedHashMap<String, String> selectors = new LinkedHashMap<>();

        CosmicTextRenderer.registerFamilySources(selectors, "Fallback Sans", List.of(
                "neofontrender/fonts/FallbackSans-Regular.ttf",
                "neofontrender/fonts/FallbackSans-Bold.ttf",
                "neofontrender/fonts/FallbackSans-Italic.ttf",
                "neofontrender/fonts/FallbackSans-BoldItalic.ttf"));

        assertEquals("neofontrender/fonts/FallbackSans-Regular.ttf",
                selectors.get("Fallback Sans"));
        assertEquals(List.of(
                        "neofontrender/fonts/FallbackSans-Regular.ttf",
                        "neofontrender/fonts/FallbackSans-Bold.ttf",
                        "neofontrender/fonts/FallbackSans-Italic.ttf",
                        "neofontrender/fonts/FallbackSans-BoldItalic.ttf"),
                new ArrayList<>(selectors.values()));
    }

    @Test
    void preservesTheSelectedPrimarySourceWhileAddingSiblingFaces() {
        LinkedHashMap<String, String> selectors = new LinkedHashMap<>();
        selectors.put("Primary Sans", "neofontrender/fonts/PrimarySans-Regular.ttf");

        CosmicTextRenderer.registerFamilySources(selectors, "Primary Sans", List.of(
                "neofontrender/fonts/PrimarySans-Bold.ttf",
                "neofontrender/fonts/PrimarySans-Regular.ttf"));

        assertEquals("neofontrender/fonts/PrimarySans-Regular.ttf",
                selectors.get("Primary Sans"));
        assertEquals(List.of(
                        "neofontrender/fonts/PrimarySans-Regular.ttf",
                        "neofontrender/fonts/PrimarySans-Bold.ttf"),
                new ArrayList<>(selectors.values()));
    }
}
