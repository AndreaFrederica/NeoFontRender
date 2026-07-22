package neofontrender.addons.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudBarThemeTest {
    @Test
    void parsesEveryPublishedThemeIgnoringCaseAndWhitespace() {
        assertEquals(HudBarTheme.CLASSIC, HudBarTheme.parse(" CLASSIC "));
        assertEquals(HudBarTheme.MODERN, HudBarTheme.parse("modern"));
        assertEquals(HudBarTheme.FLAT, HudBarTheme.parse("FLAT"));
        assertEquals(HudBarTheme.GLASS, HudBarTheme.parse("glass"));
        assertEquals(HudBarTheme.SEGMENTED, HudBarTheme.parse("segmented"));
        assertEquals(HudBarTheme.MINIMAL, HudBarTheme.parse("minimal"));
    }

    @Test
    void fallsBackToModernForMissingOrUnknownTheme() {
        assertEquals(HudBarTheme.MODERN, HudBarTheme.parse(null));
        assertEquals(HudBarTheme.MODERN, HudBarTheme.parse("future-theme"));
    }
}
