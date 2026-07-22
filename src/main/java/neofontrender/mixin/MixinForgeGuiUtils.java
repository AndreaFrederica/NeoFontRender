package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.config.GuiUtils;
import neofontrender.core.font.support.TooltipBoundsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Corrects Forge tooltip wrapping and screen-edge placement for modern glyph pixel bounds. */
@Mixin(value = GuiUtils.class, remap = false)
public abstract class MixinForgeGuiUtils {

    @Redirect(
            method = "drawHoveringText(Lnet/minecraft/item/ItemStack;Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I",
                    remap = false
            ),
            require = 0
    )
    @Group(name = "sfr$tooltipWidth", min = 2, max = 2)
    private static int sfr$measureTooltipVisualWidthMcp(FontRenderer font, String text) {
        return TooltipBoundsCompat.measuredWidth(font, text);
    }

    @Redirect(
            method = "drawHoveringText(Lnet/minecraft/item/ItemStack;Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;func_78256_a(Ljava/lang/String;)I",
                    remap = false
            ),
            require = 0
    )
    @Group(name = "sfr$tooltipWidth", min = 2, max = 2)
    private static int sfr$measureTooltipVisualWidthSrg(FontRenderer font, String text) {
        return TooltipBoundsCompat.measuredWidth(font, text);
    }
}
