package neofontrender.addons.muixml.client;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public final class ShowcaseCommand extends CommandBase {
    @Override
    public String getName() { return "mui_xml_showcase"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/mui_xml_showcase"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        ShowcaseScreen.open();
    }
}
