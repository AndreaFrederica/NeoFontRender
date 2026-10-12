package neofontrender.addons.mixin.tabbychat;

import net.minecraft.client.gui.GuiChat.ChatTabCompleter;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.TabCompleter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import neofontrender.addons.chat.EnhancedChatConfigAccess;

@Mixin(ChatTabCompleter.class)
public abstract class MixinChatTabCompleter extends TabCompleter {

    public MixinChatTabCompleter(GuiTextField textFieldIn) {
        super(textFieldIn, false);
    }

    @Inject(
            method = "complete()V",
            at = @At("HEAD"),
            cancellable = true)
    private void onComplete(CallbackInfo ci) {
        // UIE owns Tab handling and draws a scrollable popup. Continuing would
        // dump the completion list into chat history through vanilla GuiChat.
        if (EnhancedChatConfigAccess.tabbedChatEnabled()) ci.cancel();
    }

}
