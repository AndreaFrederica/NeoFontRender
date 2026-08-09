package neofontrender.addons.electricelytra.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import neofontrender.addons.electricelytra.ElectricElytraMod;
import neofontrender.addons.electricelytra.ItemElectricElytra;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.electricelytra.client.ElectricRemoteAttitudes;

public final class ElectricElytraNetwork {
    private static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel("nfr_electric_flight");

    private ElectricElytraNetwork() {}

    public static void initialize() {
        CHANNEL.registerMessage(InputHandler.class, InputMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ToggleHandler.class, ToggleEngineMessage.class, 1, Side.SERVER);
        CHANNEL.registerMessage(ThrottleHandler.class, ThrottleMessage.class, 2, Side.SERVER);
        CHANNEL.registerMessage(SasHandler.class, ToggleSasMessage.class, 3, Side.SERVER);
        CHANNEL.registerMessage(FlapHandler.class, FlapMessage.class, 4, Side.SERVER);
    }

    /** Called only by the physical client proxy; keeps client classes off dedicated servers. */
    @SideOnly(Side.CLIENT)
    public static void initializeClient() {
        CHANNEL.registerMessage(AttitudeHandler.class, AttitudeMessage.class, 5, Side.CLIENT);
    }

    public static void sendInput(boolean jumpHeld, float pitch, float yaw, float roll,
                                 FlightAttitude attitude) {
        CHANNEL.sendToServer(new InputMessage(jumpHeld, pitch, yaw, roll, attitude));
    }
    public static void toggleEngine() { CHANNEL.sendToServer(new ToggleEngineMessage()); }
    public static void setThrottle(int throttle) { CHANNEL.sendToServer(new ThrottleMessage(throttle)); }
    public static void toggleSas(FlightAttitude attitude) {
        CHANNEL.sendToServer(new ToggleSasMessage(attitude));
    }
    public static void setFlap(int setting) { CHANNEL.sendToServer(new FlapMessage(setting)); }

    public static final class InputMessage implements IMessage {
        private boolean jumpHeld;
        private float pitch;
        private float yaw;
        private float roll;
        private float qx, qy, qz, qw;
        public InputMessage() {}
        InputMessage(boolean jumpHeld, float pitch, float yaw, float roll,
                     FlightAttitude attitude) {
            this.jumpHeld = jumpHeld; this.pitch = pitch; this.yaw = yaw; this.roll = roll;
            this.qx = (float) attitude.x; this.qy = (float) attitude.y;
            this.qz = (float) attitude.z; this.qw = (float) attitude.w;
        }
        @Override public void fromBytes(ByteBuf buf) {
            jumpHeld = buf.readBoolean(); pitch = buf.readFloat();
            yaw = buf.readFloat(); roll = buf.readFloat();
            qx = buf.readFloat(); qy = buf.readFloat(); qz = buf.readFloat(); qw = buf.readFloat();
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeBoolean(jumpHeld); buf.writeFloat(pitch); buf.writeFloat(yaw); buf.writeFloat(roll);
            buf.writeFloat(qx); buf.writeFloat(qy); buf.writeFloat(qz); buf.writeFloat(qw);
        }
    }

    public static final class InputHandler implements IMessageHandler<InputMessage, IMessage> {
        @Override public IMessage onMessage(InputMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            {
                ServerInputState.update(player, message.jumpHeld, message.pitch,
                        message.yaw, message.roll);
                FlightAttitude attitude = new FlightAttitude(
                        message.qx, message.qy, message.qz, message.qw);
                CHANNEL.sendToAllAround(new AttitudeMessage(player.getEntityId(), attitude),
                        new NetworkRegistry.TargetPoint(player.dimension, player.posX,
                                player.posY, player.posZ, 384.0D));
            }
            return null;
        }
    }

    public static final class AttitudeMessage implements IMessage {
        private int entityId;
        private float x, y, z, w;
        public AttitudeMessage() {}
        AttitudeMessage(int entityId, FlightAttitude attitude) {
            this.entityId = entityId;
            x = (float) attitude.x; y = (float) attitude.y;
            z = (float) attitude.z; w = (float) attitude.w;
        }
        @Override public void fromBytes(ByteBuf buf) {
            entityId = buf.readInt(); x = buf.readFloat(); y = buf.readFloat();
            z = buf.readFloat(); w = buf.readFloat();
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeInt(entityId); buf.writeFloat(x); buf.writeFloat(y);
            buf.writeFloat(z); buf.writeFloat(w);
        }
    }

    @SideOnly(Side.CLIENT)
    public static final class AttitudeHandler
            implements IMessageHandler<AttitudeMessage, IMessage> {
        @Override public IMessage onMessage(AttitudeMessage message, MessageContext context) {
            {
                if (Minecraft.getMinecraft().thePlayer != null
                        && Minecraft.getMinecraft().thePlayer.getEntityId() == message.entityId) return null;
                ElectricRemoteAttitudes.update(message.entityId,
                        new FlightAttitude(message.x, message.y, message.z, message.w));
            }
            return null;
        }
    }

