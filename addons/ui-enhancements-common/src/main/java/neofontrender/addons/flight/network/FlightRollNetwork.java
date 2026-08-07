package neofontrender.addons.flight.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import neofontrender.addons.api.flight.server.FlightServerApi;
import neofontrender.addons.api.flight.server.FlightServerPolicy;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Optional UIE client/server protocol for flight-roll policy and remote-player roll sync. */
public final class FlightRollNetwork {
    public static final int PROTOCOL_VERSION = 2;
    public static final String CHANNEL_NAME = "nfr_ui_flight";
    static final int HANDSHAKE_REQUEST_ID = 0;
    static final int ROLL_UPDATE_ID = 1;
    static final int HANDSHAKE_RESPONSE_ID = 2;
    static final int REMOTE_ROLL_ID = 3;
    private static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
    private static final Set<UUID> COMPATIBLE_CLIENTS = ConcurrentHashMap.newKeySet();
    private static final ServerConnectionListener SERVER_CONNECTION_LISTENER =
            new ServerConnectionListener();

    private static boolean clientInitialized;
    private static volatile boolean serverInitialized;
    private static boolean protocolRegistered;
    private static volatile ClientListener clientListener;

    private FlightRollNetwork() {}

    public static synchronized void initializeClient(ClientListener listener) {
        clientListener = listener;
        if (clientInitialized) return;
        registerProtocol();
        clientInitialized = true;
    }

    public static synchronized void initializeServer() {
        if (serverInitialized) return;
        registerProtocol();
        serverInitialized = true;
        FMLCommonHandler.instance().bus().register(SERVER_CONNECTION_LISTENER);
    }

    /**
     * Forge uses one discriminator table for both directions of a SimpleNetworkWrapper. Every
     * physical side must therefore register the complete protocol, and every packet class needs
     * its own channel-wide id even when two packets travel in opposite directions.
     */
    private static void registerProtocol() {
        if (protocolRegistered) return;
        CHANNEL.registerMessage(HandshakeRequestHandler.class, HandshakeRequest.class,
                HANDSHAKE_REQUEST_ID, Side.SERVER);
        CHANNEL.registerMessage(RollUpdateHandler.class, RollUpdate.class,
                ROLL_UPDATE_ID, Side.SERVER);
        CHANNEL.registerMessage(HandshakeResponseHandler.class, HandshakeResponse.class,
                HANDSHAKE_RESPONSE_ID, Side.CLIENT);
        CHANNEL.registerMessage(RemoteRollHandler.class, RemoteRoll.class,
                REMOTE_ROLL_ID, Side.CLIENT);
        protocolRegistered = true;
    }

    public static void configureServer(boolean enabled, boolean syncEnabled, float maximumRoll) {
        FlightServerApi.configureDefaults(enabled, syncEnabled, maximumRoll);
    }

    public static void requestHandshake() { CHANNEL.sendToServer(new HandshakeRequest(PROTOCOL_VERSION)); }

    public static void sendRollUpdate(boolean rolling, float roll) {
        CHANNEL.sendToServer(new RollUpdate(rolling, finiteOrZero(roll)));
    }

    private static float clampMaximum(float value) {
        return Math.max(0.0F, Math.min(720.0F, finiteOrZero(value)));
    }

    private static float finiteOrZero(float value) { return Float.isFinite(value) ? value : 0.0F; }

