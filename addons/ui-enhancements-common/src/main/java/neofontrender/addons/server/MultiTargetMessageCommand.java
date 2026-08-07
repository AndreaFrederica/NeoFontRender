package neofontrender.addons.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /nfrmessage &lt;玩家1&gt; [玩家2 ...] &lt;消息&gt; — private-message style delivery
 * to several online players at once. Each delivery is persisted separately as a
 * PRIVATE record so per-player private history stays consistent.
 */
public final class MultiTargetMessageCommand extends CommandBase {
    private static final int MAX_TARGETS = 32;

    @Override public String getCommandName() { return "nfrmessage"; }

    @Override public List<String> getCommandAliases() { return Collections.singletonList("nfrtell"); }

    @Override public String getCommandUsage(ICommandSender sender) {
        return "/nfrmessage <玩家1> [玩家2 ...] <消息>";
    }

    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) throw new WrongUsageException(getCommandUsage(sender));
        List<EntityPlayerMP> targets = new ArrayList<>();
        int messageStart = 0;
        for (int index = 0; index < args.length - 1 && targets.size() < MAX_TARGETS; index++) {
            EntityPlayerMP player = MinecraftServer.getServer()
                    .getConfigurationManager().func_152612_a(args[index]);
            if (player == null) break;
            targets.add(player);
            messageStart = index + 1;
        }
        if (targets.isEmpty()) throw new CommandException("没有找到目标玩家");
        if (messageStart >= args.length) throw new CommandException("消息内容不能为空");
        IChatComponent message = func_147176_a(sender, args, messageStart, false);
        if (message == null || message.getUnformattedText().isEmpty()) {
            throw new CommandException("消息内容不能为空");
        }
        String text = message.getUnformattedText();
        for (EntityPlayerMP target : targets) {
            target.addChatMessage(new ChatComponentTranslation("commands.message.display.incoming",
                    sender.func_145748_c_(), target.func_145748_c_(), message.createCopy()));
            ServerChatHistoryManager.INSTANCE.recordPrivate(
                    sender.getCommandSenderName(), target.getCommandSenderName(), text);
        }
        String names = targets.stream().map(EntityPlayerMP::getCommandSenderName)
                .collect(Collectors.joining(", "));
        sender.addChatMessage(new ChatComponentTranslation("commands.message.display.outgoing",
                sender.func_145748_c_(), names, text));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return getListOfStringsMatchingLastWord(args,
                MinecraftServer.getServer().getAllUsernames());
    }
}
