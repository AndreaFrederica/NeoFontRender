package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.config.GuiUtils;
import neofontrender.core.font.support.TooltipBoundsCompat;
import neofontrender.core.font.support.TooltipLayoutCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.item.ItemStack;
import java.util.List;

/** Corrects Forge tooltip wrapping and screen-edge placement for modern glyph pixel bounds. */
@Mixin(value = GuiUtils.class, remap = false)
public abstract class MixinForgeGuiUtils {

    @Inject(method = "drawHoveringText(Lnet/minecraft/item/ItemStack;Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), require = 0)
    private static void sfr$captureTooltipLayout(ItemStack stack, List<String> lines, int x, int y,
                                                  int screenWidth, int screenHeight, int maxTextWidth,
                                                  FontRenderer font, CallbackInfo ci) {
        int width = 0;
        if (lines != null) for (String line : lines) width = Math.max(width, TooltipBoundsCompat.measuredWidth(font, line));
        int height = lines == null || lines.isEmpty() ? 0 : 8 + Math.max(0, lines.size() - 1) * 10;
        int tooltipX = x + 12;
        int tooltipY = y - 12;
        if (tooltipX + width + 4 > screenWidth) {
            tooltipX = x - 16 - width;
            if (tooltipX < 4) tooltipX = x + 12;
        }
        if (tooltipY + height + 6 > screenHeight) tooltipY = screenHeight - height - 6;
        if (tooltipY < 4) tooltipY = 4;
        TooltipLayoutCompat.publish(font, lines, tooltipX, tooltipY, width, height);
    }

    @Inject(method = "drawHoveringText(Lnet/minecraft/item/ItemStack;Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("RETURN"), require = 0)
    private static void sfr$clearTooltipLayout(ItemStack stack, List<String> lines, int x, int y,
                                                int screenWidth, int screenHeight, int maxTextWidth,
                                                FontRenderer font, CallbackInfo ci) {
        TooltipLayoutCompat.clear();
    }

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
