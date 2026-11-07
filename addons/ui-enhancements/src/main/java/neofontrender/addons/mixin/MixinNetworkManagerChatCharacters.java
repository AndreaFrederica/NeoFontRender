package neofontrender.addons.mixin;

import neofontrender.addons.chat.ClientChatPolicy;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketChatMessage;
import io.netty.util.concurrent.GenericFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Final outgoing boundary, including mods which bypass ClientChatEvent. */
@Mixin(NetworkManager.class)
public abstract class MixinNetworkManagerChatCharacters {
    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$validateChat(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof CPacketChatMessage
                && !ClientChatPolicy.canSend((NetworkManager) (Object) this, ((CPacketChatMessage) packet).getMessage())) ci.cancel();
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$validateChatWithListeners(Packet<?> packet, GenericFutureListener<?> listener,
            GenericFutureListener<?>[] listeners, CallbackInfo ci) {
        if (packet instanceof CPacketChatMessage
                && !ClientChatPolicy.canSend((NetworkManager) (Object) this, ((CPacketChatMessage) packet).getMessage())) ci.cancel();
    }
}
