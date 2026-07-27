package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.linebreak.CjkComponentLineWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiUtilRenderComponents.class)
public abstract class MixinGuiUtilRenderComponents {
    @Inject(method = "splitText", at = @At("HEAD"), cancellable = true)
    private static void sfr$wrapCjkComponents(ITextComponent text, int maxWidth,
                                              FontRenderer font, boolean removeLeadingSpace,
                                              boolean forceTextColor,
                                              CallbackInfoReturnable<List<ITextComponent>> cir) {
        if (NeofontrenderConfig.fixCjkLineBreak()) {
            cir.setReturnValue(CjkComponentLineWrapper.wrap(
                    text, maxWidth, font, removeLeadingSpace, forceTextColor));
        }
    }
}
