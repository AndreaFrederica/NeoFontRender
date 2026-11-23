package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import neofontrender.addons.chat.ChatKeyBindings;
import neofontrender.addons.chat.ChatCommandCompletionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds UIE's Salutation-compatible completion engine from vanilla GuiChat. */
@Mixin(GuiChat.class)
public abstract class MixinGuiChatCommandCompletion {
    @Shadow
    protected GuiTextField inputField;

    /**
     * Forge's keyboard event is observed before GuiChat.keyTyped, but cancelling that
     * event does not stop every GuiChat subclass (the current instance is OptiFine's
     * GuiChatOF) from continuing into the inherited vanilla ChatTabCompleter. Once UIE handled the key, terminate
     * the screen-level path as well so the native completer cannot rewrite the input.
     */
    @Inject(method = "keyTyped(CI)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$stopNativeCompletion(char typedChar, int keyCode, CallbackInfo ci) {
        if (ChatCommandCompletionController.shouldBlockNativeTab(inputField, keyCode)
                || ChatKeyBindings.handledCurrentEvent()) {
            ci.cancel();
        }
    }

    @Inject(method = "setCompletions([Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void nfrUi$receiveCompletions(String[] completions, CallbackInfo ci) {
        ChatCommandCompletionController.setCompletions(inputField, completions);
    }

    @Inject(method = "keyTyped(CI)V", at = @At("RETURN"), require = 1)
    private void nfrUi$requestCompletions(char typedChar, int keyCode, CallbackInfo ci) {
        ChatCommandCompletionController.afterKeyTyped(inputField, keyCode);
    }

    @Inject(method = "onGuiClosed()V", at = @At("RETURN"), require = 1)
    private void nfrUi$clearCompletionState(CallbackInfo ci) {
        ChatCommandCompletionController.onChatClosed(inputField);
    }
}
