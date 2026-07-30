package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiEnchantment;
import neofontrender.client.EnchantmentTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces only the three Standard Galactic Alphabet magic-name lines. */
@Mixin(GuiEnchantment.class)
public abstract class MixinGuiEnchantment {
    @Redirect(method = "drawGuiContainerBackgroundLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawSplitString(Ljava/lang/String;IIII)V"))
    private void nfr$drawMagicName(FontRenderer vanilla, String text, int x, int y, int width, int color) {
        EnchantmentTextRenderer.drawMagicName(vanilla, text, x, y, width, color);
    }
}
