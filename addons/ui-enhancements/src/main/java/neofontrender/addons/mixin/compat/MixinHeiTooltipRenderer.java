package neofontrender.addons.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import neofontrender.addons.tooltips.HeiTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.gui.TooltipRenderer", remap = false)
public abstract class MixinHeiTooltipRenderer {
    private static final String CUSTOM_METHOD = "drawHoveringTextAndItems(Lnet/minecraft/item/ItemStack;"
            + "Lnet/minecraft/client/Minecraft;Ljava/util/List;Ljava/util/List;IIILnet/minecraft/client/gui/FontRenderer;)V";

    @Inject(method = CUSTOM_METHOD, at = @At("HEAD"), require = 0, remap = false)
    private static void nfrUi$beginCustomTooltip(ItemStack stack, Minecraft minecraft,
                                                  List<String> lines, List<?> itemLines,
                                                  int mouseX, int mouseY, int maxTextWidth,
                                                  FontRenderer font, CallbackInfo ci) {
        HeiTooltipCompat.begin(stack);
    }

    @Inject(method = CUSTOM_METHOD, at = @At("RETURN"), require = 0, remap = false)
    private static void nfrUi$endCustomTooltip(ItemStack stack, Minecraft minecraft,
                                                List<String> lines, List<?> itemLines,
                                                int mouseX, int mouseY, int maxTextWidth,
                                                FontRenderer font, CallbackInfo ci) {
        HeiTooltipCompat.end();
    }

    @Redirect(method = CUSTOM_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/config/GuiUtils;drawGradientRect(IIIIIII)V"),
            require = 0, remap = false)
    private static void nfrUi$replaceCustomPanel(int zLevel, int left, int top, int right, int bottom,
                                                  int startColor, int endColor) {
        HeiTooltipCompat.drawGradientRect(zLevel, left, top, right, bottom, startColor, endColor);
    }
}