    public static final class ToggleEngineMessage implements IMessage {
        @Override public void fromBytes(ByteBuf buf) {}
        @Override public void toBytes(ByteBuf buf) {}
    }

    public static final class ToggleHandler implements IMessageHandler<ToggleEngineMessage, IMessage> {
        @Override public IMessage onMessage(ToggleEngineMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            {
                ItemStack stack = EntityEquipmentSlot.getChest(player);
                if (!ItemElectricElytra.isElectricElytra(stack)) return null;
                boolean enabled = !ItemElectricElytra.isEngineEnabled(stack);
                if (enabled && ItemElectricElytra.getEnergy(stack).getEnergyStored() <= 0) {
                    player.addChatComponentMessage(new ChatComponentTranslation(
                            "message.neofontrender_electric_elytra.empty"));
                    return null;
                }
                ItemElectricElytra.setEngineEnabled(stack, enabled);
                player.addChatComponentMessage(new ChatComponentTranslation(enabled
                        ? "message.neofontrender_electric_elytra.engine_on"
                        : "message.neofontrender_electric_elytra.engine_off"));
            }
            return null;
        }
    }

    public static final class ThrottleMessage implements IMessage {
        private int throttle;
        public ThrottleMessage() {}
        ThrottleMessage(int throttle) { this.throttle = Math.max(0, Math.min(100, throttle)); }
        @Override public void fromBytes(ByteBuf buf) { throttle = buf.readUnsignedByte(); }
        @Override public void toBytes(ByteBuf buf) { buf.writeByte(throttle); }
    }

    public static final class ThrottleHandler implements IMessageHandler<ThrottleMessage, IMessage> {
        @Override public IMessage onMessage(ThrottleMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            {
                ItemStack stack = EntityEquipmentSlot.getChest(player);
                if (!ItemElectricElytra.isElectricElytra(stack)) return null;
                int throttle = Math.max(0, Math.min(100, message.throttle));
                ItemElectricElytra.setThrottle(stack, throttle);
                player.addChatComponentMessage(new ChatComponentTranslation(
                        "message.neofontrender_electric_elytra.throttle", throttle));
            }
            return null;
        }
    }

    public static final class ToggleSasMessage implements IMessage {
        private float x;
        private float y;
        private float z;
        private float w;
        public ToggleSasMessage() {}
        ToggleSasMessage(FlightAttitude attitude) {
            this.x = (float) attitude.x; this.y = (float) attitude.y;
            this.z = (float) attitude.z; this.w = (float) attitude.w;
        }
        @Override public void fromBytes(ByteBuf buf) {
            x = buf.readFloat(); y = buf.readFloat(); z = buf.readFloat(); w = buf.readFloat();
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeFloat(x); buf.writeFloat(y); buf.writeFloat(z); buf.writeFloat(w);
        }
    }

    public static final class SasHandler implements IMessageHandler<ToggleSasMessage, IMessage> {
        @Override public IMessage onMessage(ToggleSasMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            {
                ItemStack stack = EntityEquipmentSlot.getChest(player);
                if (!ItemElectricElytra.isSasCapable(stack)) return null;
                boolean enabled = !ItemElectricElytra.isSasEnabled(stack);
                ItemElectricElytra.setSas(stack, enabled,
                        new FlightAttitude(message.x, message.y, message.z, message.w));
                player.addChatComponentMessage(new ChatComponentTranslation(enabled
                        ? "message.neofontrender_electric_elytra.sas_on"
                        : "message.neofontrender_electric_elytra.sas_off"));
            }
            return null;
        }
    }

    public static final class FlapMessage implements IMessage {
        private int setting;
        public FlapMessage() {}
        FlapMessage(int setting) { this.setting = Math.max(0, Math.min(2, setting)); }
        @Override public void fromBytes(ByteBuf buf) { setting = buf.readUnsignedByte(); }
        @Override public void toBytes(ByteBuf buf) { buf.writeByte(setting); }
    }

    public static final class FlapHandler implements IMessageHandler<FlapMessage, IMessage> {
        @Override public IMessage onMessage(FlapMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            {
                ItemStack stack = EntityEquipmentSlot.getChest(player);
                if (!ItemElectricElytra.isFlapCapable(stack)) return null;
                int setting = Math.max(0, Math.min(2, message.setting));
                ItemElectricElytra.setFlapSetting(stack, setting);
                player.addChatComponentMessage(new ChatComponentTranslation(
                        "message.neofontrender_electric_elytra.flap_" + setting));
            }
            return null;
        }
    }
}
