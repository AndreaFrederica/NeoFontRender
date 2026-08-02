package neofontrender.addons.mixin;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
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
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void nfrUi$allowSelfMessage(MinecraftServer server, ICommandSender sender,
                                        String[] arguments, CallbackInfo ci)
            throws CommandException {
        if (!(sender instanceof EntityPlayerMP) || arguments.length < 2) return;
        if (!arguments[0].equalsIgnoreCase(sender.getName())
                && !arguments[0].equalsIgnoreCase("@s")) return;
        EntityPlayerMP target = getPlayer(server, sender, arguments[0]);
        if (target != sender) return;

        ITextComponent body = getChatComponentFromNthArg(
                sender, arguments, 1, !(sender instanceof EntityPlayer));
        TextComponentTranslation outgoing = new TextComponentTranslation(
                "commands.message.display.outgoing", target.getDisplayName(), body.createCopy());
        outgoing.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
        sender.sendMessage(outgoing);
        ci.cancel();
    }
}
