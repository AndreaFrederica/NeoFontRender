package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
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

    @Inject(method = "setCompletions([Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void nfrUi$receiveCompletions(String[] completions, CallbackInfo ci) {
        ChatCommandCompletionController.setCompletions(inputField, completions);
    }

    @Inject(method = "keyTyped(CI)V", at = @At("RETURN"), require = 1)
    private void nfrUi$requestCompletions(char typedChar, int keyCode, CallbackInfo ci) {
        ChatCommandCompletionController.afterKeyTyped(inputField, keyCode);
    }
}
