package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackportCrosshairCompatTest {
    @Test
    void bundledListUsesExactIdsAndUserConfigOnlyAddsExactIds() {
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "crossbow:crossbow"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "crossbows:crossbow"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "tconstruct:crossbow"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "futuremc:crossbow"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.SPYGLASS, "hgwsspyglasses:spyglassred"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.TRIDENT, "futuremc:trident"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, "matteroverdrive:phaser"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, "matteroverdrive:phaser_rifle"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, "matteroverdrive:plasma_shotgun"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, "matteroverdrive:ion_sniper"));
        assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, "matteroverdrive:omni_tool"));
        assertFalse(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "example:super_crossbow"));

        try {
            CrosshairItemCompat.configure("example:brass_scope", "example:super_crossbow",
                    "example:trident", "example:blaster");
            assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.SPYGLASS, "example:brass_scope"));
            assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "example:super_crossbow"));
            assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.TRIDENT, "example:trident"));
            assertTrue(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED, "example:blaster"));
            assertFalse(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "example:other_crossbow"));

            CrosshairItemCompat.configure("example:*", "*:crossbow, example:cross*",
                    "example:tri.*", "matteroverdrive:*");
            assertFalse(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.SPYGLASS, "example:spyglass"));
            assertFalse(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.CROSSBOW, "example:crossbow"));
            assertFalse(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.TRIDENT, "example:trident"));
            assertFalse(CrosshairItemCompat.matches(CrosshairItemCompat.Kind.RANGED,
                    "example:blaster"));
        } finally {
            CrosshairItemCompat.configure("", "", "", "");
        }
    }
}
