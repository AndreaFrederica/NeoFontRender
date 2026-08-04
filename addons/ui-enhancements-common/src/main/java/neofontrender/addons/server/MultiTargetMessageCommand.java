package neofontrender.addons.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

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

    @Override public String getName() { return "nfrmessage"; }

    @Override public List<String> getAliases() { return Collections.singletonList("nfrtell"); }

    @Override public String getUsage(ICommandSender sender) {
        return "/nfrmessage <玩家1> [玩家2 ...] <消息>";
    }

    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) throw new WrongUsageException(getUsage(sender));
        List<EntityPlayerMP> targets = new ArrayList<>();
        int messageStart = 0;
        for (int index = 0; index < args.length - 1 && targets.size() < MAX_TARGETS; index++) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(args[index]);
            if (player == null) break;
            targets.add(player);
            messageStart = index + 1;
        }
        if (targets.isEmpty()) throw new CommandException("没有找到目标玩家");
        if (messageStart >= args.length) throw new CommandException("消息内容不能为空");
        ITextComponent message = getChatComponentFromNthArg(sender, args, messageStart, false);
        if (message == null || message.getUnformattedText().isEmpty()) {
            throw new CommandException("消息内容不能为空");
        }
        String text = message.getUnformattedText();
        for (EntityPlayerMP target : targets) {
            target.sendMessage(new TextComponentTranslation("commands.message.display.incoming",
                    sender.getDisplayName(), target.getDisplayName(), message.createCopy()));
            ServerChatHistoryManager.INSTANCE.recordPrivate(sender.getName(), target.getName(), text);
        }
        String names = targets.stream().map(EntityPlayerMP::getName).collect(Collectors.joining(", "));
        sender.sendMessage(new TextComponentTranslation("commands.message.display.outgoing",
                sender.getDisplayName(), names, text));
    }
}