    private static float wrapDegrees(float value) {
        float wrapped = finiteOrZero(value) % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    public interface ClientListener {
        void onHandshake(int protocolVersion, boolean enabled, boolean syncEnabled, float maximumRoll);
        void onRemoteRoll(int entityId, boolean rolling, float roll);
    }

    public static final class HandshakeRequest implements IMessage {
        private int protocolVersion;
        public HandshakeRequest() {}
        HandshakeRequest(int protocolVersion) { this.protocolVersion = protocolVersion; }
        @Override public void fromBytes(ByteBuf buffer) { protocolVersion = buffer.readInt(); }
        @Override public void toBytes(ByteBuf buffer) { buffer.writeInt(protocolVersion); }
    }

    public static final class HandshakeResponse implements IMessage {
        private int protocolVersion;
        private boolean enabled;
        private boolean syncEnabled;
        private float maximumRoll;
        public HandshakeResponse() {}
        HandshakeResponse(int protocolVersion, boolean enabled, boolean syncEnabled, float maximumRoll) {
            this.protocolVersion = protocolVersion;
            this.enabled = enabled;
            this.syncEnabled = syncEnabled;
            this.maximumRoll = maximumRoll;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            protocolVersion = buffer.readInt();
            enabled = buffer.readBoolean();
            syncEnabled = buffer.readBoolean();
            maximumRoll = buffer.readFloat();
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(protocolVersion);
            buffer.writeBoolean(enabled);
            buffer.writeBoolean(syncEnabled);
            buffer.writeFloat(maximumRoll);
        }
    }

    public static final class RollUpdate implements IMessage {
        private boolean rolling;
        private float roll;
        public RollUpdate() {}
        RollUpdate(boolean rolling, float roll) { this.rolling = rolling; this.roll = roll; }
        @Override public void fromBytes(ByteBuf buffer) {
            rolling = buffer.readBoolean();
            roll = buffer.readFloat();
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeBoolean(rolling);
            buffer.writeFloat(roll);
        }
    }

    public static final class RemoteRoll implements IMessage {
        private int entityId;
        private boolean rolling;
        private float roll;
        public RemoteRoll() {}
        RemoteRoll(int entityId, boolean rolling, float roll) {
            this.entityId = entityId;
            this.rolling = rolling;
            this.roll = roll;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            rolling = buffer.readBoolean();
            roll = buffer.readFloat();
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeBoolean(rolling);
            buffer.writeFloat(roll);
        }
    }

    public static final class HandshakeRequestHandler
            implements IMessageHandler<HandshakeRequest, IMessage> {
        @Override public IMessage onMessage(HandshakeRequest message, MessageContext context) {
            if (!serverInitialized) return null;
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            boolean compatible = message.protocolVersion == PROTOCOL_VERSION;
            FlightServerPolicy policy = FlightServerApi.policyFor(player);
            if (compatible) COMPATIBLE_CLIENTS.add(player.getUniqueID());
            else COMPATIBLE_CLIENTS.remove(player.getUniqueID());
            CHANNEL.sendTo(new HandshakeResponse(PROTOCOL_VERSION,
                    compatible && policy.isEnabled(),
                    compatible && policy.isSynchronizationEnabled(),
                    policy.getMaximumRollSpeed()), player);
            return null;
        }
    }

    public static final class RollUpdateHandler implements IMessageHandler<RollUpdate, IMessage> {
        @Override public IMessage onMessage(RollUpdate message, MessageContext context) {
            if (!serverInitialized) return null;
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            FlightServerPolicy policy = FlightServerApi.policyFor(player);
            if (!policy.isEnabled() || !policy.isSynchronizationEnabled()) return null;
            boolean rolling = message.rolling
                    && (!policy.isElytraRequired() || player.capabilities.isFlying);
            // A complete barrel roll crosses +/-180 degrees. Normalize the transmitted
            // orientation instead of clipping a complete turn at the angular-speed policy.
            float roll = rolling ? wrapDegrees(message.roll) : 0.0F;
            RemoteRoll packet = new RemoteRoll(player.getEntityId(), rolling, roll);
            double maximumDistanceSq = policy.getSynchronizationRange()
                    * policy.getSynchronizationRange();
            for (EntityPlayerMP recipient : MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList) {
                if (COMPATIBLE_CLIENTS.contains(recipient.getUniqueID())
                        && recipient.getDistanceSqToEntity(player) <= maximumDistanceSq) {
                    CHANNEL.sendTo(packet, recipient);
                }
            }
            return null;
        }
    }

    public static final class ServerConnectionListener {
        @SubscribeEvent
        public void disconnected(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) {
            if (event.handler instanceof NetHandlerPlayServer) {
                COMPATIBLE_CLIENTS.remove(
                        ((NetHandlerPlayServer) event.handler).playerEntity.getUniqueID());
            }
        }
    }

    public static final class HandshakeResponseHandler
            implements IMessageHandler<HandshakeResponse, IMessage> {
        @Override public IMessage onMessage(HandshakeResponse message, MessageContext context) {
            ClientListener listener = clientListener;
            if (listener != null) listener.onHandshake(message.protocolVersion, message.enabled,
                    message.syncEnabled, clampMaximum(message.maximumRoll));
            return null;
        }
    }

    public static final class RemoteRollHandler implements IMessageHandler<RemoteRoll, IMessage> {
        @Override public IMessage onMessage(RemoteRoll message, MessageContext context) {
            ClientListener listener = clientListener;
            if (listener != null) listener.onRemoteRoll(message.entityId, message.rolling,
                    finiteOrZero(message.roll));
            return null;
        }
    }
}
