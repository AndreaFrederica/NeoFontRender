package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.client.EnchantmentTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces only Reagenchant's two magic-name draw calls when that mod is present. */
@Pseudo
@Mixin(targets = "logictechcorp.reagenchant.client.gui.GuiReagentTable", remap = false)
public abstract class MixinReagenchantGuiReagentTable {
    @Redirect(method = "func_146976_a(FII)V", remap = false, require = 2,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/client/gui/FontRenderer;func_78279_b(Ljava/lang/String;IIII)V"))
    private void nfr$drawMagicName(FontRenderer vanilla, String text,
                                   int x, int y, int width, int color) {
        EnchantmentTextRenderer.drawMagicName(vanilla, text, x, y, width, color);
    }
}
