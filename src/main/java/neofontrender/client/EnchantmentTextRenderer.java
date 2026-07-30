package neofontrender.client;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.AdvancedTextApi;
import neofontrender.api.text.FontRenderBackend;
import neofontrender.api.text.FontRenderSpec;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.ScopedFontRenderBypass;

/** Scoped renderer shared by vanilla and compatible enchanting-table screens. */
public final class EnchantmentTextRenderer {
    private EnchantmentTextRenderer() {}

    public static void drawMagicName(FontRenderer vanilla, String text,
                                     int x, int y, int width, int color) {
        FontRenderSpec spec = FontRenderSpec.builder()
                .backend(backend(NeofontrenderConfig.enchantmentFontBackend()))
                .fonts(NeofontrenderConfig.enchantmentFonts())
                .size(8.0F)
                .build();
        if (AdvancedTextApi.drawWrapped(text, x, y, width, color, spec)) return;
        ScopedFontRenderBypass.run(() -> vanilla.drawSplitString(text, x, y, width, color));
    }

    static FontRenderBackend backend(String value) {
        if ("awt".equals(value)) return FontRenderBackend.AWT;
        if ("cosmic".equals(value)) return FontRenderBackend.COSMIC;
        if ("auto".equals(value)) return FontRenderBackend.AUTO;
        return FontRenderBackend.VANILLA;
    }
}
