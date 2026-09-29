package neofontrender.addons.inlinecontent.client;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public final class InlineContentShowcaseCommand extends CommandBase {
    @Override public String getName() { return "nfr_inline_showcase"; }
    @Override public String getUsage(ICommandSender sender) { return "/nfr_inline_showcase"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        InlineContentShowcaseScreen.open();
    }
}
