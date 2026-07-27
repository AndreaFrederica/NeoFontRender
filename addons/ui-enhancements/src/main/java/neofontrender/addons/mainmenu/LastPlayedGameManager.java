package neofontrender.addons.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.ui.NfrUiEnhancements;

public enum LastPlayedGameManager {
    INSTANCE;

    private LastPlayedTarget observedTarget;

    public void initialize() {
        observedTarget = MainMenuConfig.target();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LastPlayedTarget target = currentTarget(Minecraft.getMinecraft());
        if (target == null || target.equals(observedTarget)) return;
        observedTarget = target;
        MainMenuConfig.record(target);
    }

    public LastPlayedTarget availableTarget() {
        LastPlayedTarget target = MainMenuConfig.target();
        if (target == null) return null;
        if (target.kind() == LastPlayedTarget.Kind.SINGLEPLAYER) {
            try {
                WorldInfo info = Minecraft.getMinecraft().getSaveLoader()
                        .getWorldInfo(target.identifier());
                return info == null ? null
                        : LastPlayedTarget.singleplayer(target.identifier(), info.getWorldName());
            } catch (Throwable throwable) {
                NfrUiEnhancements.LOGGER.warn("Could not inspect last played world {}",
                        target.identifier(), throwable);
                return null;
            }
        }
        return target.address().isEmpty() ? null : target;
    }

    public boolean resume(GuiScreen parent, LastPlayedTarget requested) {
        LastPlayedTarget target = availableTarget();
        if (target == null || requested == null
                || !target.stableKey().equals(requested.stableKey())) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (target.kind() == LastPlayedTarget.Kind.SINGLEPLAYER) {
            mc.launchIntegratedServer(target.identifier(), target.displayName(), null);
        } else {
            FMLClientHandler.instance().connectToServer(parent, savedServer(mc, target));
        }
        return true;
    }

    private static LastPlayedTarget currentTarget(Minecraft mc) {
        if (mc.world == null || mc.player == null) return null;
        if (mc.isSingleplayer()) {
            IntegratedServer server = mc.getIntegratedServer();
            if (server == null) return null;
            String name = mc.world.getWorldInfo() == null ? server.getWorldName()
                    : mc.world.getWorldInfo().getWorldName();
            return LastPlayedTarget.singleplayer(server.getFolderName(), name);
        }
        ServerData server = mc.getCurrentServerData();
        return server == null ? null : LastPlayedTarget.server(server.serverIP, server.serverName);
    }

    private static ServerData savedServer(Minecraft mc, LastPlayedTarget target) {
        ServerList servers = new ServerList(mc);
        for (int index = 0; index < servers.countServers(); index++) {
            ServerData server = servers.getServerData(index);
            if (server.serverIP.equalsIgnoreCase(target.address())) return server;
        }
        return new ServerData(target.displayName(), target.address(), false);
    }
}
