package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;

/** Immutable virtual-stick command produced by UIE after mouse, keyboard and controller mapping. */
public final class FlightManeuverInput {
    private final EntityPlayerSP player;
    private final float partialTicks;
    private final double frameSeconds;
    private final float pitch;
    private final float yaw;
    private final float roll;
    private final float keyboardYaw;

    public FlightManeuverInput(EntityPlayerSP player, float partialTicks, double frameSeconds,
                               float pitch, float yaw, float roll) {
        this(player, partialTicks, frameSeconds, pitch, yaw, roll, 0.0F);
    }

    public FlightManeuverInput(EntityPlayerSP player, float partialTicks, double frameSeconds,
                               float pitch, float yaw, float roll, float keyboardYaw) {
        this.player = player;
        this.partialTicks = Math.max(0.0F, Math.min(1.0F,
                Float.isFinite(partialTicks) ? partialTicks : 0.0F));
        this.frameSeconds = Double.isFinite(frameSeconds) && frameSeconds > 0.0D
                ? frameSeconds : 0.0D;
        this.pitch = axis(pitch);
        this.yaw = axis(yaw);
        this.roll = axis(roll);
        this.keyboardYaw = axis(keyboardYaw);
    }

    public EntityPlayerSP getPlayer() { return player; }
    public float getPartialTicks() { return partialTicks; }
    public double getFrameSeconds() { return frameSeconds; }
    /** Positive pitch commands nose-down rotation, matching Minecraft pitch. */
    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }
    public float getRoll() { return roll; }
    /** Raw A/D rudder axis, independent of UIE's legacy keyboard-yaw setting/capability. */
    public float getKeyboardYaw() { return keyboardYaw; }

    private static float axis(float value) {
        return Math.max(-1.0F, Math.min(1.0F, Float.isFinite(value) ? value : 0.0F));
    }
}
