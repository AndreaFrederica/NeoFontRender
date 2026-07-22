package neofontrender.addons.vendor.tabbychat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class PlayerLoginHandler {
    @SubscribeEvent
    public void onJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (event.isLocal) {
            TabbyChat.getInstance().onJoin(null);
        } else {
            TabbyChat.getInstance().onJoin(event.manager.getSocketAddress());
        }
    }

    @SubscribeEvent
    public void onJoin(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        TabbyChat.getInstance().onQuit();
    }
}
