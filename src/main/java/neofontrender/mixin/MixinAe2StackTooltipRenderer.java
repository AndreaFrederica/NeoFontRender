package neofontrender.mixin;

import neofontrender.core.font.support.TooltipLayoutCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

/** Feeds ae2s its final nfr tooltip geometry without linking against ae2s at compile time. */
@Mixin(targets = "ae2.client.gui.StackTooltipRenderer", remap = false)
public abstract class MixinAe2StackTooltipRenderer {
    @ModifyArgs(method = "drawTooltipImage(Lnet/minecraftforge/client/event/RenderTooltipEvent$PostText;)V",
            at = @At(value = "INVOKE", target = "Lae2/client/gui/StackTooltipRenderer;drawTooltipImage(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/item/ItemStack;IIIILjava/util/List;)V"),
            require = 0)
    private void nfr$useFinalLayout(Args args) {
        TooltipLayoutCompat.Layout layout = TooltipLayoutCompat.current();
        if (layout == null) return;
        args.set(3, layout.x);
        args.set(4, layout.y);
        args.set(5, layout.height);
        args.set(6, layout.lines);
    }
}
