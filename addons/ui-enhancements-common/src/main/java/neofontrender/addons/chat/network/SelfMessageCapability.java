package neofontrender.addons.chat.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Explicit client/server capability handshake for self-targeted private messages. */
public final class SelfMessageCapability {
    private static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel("nfr_ui_selfmsg");

    private static boolean initialized;
    private static volatile boolean serverSupported;

    private SelfMessageCapability() {}

    public static synchronized void initialize() {
        if (initialized) return;
        CHANNEL.registerMessage(ProbeHandler.class, Probe.class, 0, Side.SERVER);
        CHANNEL.registerMessage(AcknowledgementHandler.class, Acknowledgement.class, 1, Side.CLIENT);
        initialized = true;
    }

    public static void resetClient() {
        serverSupported = false;
    }

    public static void probeServer() {
        serverSupported = false;
        CHANNEL.sendToServer(new Probe());
    }

    public static boolean isServerSupported() {
        return serverSupported;
    }

    public static final class Probe implements IMessage {
        @Override public void fromBytes(ByteBuf buffer) {}
        @Override public void toBytes(ByteBuf buffer) {}
    }

    public static final class Acknowledgement implements IMessage {
        @Override public void fromBytes(ByteBuf buffer) {}
        @Override public void toBytes(ByteBuf buffer) {}
    }

    public static final class ProbeHandler implements IMessageHandler<Probe, IMessage> {
        @Override
        public IMessage onMessage(Probe message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(
                    () -> CHANNEL.sendTo(new Acknowledgement(), player));
            return null;
        }
    }

    public static final class AcknowledgementHandler
            implements IMessageHandler<Acknowledgement, IMessage> {
        @Override
        public IMessage onMessage(Acknowledgement message, MessageContext context) {
            serverSupported = true;
            return null;
        }
    }
}
