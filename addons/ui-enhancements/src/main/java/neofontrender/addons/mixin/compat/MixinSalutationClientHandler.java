package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.GuiOpenEvent;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets UI Enhancements own the chat screen when its embedded TabbyChat is enabled.
 *
 * Salutation 1.12.2 replaces every GuiChat from ClientHandler.onGuiOpen. That replacement
 * has its own keyTyped implementation, so GuiChat mixins (including TabbyChat's
 * channel input and history handling) cannot run. Cancelling only Salutation's
 * handler preserves the original GuiOpenEvent and leaves Salutation available when
 * the embedded chat is disabled.
 */
@Pseudo
@Mixin(targets = "speiger.src.salutation.client.ClientHandler", remap = false)
public abstract class MixinSalutationClientHandler {
    @Inject(method = "onGuiOpen", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$keepUiEnhancementsChat(GuiOpenEvent event, CallbackInfo ci) {
        if (EnhancedChatConfigAccess.tabbedChatEnabled()) {
            ci.cancel();
        }
    }
}
