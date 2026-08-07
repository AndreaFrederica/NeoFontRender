package neofontrender.addons.mixin;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles the one target vanilla rejects before the normal message construction path.
 * A self message is shown once, using vanilla's outgoing private-message presentation.
 */
@Mixin(CommandMessage.class)
public abstract class MixinCommandMessageSelfTarget extends CommandBase {
    @Inject(method = "processCommand", at = @At("HEAD"), cancellable = true)
    private void nfrUi$allowSelfMessage(ICommandSender sender, String[] arguments, CallbackInfo ci)
            throws CommandException {
        if (!(sender instanceof EntityPlayerMP) || arguments.length < 2) return;
        if (!arguments[0].equalsIgnoreCase(sender.getCommandSenderName())
                && !arguments[0].equalsIgnoreCase("@s")) return;
        EntityPlayerMP target = getPlayer(sender, arguments[0]);
        if (target != sender) return;

        IChatComponent body = func_147176_a(
                sender, arguments, 1, !(sender instanceof EntityPlayer));
        ChatComponentTranslation outgoing = new ChatComponentTranslation(
                "commands.message.display.outgoing", target.func_145748_c_(), body.createCopy());
        outgoing.getChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(true);
        sender.addChatMessage(outgoing);
        ci.cancel();
    }
}
