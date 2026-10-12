package neofontrender.addons.chat.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/** Separate channel keeps the older self-message protocol wire-compatible. No client classes here. */
public final class ChatCharacterNetwork {
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("nfr_ui_chatpol");
    private static boolean initialized;
    private static volatile ClientListener clientListener;

    private ChatCharacterNetwork() {}

    public static synchronized void initialize() {
        if (initialized) return;
        CHANNEL.registerMessage(ProbeHandler.class, Probe.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ReplyHandler.class, Reply.class, 1, Side.CLIENT);
        initialized = true;
    }

    public static void setClientListener(ClientListener listener) { clientListener = listener; }
    public static void probe(long request) { CHANNEL.sendToServer(new Probe(request)); }

    public interface ClientListener {
        void reply(INetHandler handler, long request, int version, boolean allowed);
    }

    public static final class Probe implements IMessage {
        private int version;
        private long request;
        public Probe() {}
        Probe(long request) {
            this.version = ChatPolicySession.PROTOCOL_VERSION;
            this.request = request;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            version = buffer.readInt();
            request = buffer.readLong();
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(version);
            buffer.writeLong(request);
        }
    }

    public static final class Reply implements IMessage {
        private int version;
        private long request;
        private boolean allowed;
        public Reply() {}
        Reply(long request, boolean allowed) {
            this.version = ChatPolicySession.PROTOCOL_VERSION;
            this.request = request;
            this.allowed = allowed;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            version = buffer.readInt();
            request = buffer.readLong();
            allowed = buffer.readBoolean();
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(version);
            buffer.writeLong(request);
            buffer.writeBoolean(allowed);
        }
    }

    public static final class ProbeHandler implements IMessageHandler<Probe, IMessage> {
        @Override public IMessage onMessage(Probe message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (!player.playerNetServerHandler.netManager.isChannelOpen()) return null;
            // 1.7.10 has no server-thread task queue; the reply only reads a volatile policy flag,
            // so it is built and enqueued straight from the channel handler.
            CHANNEL.sendTo(new Reply(message.request,
                    message.version == ChatPolicySession.PROTOCOL_VERSION
                            && ChatCharacterPolicy.serverAllowsSectionSign()), player);
            return null;
        }
    }

    public static final class ReplyHandler implements IMessageHandler<Reply, IMessage> {
        @Override public IMessage onMessage(Reply message, MessageContext context) {
            ClientListener listener = clientListener;
            if (listener != null) listener.reply(context.netHandler, message.request, message.version, message.allowed);
            return null;
        }
    }
}
