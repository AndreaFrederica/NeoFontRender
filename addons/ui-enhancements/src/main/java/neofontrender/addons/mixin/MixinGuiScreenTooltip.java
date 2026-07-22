package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import neofontrender.addons.tooltips.ModernTooltipHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Replaces GuiScreen tooltip panels while retaining the current item context. */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenTooltip {
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "renderToolTip", at = @At("HEAD"))
    private void nfrUi$beginItemTooltip(ItemStack stack, int x, int y, CallbackInfo callback) {
        ModernTooltipHandler.beginItem(stack);
    }

    @Inject(method = "renderToolTip", at = @At("RETURN"))
    private void nfrUi$endItemTooltip(ItemStack stack, int x, int y, CallbackInfo callback) {
        ModernTooltipHandler.endItem();
    }

    @Inject(method = "drawHoveringText", at = @At("HEAD"), cancellable = true, remap = false)
    private void nfrUi$drawTooltip(
            List<String> lines, int x, int y, FontRenderer font, CallbackInfo callback) {
        if (ModernTooltipHandler.draw(lines, x, y, width, height, font)) callback.cancel();
    }
}
