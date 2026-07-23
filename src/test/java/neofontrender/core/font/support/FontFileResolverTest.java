package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FontFileResolverTest {
    private final File gameDirectory = new File("build/test-game").getAbsoluteFile();

    @Test
    void storesGameFolderFontsAsPortableLocations() {
        File font = new File(gameDirectory, "neofontrender/fonts/My Font.ttf");

        assertEquals("neofontrender/fonts/My Font.ttf",
                FontFileResolver.portableLocation(gameDirectory, font));
    }

    @Test
    void resolvesPortableLocationsAgainstTheCurrentGameDirectory() {
        File expected = new File(gameDirectory, "neofontrender/fonts/My Font.ttf");

        assertEquals(expected.toPath().toAbsolutePath().normalize(),
                FontFileResolver.resolve(gameDirectory, "neofontrender/fonts/My Font.ttf")
                        .toPath().toAbsolutePath().normalize());
    }

    @Test
    void normalizesLegacyAbsoluteGameFontPaths() {
        File font = new File(gameDirectory, "neofontrender/fonts/Legacy.ttf").getAbsoluteFile();

        assertEquals("neofontrender/fonts/Legacy.ttf",
                FontFileResolver.normalizeLocation(gameDirectory, font.getPath()));
    }

    @Test
    void leavesExternalFilesAndSystemFamilyNamesUnchanged() {
        File external = new File(gameDirectory.getParentFile(), "shared/External.ttf").getAbsoluteFile();

        assertEquals(external.getPath(),
                FontFileResolver.normalizeLocation(gameDirectory, external.getPath()));
        assertEquals(new File("Chakra Petch"), FontFileResolver.resolve(gameDirectory, "Chakra Petch"));
    }

    @Test
    void normalizesPrimaryFallbackAndVariantStyleLists() {
        File primary = new File(gameDirectory, "neofontrender/fonts/Primary.ttf").getAbsoluteFile();
        File fallback = new File(gameDirectory, "neofontrender/fonts/Fallback.ttf").getAbsoluteFile();

        List<String> normalized = FontFileResolver.normalizeLocations(gameDirectory, Arrays.asList(
                primary.getPath(),
                fallback.getPath() + "; Chakra Petch",
                primary.getPath()));

        assertEquals(List.of(
                "neofontrender/fonts/Primary.ttf",
                "neofontrender/fonts/Fallback.ttf",
                "Chakra Petch"), normalized);
    }

    @Test
    void groupsSplitStyleFacesUnderTheirCanonicalFamily() {
        assertEquals("Chakra Petch",
                FontFileResolver.normalizeFamilyName("Chakra Petch Medium"));
        assertEquals("Chakra Petch",
                FontFileResolver.normalizeFamilyName("Chakra Petch Bold"));
        assertEquals("Chakra Petch",
                FontFileResolver.normalizeFamilyName("Chakra Petch Medium Italic"));
        assertEquals("Chakra Petch",
                FontFileResolver.normalizeFamilyName("Chakra Petch SemiBold Italic"));
    }
}
