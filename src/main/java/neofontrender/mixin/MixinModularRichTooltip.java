package neofontrender.mixin;

import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import net.minecraft.item.ItemStack;
import neofontrender.core.font.support.TooltipBoundsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks only ModularUI RichTooltip compilation for visual-bound-aware line measurement. */
@Mixin(value = RichTooltip.class, remap = false)
public abstract class MixinModularRichTooltip {
    @Inject(method = "draw(Lcom/cleanroommc/modularui/screen/viewport/GuiContext;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"))
    private void sfr$beginVisualTooltipLayout(GuiContext context, ItemStack stack, CallbackInfo ci) {
        TooltipBoundsCompat.beginRichTooltip();
    }

    @Inject(method = "draw(Lcom/cleanroommc/modularui/screen/viewport/GuiContext;Lnet/minecraft/item/ItemStack;)V",
            at = @At("RETURN"))
    private void sfr$endVisualTooltipLayout(GuiContext context, ItemStack stack, CallbackInfo ci) {
        TooltipBoundsCompat.endRichTooltip();
    }
}
