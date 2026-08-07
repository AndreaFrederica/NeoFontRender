package neofontrender.api.color;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextColorPaletteRegistryTest {
    @Test
    void runtimeProviderCopiesAllForegroundAndShadowEntries() {
        int[] runtime = TextColorPaletteRegistry.vanillaColorCodes();
        runtime[1] = 0x1E90FF;
        runtime[17] = 0x07243F;

        int[] resolved = TextColorPaletteRegistry.resolve("runtime", runtime);

        assertEquals(0x1E90FF, resolved[1]);
        assertEquals(0x07243F, resolved[17]);
        assertNotSame(runtime, resolved);
    }

    @Test
    void vanillaSelectionIgnoresRuntimeMixinChanges() {
        int[] runtime = TextColorPaletteRegistry.vanillaColorCodes();
        runtime[1] = 0x1E90FF;

        int[] resolved = TextColorPaletteRegistry.resolve("vanilla", runtime);

        assertEquals(0x0000AA, resolved[1]);
    }

    @Test
    void derivesShadowEntriesForSixteenColorProviders() {
        String id = "test_sixteen_colors";
        if (!TextColorPaletteRegistry.providerIds().contains(id)) {
            TextColorPaletteRegistry.register(new TextColorPaletteProvider() {
                @Override public String id() { return id; }
                @Override public String displayName() { return "Test palette"; }
                @Override public int[] colorCodes(int[] runtimeColorCodes) {
                    int[] colors = new int[16];
                    colors[4] = 0xD32F2F;
                    return colors;
                }
            });
        }

        int[] resolved = TextColorPaletteRegistry.resolve(id, null);

        assertEquals(0xD32F2F, resolved[4]);
        assertEquals(0x340B0B, resolved[20]);
        assertTrue(TextColorPaletteRegistry.providerIds().containsAll(
                List.of("auto", "vanilla", "runtime", id)));
    }

    @Test
    void customProviderUsesEditablePalette() {
        int[] custom = new int[16];
        custom[2] = 0x00C853;
        TextColorPaletteRegistry.setCustomColorCodes(custom);

        int[] resolved = TextColorPaletteRegistry.resolve("custom", null);

        assertEquals(0x00C853, resolved[2]);
        assertEquals(0x003214, resolved[18]);
    }

    @Test
    void codecAcceptsNovaStyleForegroundPaletteAndExplicitShadows() {
        String novaForeground = "000000,1e90ff,00c853,4db6ac,d32f2f,e040fb,ffa726,bdbdbd,"
                + "546e7a,03a9f4,69f0ae,18ffff,ff5e62,ff80ab,ffeb3b,ffffff";
        int[] derived = TextColorPaletteCodec.parse(novaForeground);
        assertEquals(0x1E90FF, derived[1]);
        assertEquals(0x07243F, derived[17]);

        String explicit = TextColorPaletteCodec.format(derived).replaceFirst("07243F", "123456");
        assertEquals(0x123456, TextColorPaletteCodec.parse(explicit)[17]);
    }
}
