package mnm.mods.tabbychat;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class PlayerLoginHandler {
    @SubscribeEvent
    public void onJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (event.isLocal()) {
            TabbyChat.getInstance().onJoin(null);
        } else {
            TabbyChat.getInstance().onJoin(event.getManager().getRemoteAddress());
        }
    }

    @SubscribeEvent
    public void onJoin(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        TabbyChat.getInstance().onQuit();
    }
}
