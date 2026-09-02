package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;
import net.minecraft.util.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SvgTokenParserTest {
    @Test
    void parsesDefaultAndExplicitHeights() {
        assertMatch("<svg:resource:test:formula.svg>", 24, 31);
        assertMatch("<svg:resource:test:formula.svg>[height=48]", 48, 42);
        assertMatch("<svg:resource:test:formula.svg>[ HEIGHT = 31.6 ]", 32, 48);
    }

    @Test
    void acceptsAngleBracketStrippedResourceTokens() {
        SvgTokenParser.Match match = SvgTokenParser.match(
                "svg:resource:test:formula.svg>[height=48]", 0);
        assertNotNull(match);
        assertEquals("resource:test:formula.svg", match.reference);
        assertEquals(48, match.height);
        assertEquals(41, match.end);
    }

    @Test
    void clampsHeightToTheLayoutBudget() {
        assertMatch("<svg:resource:test:formula.svg>[height=.1]", 4, 42);
        assertMatch("<svg:resource:test:formula.svg>[height=9999]", 256, 44);
    }

    @Test
    void leavesMalformedOptionsAsOrdinaryText() {
        assertMatch("<svg:resource:test:formula.svg>[height=large]", 24, 31);
        assertMatch("<svg:resource:test:formula.svg>[width=48]", 24, 31);
    }

    @Test
    void parsesStableResourceReferencesAcrossRuntimeResourceLocationImplementations() {
        ResourceLocation location = SvgContentMiddleware.parseResourceLocation(
                " neofontrender_inline_content_showcase:structures\\aminobenzo_18_crown_6.svg ");
        assertNotNull(location);
        assertEquals("neofontrender_inline_content_showcase", location.getNamespace());
        assertEquals("structures/aminobenzo_18_crown_6.svg", location.getPath());
    }

    @Test
    void rejectsUnsafeOrAmbiguousResourceReferences() {
        org.junit.jupiter.api.Assertions.assertNull(
                SvgContentMiddleware.parseResourceLocation("test:../formula.svg"));
        org.junit.jupiter.api.Assertions.assertNull(
                SvgContentMiddleware.parseResourceLocation("test:formula.png"));
        org.junit.jupiter.api.Assertions.assertNull(
                SvgContentMiddleware.parseResourceLocation("test:one:formula.svg"));
    }

    private static void assertMatch(String source, int expectedHeight, int expectedEnd) {
        SvgTokenParser.Match match = SvgTokenParser.match(source, 0);
        assertNotNull(match);
        assertEquals("resource:test:formula.svg", match.reference);
        assertEquals(expectedHeight, match.height);
        assertEquals(expectedEnd, match.end);
    }
}
