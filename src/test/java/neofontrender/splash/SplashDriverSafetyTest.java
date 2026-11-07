package neofontrender.splash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplashDriverSafetyTest {
    @Test
    void identifiesTheKnownIntelWindowsCombination() {
        assertTrue(SplashDriverSafety.unsafeIntelCleanroom(
                "Windows 11", "Intel", "Intel(R) Iris(R) Xe Graphics"));
    }

    @Test
    void keepsOtherDriversAndPlatforms() {
        assertFalse(SplashDriverSafety.unsafeIntelCleanroom("Windows 11", "NVIDIA", "RTX"));
        assertFalse(SplashDriverSafety.unsafeIntelCleanroom("Linux", "Intel", "Mesa Intel"));
        assertFalse(SplashDriverSafety.unsafeIntelCleanroom("Windows 11", null, null));
    }
}
