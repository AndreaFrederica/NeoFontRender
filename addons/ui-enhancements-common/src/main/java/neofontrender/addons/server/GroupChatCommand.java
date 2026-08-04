package neofontrender.addons.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Collections;
import java.util.List;

/**
 * /nfrgroup — lists configured groups.
 * /nfrgroup &lt;组名&gt; — shows the members of a group.
 * /nfrgroup &lt;组名&gt; &lt;消息&gt; — sends a message to every online member of a
 * configured group. The message is persisted server-side under scope
 * group:&lt;name&gt; for the upcoming chat-group feature.
 */
public final class GroupChatCommand extends CommandBase {
    @Override public String getName() { return "nfrgroup"; }

    @Override public List<String> getAliases() { return Collections.singletonList("g"); }

    @Override public String getUsage(ICommandSender sender) {
        return "/nfrgroup [组名] [消息]";
    }

    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            listGroups(sender);
            return;
        }
        if (args.length == 1) {
            showGroup(sender, args[0]);
            return;
        }
        ServerGroupConfig groups = ServerChatHistoryManager.INSTANCE.groups();
        String canonical = groups.findGroup(args[0]);
        if (canonical == null) throw new CommandException("组 '" + args[0] + "' 不存在");
        List<String> members = groups.members(canonical);
        ITextComponent message = getChatComponentFromNthArg(sender, args, 1, false);
        if (message == null || message.getUnformattedText().isEmpty()) {
            throw new CommandException("消息内容不能为空");
        }
        String text = message.getUnformattedText();
        ServerChatHistoryManager.INSTANCE.recordGroup(sender.getName(), canonical,
                String.join(",", members), text);
        String senderName = sender.getName();
        for (String member : members) {
            if (member.equalsIgnoreCase(senderName)) continue;
            EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(member);
            if (player != null) {
                player.sendMessage(new TextComponentTranslation("nfr.group.message.incoming",
                        sender.getDisplayName(), canonical, message.createCopy()));
            }
        }
        sender.sendMessage(new TextComponentTranslation("nfr.group.message.outgoing",
                sender.getDisplayName(), canonical, text));
    }

    private void listGroups(ICommandSender sender) {
        List<String> names = ServerChatHistoryManager.INSTANCE.groups().groupNames();
        if (names.isEmpty()) {
            sender.sendMessage(new TextComponentString(
                    "还没有配置任何组，请在 config/nfr-group-chat.properties 中添加 groups.<组名>=玩家1,玩家2"));
            return;
        }
        sender.sendMessage(new TextComponentString("已配置的组（" + names.size() + "）："));
        for (String name : names) {
            sender.sendMessage(new TextComponentString(
                    " - " + name + ": " + String.join(", ", ServerChatHistoryManager.INSTANCE.groups().members(name))));
        }
    }

    private void showGroup(ICommandSender sender, String name) throws CommandException {
        ServerGroupConfig groups = ServerChatHistoryManager.INSTANCE.groups();
        String canonical = groups.findGroup(name);
        if (canonical == null) throw new CommandException("组 '" + name + "' 不存在");
        sender.sendMessage(new TextComponentString(
                canonical + ": " + String.join(", ", groups.members(canonical))));
    }
}
