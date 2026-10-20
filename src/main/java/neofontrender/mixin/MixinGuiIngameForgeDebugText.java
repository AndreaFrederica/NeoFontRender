package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.GuiIngameForge;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.compat.CompatFontEngine;
import neofontrender.core.font.support.FontEngineScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntSupplier;

/** Forge draws the debug columns itself instead of calling GuiOverlayDebug.renderDebugInfo. */
@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiIngameForgeDebugText {
    // Scope each font call, not renderHUDText as a whole: Forge posts other mods' HUD events
    // in this method too. Their rendering must keep the global engine, even with F3 open.
    @Redirect(method = "renderHUDText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I",
            remap = false), require = 0)
    @Group(name = "nfr$debugWidth", min = 2, max = 2)
    private int nfr$debugWidthMcp(FontRenderer font, String text) {
        return nfr$withDebugEngine(font, text, () -> font.getStringWidth(text));
    }

    @Redirect(method = "renderHUDText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;func_78256_a(Ljava/lang/String;)I",
            remap = false), require = 0)
    @Group(name = "nfr$debugWidth", min = 2, max = 2)
    private int nfr$debugWidthSrg(FontRenderer font, String text) {
        return nfr$withDebugEngine(font, text, () -> font.getStringWidth(text));
    }

    @Redirect(method = "renderHUDText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
            remap = false), require = 0)
    @Group(name = "nfr$debugDraw", min = 2, max = 2)
    private int nfr$debugDrawMcp(FontRenderer font, String text, int x, int y, int color) {
        return nfr$withDebugEngine(font, text, () -> font.drawString(text, x, y, color));
    }

    @Redirect(method = "renderHUDText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;func_78276_b(Ljava/lang/String;III)I",
            remap = false), require = 0)
    @Group(name = "nfr$debugDraw", min = 2, max = 2)
    private int nfr$debugDrawSrg(FontRenderer font, String text, int x, int y, int color) {
        return nfr$withDebugEngine(font, text, () -> font.drawString(text, x, y, color));
    }

    @Unique
    private static int nfr$withDebugEngine(FontRenderer font, String text, IntSupplier action) {
        return FontEngineScope.callForOverlay(Minecraft.getMinecraft().gameSettings.showDebugInfo,
                FontEngineScope.overlayMode(), () -> {
                    if (FontEngineScope.current() == FontEngineScope.Mode.VANILLA) {
                        CompatFontEngine compat = FontManager.INSTANCE.getCompatEngine();
                        if (compat != null) compat.prepareVanillaMetrics(font, text);
                    }
                    return action.getAsInt();
                });
    }
}
