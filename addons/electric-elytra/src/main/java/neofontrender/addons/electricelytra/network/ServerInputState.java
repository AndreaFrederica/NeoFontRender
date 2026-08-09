package neofontrender.addons.electricelytra.network;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerInputState {
    private static final Map<UUID, Input> INPUTS = new HashMap<>();

    private ServerInputState() {}

    static void update(EntityPlayer player, boolean jumpHeld, float pitch, float yaw, float roll) {
        INPUTS.put(player.getUniqueID(), new Input(jumpHeld, axis(pitch), axis(yaw), axis(roll),
                player.worldObj.getTotalWorldTime()));
    }

    public static boolean isJumpHeld(EntityPlayer player) {
        Input input = INPUTS.get(player.getUniqueID());
        return input != null && input.jumpHeld
                && player.worldObj.getTotalWorldTime() - input.tick <= 30L;
    }

    public static double getPitchCommand(EntityPlayer player) {
        Input input = INPUTS.get(player.getUniqueID());
        return input != null && player.worldObj.getTotalWorldTime() - input.tick <= 30L
                ? input.pitch : 0.0D;
    }

    public static double getRollCommand(EntityPlayer player) {
        Input input = INPUTS.get(player.getUniqueID());
        return input != null && player.worldObj.getTotalWorldTime() - input.tick <= 30L
                ? input.roll : 0.0D;
    }

    public static double getYawCommand(EntityPlayer player) {
        Input input = INPUTS.get(player.getUniqueID());
        return input != null && player.worldObj.getTotalWorldTime() - input.tick <= 30L
                ? input.yaw : 0.0D;
    }

    public static void remove(EntityPlayer player) { INPUTS.remove(player.getUniqueID()); }

    private static final class Input {
        final boolean jumpHeld;
        final float pitch;
        final float yaw;
        final float roll;
        final long tick;
        Input(boolean jumpHeld, float pitch, float yaw, float roll, long tick) {
            this.jumpHeld = jumpHeld;
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
            this.tick = tick;
        }
    }

    private static float axis(float value) {
        return Math.max(-1.0F, Math.min(1.0F, Float.isFinite(value) ? value : 0.0F));
    }
}
