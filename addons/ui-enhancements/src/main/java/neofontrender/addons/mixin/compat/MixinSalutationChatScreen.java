package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.GuiTextField;
import neofontrender.addons.chat.ChatKeyBindings;
import neofontrender.addons.chat.ChatKeepOpenPolicy;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Salutation's custom keyTyped path compatible with TabbyChat's removable PM prefix. */
@Pseudo
@Mixin(targets = "speiger.src.salutation.client.gui.chat.ChatScreen", remap = false)
public abstract class MixinSalutationChatScreen {
    @Inject(method = {"keyTyped(CI)V", "func_73869_a(CI)V"}, at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$removePrivateCommandBlock(char typedChar, int keyCode, CallbackInfo ci) {
        GuiTextField inputField = nfrUi$inputField();
        if (keyCode == Keyboard.KEY_BACK && inputField != null
                && inputField.getCursorPosition() == 0
                && inputField.getSelectionEnd() == 0
                && ChatKeyBindings.removePrivateCommandBlock(inputField)) {
            ci.cancel();
        }
    }

    @Inject(method = {"keyTyped(CI)V", "func_73869_a(CI)V"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;func_147108_a(Lnet/minecraft/client/gui/GuiScreen;)V",
                    ordinal = 1), cancellable = true, require = 1, remap = false)
    private void nfrUi$keepChatOpenAfterSend(char typedChar, int keyCode, CallbackInfo ci) {
        if (neofontrender.addons.chat.EnhancedChatConfigAccess.tabbedChatEnabled()
                && TabbyChat.getInstance().getChat() != null
                && ChatKeepOpenPolicy.shouldKeepOpen(
                        TabbyChat.getInstance().getChat().getActiveChannel())) {
            ci.cancel();
        }
    }

    private GuiTextField nfrUi$inputField() {
        return ((AccessorGuiChatFeatures) (Object) this).nfrUi$getInputField();
    }
}
