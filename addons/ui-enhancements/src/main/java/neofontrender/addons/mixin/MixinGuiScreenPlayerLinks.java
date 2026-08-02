package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import neofontrender.addons.chat.ChatPlayerLinks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenPlayerLinks {
    @Inject(method = "handleComponentClick", at = @At("HEAD"), cancellable = true)
    private void nfrUi$handlePlayerLink(ITextComponent component,
                                        CallbackInfoReturnable<Boolean> cir) {
        String player = ChatPlayerLinks.playerFrom(component);
        if (player == null) return;
        ChatPlayerLinks.activate(player);
        cir.setReturnValue(true);
    }
}
