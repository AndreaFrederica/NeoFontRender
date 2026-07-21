package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import neofontrender.addons.chat.ChatHistoryManager;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatHistory {
    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100), require = 2)
    private int nfrUi$historyLimit(int original) {
        return EnhancedChatConfigAccess.messageLimit();
    }

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"))
    private void nfrUi$recordReceived(ITextComponent component, int id, CallbackInfo ci) {
        ChatHistoryManager.INSTANCE.recordReceived(component, id);
    }

    @Inject(method = "addToSentMessages", at = @At("RETURN"))
    private void nfrUi$recordSent(String message, CallbackInfo ci) {
        ChatHistoryManager.INSTANCE.recordSent(message);
    }
}
