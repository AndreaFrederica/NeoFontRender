package neofontrender.addons.mixin;

import io.netty.util.concurrent.GenericFutureListener;
import neofontrender.addons.chat.ClientChatPolicy;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Final outgoing boundary, including mods which bypass ClientChatEvent. */
@Mixin(NetworkManager.class)
public abstract class MixinNetworkManagerChatCharacters {
    @Inject(method = "scheduleOutboundPacket(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$validateChat(Packet packet, GenericFutureListener<?>[] listeners, CallbackInfo ci) {
        if (packet instanceof C01PacketChatMessage
                && !ClientChatPolicy.canSend((NetworkManager) (Object) this,
                        ((C01PacketChatMessage) packet).func_149439_c())) ci.cancel();
    }
}
