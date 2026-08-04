package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosmicStyleFlagsTest {

    @Test
    void preservesAllMinecraftStyleBitsForNativeRendering() {
        assertEquals(0b1111,
                CosmicTextRenderer.composeStyleFlags(0, true, true, true, true));
    }

    @Test
    void combinesForcedFaceStyleWithRunDecorations() {
        assertEquals(0b1101,
                CosmicTextRenderer.composeStyleFlags(1, false, false, true, true));
    }
}
