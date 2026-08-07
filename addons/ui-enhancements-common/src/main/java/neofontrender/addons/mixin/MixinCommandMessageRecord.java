package neofontrender.addons.mixin;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import neofontrender.addons.server.ServerChatHistoryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Persists vanilla private messages (/msg, /tell, /w, ...) as PRIVATE records
 * after a successful delivery. Self-targeted messages (handled by
 * {@code MixinCommandMessageSelfTarget} cancelling execution) are skipped.
 */
@Mixin(CommandMessage.class)
public abstract class MixinCommandMessageRecord extends CommandBase {
    @Inject(method = "processCommand", at = @At("RETURN"))
    private void nfrUi$recordPrivateMessage(ICommandSender sender, String[] arguments, CallbackInfo ci) {
        if (!(sender instanceof EntityPlayerMP) || arguments.length < 2) return;
        if (arguments[0].equalsIgnoreCase(sender.getCommandSenderName())
                || arguments[0].equalsIgnoreCase("@s")) return;
        String text = func_82360_a(sender, arguments, 1);
        ServerChatHistoryManager.INSTANCE.recordPrivate(
                sender.getCommandSenderName(), arguments[0], text);
    }
}
