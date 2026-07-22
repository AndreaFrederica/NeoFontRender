package neofontrender.addons.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.chat.ChatHistoryManager;
import neofontrender.addons.chat.ChatHistoryRuntimeAccess;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Extends and records GuiNewChat history while preserving vanilla replacement ids. */
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatHistory implements ChatHistoryRuntimeAccess {
    @Shadow private List<ChatLine> chatLines;
    @Shadow private List<ChatLine> field_146253_i;
    @Shadow private List<String> sentMessages;

    @ModifyConstant(method = "func_146237_a", constant = @Constant(intValue = 100), require = 2)
    private int nfrUi$historyLimit(int original) {
        return EnhancedChatConfigAccess.messageLimit();
    }

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"))
    private void nfrUi$recordReceived(IChatComponent component, int id, CallbackInfo callback) {
        ChatHistoryManager.INSTANCE.recordReceived(component, id);
    }

    @Inject(method = "addToSentMessages", at = @At("RETURN"))
    private void nfrUi$recordAndTrimSent(String message, CallbackInfo callback) {
        ChatHistoryManager.INSTANCE.recordSent(message);
        nfrUi$trimSent(sentMessages, EnhancedChatConfigAccess.messageLimit());
    }

    @Override
    @Unique
    public void nfrUi$trimHistoryToConfiguredLimit() {
        int limit = EnhancedChatConfigAccess.messageLimit();
        nfrUi$trimNewestFirst(chatLines, limit);
        nfrUi$trimNewestFirst(field_146253_i, limit);
        nfrUi$trimSent(sentMessages, limit);
    }

    @Unique
    private static void nfrUi$trimNewestFirst(List<?> lines, int limit) {
        if (lines.size() > limit) lines.subList(limit, lines.size()).clear();
    }

    @Unique
    private static void nfrUi$trimSent(List<?> lines, int limit) {
        if (lines.size() > limit) lines.subList(0, lines.size() - limit).clear();
    }
}
